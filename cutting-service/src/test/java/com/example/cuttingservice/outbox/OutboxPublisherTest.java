package com.example.cuttingservice.outbox;

import com.example.cuttingservice.sync.BackendUnavailableException;
import com.example.cuttingservice.sync.TraceabilityClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Test af at outboxen tømmes rigtigt. Klienten er mocket, så der hverken er
 * netværk eller broker med - det der testes er de to udfald.
 *
 * Stationen kender kun ét udfald nu: enten bekræfter brokeren, eller også
 * bliver hændelsen liggende. "Vil aldrig lykkes" afgøres i dead-letter-køen.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest
{
  @Mock private OutboxRepository outboxRepository;
  @Mock private TraceabilityClient traceabilityClient;

  @InjectMocks private OutboxPublisher publisher;

  // Lykkes afsendelsen, skal posten ud af køen - ellers ville den blive sendt igen.
  @Test
  void aSentEventIsDeletedFromTheOutbox()
  {
    OutboxEntry entry = OutboxEntry.of(OutboxEventType.PART_REGISTERED, "{}");
    when(outboxRepository.findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus.PENDING))
        .thenReturn(List.of(entry));

    publisher.publishPending();

    verify(traceabilityClient).send(entry);
    verify(outboxRepository).delete(entry);
  }

  /*
   * Backenden nede er midlertidigt. Posten skal blive liggende som PENDING,
   * og løkken skal stoppe - ellers sendes den næste post før denne. Her betyder
   * det konkret, at en placering kunne nå frem før delen var registreret.
   */
  @Test
  void anUnavailableBackendLeavesTheQueueUntouchedAndStops()
  {
    OutboxEntry first = OutboxEntry.of(OutboxEventType.PART_REGISTERED, "{}");
    OutboxEntry second = OutboxEntry.of(OutboxEventType.PART_PLACED_IN_TRAY, "{}");
    when(outboxRepository.findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus.PENDING))
        .thenReturn(List.of(first, second));
    doThrow(new BackendUnavailableException("nede")).when(traceabilityClient).send(first);

    publisher.publishPending();

    assertThat(first.getStatus()).isEqualTo(OutboxStatus.PENDING);
    verify(outboxRepository, never()).delete(any());
    verify(traceabilityClient, never()).send(second);
  }
}
