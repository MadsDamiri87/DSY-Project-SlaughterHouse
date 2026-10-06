package com.example.registration.web;

import com.example.registration.service.AnimalNotFoundException;
import com.example.registration.service.AnimalRegistrationService;
import com.example.registration.service.DuplicateRegistrationException;
import com.example.shared.dto.AnimalDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test af web-laget: oversætter det korrekt mellem HTTP og domænet?
 *
 * @WebMvcTest starter kun Spring MVC — ingen database, ingen rigtig service.
 * Servicen er mocket, fordi vi her kun vil teste laget udenom den: ruter,
 * statuskoder, JSON og validering.
 *
 * Det svarer til gRPC-testen i traceability-modulet, hvor vi tjekkede statuskoder
 * mod en in-process server. Her er statuskoderne bare HTTP i stedet for gRPC.
 */
@WebMvcTest(AnimalController.class)
class AnimalControllerTest
{
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AnimalRegistrationService registrationService;

  /*
   * En vellykket registrering skal give 201 Created og en Location-header,
   * der peger på det nye dyr. Det er den korrekte REST-opførsel ved oprettelse.
   */
  @Test
  void registeringAnAnimalReturnsCreatedWithALocationHeader() throws Exception
  {
    AnimalDTO animal = new AnimalDTO(1, LocalDateTime.of(2026, 10, 5, 8, 30), 95.0, "Nørregaard");
    when(registrationService.register(any())).thenReturn(animal);

    mockMvc.perform(post("/animals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(animal)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/animals/1"))
        .andExpect(jsonPath("$.origin").value("Nørregaard"));
  }

  // Ugyldigt input er klientens fejl: 400, og servicen bliver slet ikke kaldt.
  @Test
  void registeringWithoutAWeightIsRejectedAsBadRequest() throws Exception
  {
    String withoutWeight = """
        {"registrationNumber": 1, "origin": "Nørregaard"}
        """;

    mockMvc.perform(post("/animals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(withoutWeight))
        .andExpect(status().isBadRequest());
  }

  // Negativ vægt giver heller ingen mening for et dyr.
  @Test
  void registeringWithANegativeWeightIsRejectedAsBadRequest() throws Exception
  {
    String negativeWeight = """
        {"registrationNumber": 1, "weight": -5, "origin": "Nørregaard"}
        """;

    mockMvc.perform(post("/animals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(negativeWeight))
        .andExpect(status().isBadRequest());
  }

  // Et optaget registreringsnummer er en konflikt, ikke en almindelig fejl.
  @Test
  void registeringAnExistingNumberReturnsConflict() throws Exception
  {
    when(registrationService.register(any())).thenThrow(new DuplicateRegistrationException(1));

    AnimalDTO animal = new AnimalDTO(1, null, 95.0, "Nørregaard");

    mockMvc.perform(post("/animals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(animal)))
        .andExpect(status().isConflict());
  }

  @Test
  void aSpecificAnimalCanBeRead() throws Exception
  {
    when(registrationService.findByRegistrationNumber(1))
        .thenReturn(new AnimalDTO(1, LocalDateTime.of(2026, 10, 5, 8, 30), 95.0, "Nørregaard"));

    mockMvc.perform(get("/animals/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.registrationNumber").value(1))
        .andExpect(jsonPath("$.weight").value(95.0));
  }

  // Ukendt dyr skal give 404 og ikke en teknisk fejl fra servicen.
  @Test
  void anUnknownAnimalReturnsNotFound() throws Exception
  {
    when(registrationService.findByRegistrationNumber(99)).thenThrow(new AnimalNotFoundException(99));

    mockMvc.perform(get("/animals/99")).andExpect(status().isNotFound());
  }

  /*
   * De to listeopslag deler samme URL og skelnes på query-parameteren.
   * Her tjekker vi at ?date rammer dato-opslaget.
   */
  @Test
  void animalsCanBeReadByArrivalDate() throws Exception
  {
    when(registrationService.findByArrivalDate(LocalDate.of(2026, 10, 5)))
        .thenReturn(List.of(new AnimalDTO(1, LocalDateTime.of(2026, 10, 5, 8, 30), 95.0, "Nørregaard")));

    mockMvc.perform(get("/animals").param("date", "2026-10-05"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].registrationNumber").value(1));
  }

  // ... og at ?origin rammer gård-opslaget.
  @Test
  void animalsCanBeReadByOrigin() throws Exception
  {
    when(registrationService.findByOrigin("Nørregaard")).thenReturn(List.of(
        new AnimalDTO(1, LocalDateTime.of(2026, 10, 5, 8, 30), 95.0, "Nørregaard"),
        new AnimalDTO(2, LocalDateTime.of(2026, 10, 5, 23, 59), 102.5, "Nørregaard")));

    mockMvc.perform(get("/animals").param("origin", "Nørregaard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }
}
