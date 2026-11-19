package com.example.traceability.messaging;

import com.example.traceability.dto.AnimalDTO;
import com.example.traceability.dto.AnimalPartDTO;
import com.example.traceability.dto.ProductDTO;
import com.example.traceability.dto.TrayDTO;
import com.example.traceability.service.IngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/*
 * Enhedstest af serverens modtagerside.
 *
 * IngestionService er mocket, så der hverken er database eller broker med. Det der
 * testes er at hver kø bliver læst som den rigtige type, og hvad der sker med en
 * besked der aldrig kan læses.
 */
@ExtendWith(MockitoExtension.class)
class TraceabilityEventListenerTest
{
  @Mock private IngestionService ingestionService;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private TraceabilityEventListener listener;

  @BeforeEach
  void createListener()
  {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    listener = new TraceabilityEventListener(ingestionService, objectMapper, validator);
  }

  // Dyrekøen skal læses som et dyr og gives videre med felterne i behold.
  @Test
  void anAnimalMessageIsHandedToTheIngestionService()
  {
    listener.onAnimal("""
        {"registrationNumber":17,"arrivalDateTime":"2026-11-19T07:42:00Z",
         "weight":97.5,"origin":"Nørregaard"}
        """);

    ArgumentCaptor<AnimalDTO> animal = ArgumentCaptor.forClass(AnimalDTO.class);
    verify(ingestionService).receiveAnimal(animal.capture());

    assertThat(animal.getValue().registrationNumber()).isEqualTo(17);
    assertThat(animal.getValue().origin()).isEqualTo("Nørregaard");
  }

  // Bakkekøen skal læses som en bakke.
  @Test
  void aTrayMessageIsHandedToTheIngestionService()
  {
    listener.onTray("""
        {"trayId":"TRAY-00003","partType":"LOIN","maxWeight":50.0,"currentWeight":13.0}
        """);

    ArgumentCaptor<TrayDTO> tray = ArgumentCaptor.forClass(TrayDTO.class);
    verify(ingestionService).receiveTray(tray.capture());

    assertThat(tray.getValue().trayId()).isEqualTo("TRAY-00003");
  }

  /*
   * Både registrering og placering af en del rammer part.#, altså samme kø og samme
   * metode. Derfor skal begge kunne læses som en AnimalPartDTO.
   */
  @Test
  void bothPartEventsAreReadAsTheSameType()
  {
    listener.onPart("""
        {"partId":"PART-00041","type":"LOIN","weight":6.1,"animalRegistrationNumber":17,
         "registrationDateTime":"2026-11-19T08:00:00Z"}
        """);
    listener.onPart("""
        {"partId":"PART-00041","type":"LOIN","weight":6.1,"animalRegistrationNumber":17,
         "trayId":"TRAY-00003","registrationDateTime":"2026-11-19T08:05:00Z"}
        """);

    ArgumentCaptor<AnimalPartDTO> part = ArgumentCaptor.forClass(AnimalPartDTO.class);
    verify(ingestionService, times(2)).receivePart(part.capture());

    assertThat(part.getAllValues().get(0).trayId()).isNull();
    assertThat(part.getAllValues().get(1).trayId()).isEqualTo("TRAY-00003");
  }

  // Produktkøen skal læses som et produkt, inklusive listen af bakker det kom fra.
  @Test
  void aProductMessageIsHandedToTheIngestionService()
  {
    listener.onProduct("""
        {"productId":"PROD-00050","productType":"HALF_ANIMAL",
         "packingDateTime":"2026-11-19T09:00:00Z","totalWeight":17.5,
         "trayIds":["TRAY-00003","TRAY-00004"]}
        """);

    ArgumentCaptor<ProductDTO> product = ArgumentCaptor.forClass(ProductDTO.class);
    verify(ingestionService).receiveProduct(product.capture());

    assertThat(product.getValue().trayIds()).containsExactly("TRAY-00003", "TRAY-00004");
  }

  /*
   * En besked der ikke kan læses bliver aldrig gyldig. Den skal afvises uden at blive
   * lagt tilbage i køen — ellers kører den i ring for evigt. Det er den exception
   * lytter-containeren genkender, og som sender beskeden i dead-letter-køen.
   */
  @Test
  void anUnreadablePayloadIsRejectedWithoutRequeue()
  {
    assertThatThrownBy(() -> listener.onAnimal("ikke json"))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);

    verifyNoInteractions(ingestionService);
  }

  /*
   * F04: JSON kan sagtens være læsbar og alligevel ugyldig. Et negativt dyrenummer og
   * en negativ vægt må ikke slippe videre, bare fordi beskeden kom over AMQP i stedet
   * for REST. Også her er den aldrig gyldig, så den skal dead-letters med det samme.
   */
  @Test
  void aReadableButInvalidPayloadIsRejectedWithoutRequeue()
  {
    assertThatThrownBy(() -> listener.onAnimal("""
        {"registrationNumber":-1,"arrivalDateTime":"2026-11-19T07:42:00Z",
         "weight":-5.0,"origin":""}
        """))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class)
        .hasMessageContaining("registrationNumber")
        .hasMessageContaining("weight")
        .hasMessageContaining("origin");

    verifyNoInteractions(ingestionService);
  }
}
