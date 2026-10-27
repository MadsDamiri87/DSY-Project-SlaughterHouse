package com.example.cuttingservice.outbox;

import com.example.cuttingservice.sync.TraceabilityClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", matchIfMissing = true)
public class OutboxPublisher
{

  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

  private final OutboxRepository outboxRepository;
  private final TraceabilityClient traceabilityClient;

  public OutboxPublisher(OutboxRepository outboxRepository, TraceabilityClient traceabilityClient)
  {
    this.outboxRepository   = outboxRepository;
    this.traceabilityClient = traceabilityClient;
  }

  @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:5000}")
  @Transactional
  public void publishPending()
  {
    for (OutboxEntry entry : outboxRepository.findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus.PENDING))
    {
      try
      {
        traceabilityClient.
      }
    }
  }



}
