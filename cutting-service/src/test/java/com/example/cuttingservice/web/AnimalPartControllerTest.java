package com.example.cuttingservice.web;

import com.example.cuttingservice.dto.AnimalPartDTO;
import com.example.cuttingservice.service.AnimalPartNotFoundException;
import com.example.cuttingservice.service.CuttingService;
import com.example.cuttingservice.service.DuplicateAnimalPartException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test af web-laget alene. CuttingService er mocket, så det der testes er
 * stierne, valideringen og at domænefejl bliver til de rigtige statuskoder.
 */
@WebMvcTest(AnimalPartController.class)
class AnimalPartControllerTest
{
  @Autowired private MockMvc mockMvc;

  @MockitoBean private CuttingService cuttingService;

  private static final Instant CUT_AT = Instant.parse("2026-10-27T08:30:00Z");

  private static final String BODY = """
      {"partId":"PART-1","type":"LOIN","weight":6.2,"animalRegistrationNumber":9001,
       "registrationDateTime":"2026-10-27T08:30:00Z"}
      """;

  private AnimalPartDTO part(String partId, String trayId)
  {
    return new AnimalPartDTO(partId, "LOIN", 6.2, 9001, CUT_AT, trayId);
  }

  // En registreret del skal give 201 og en Location, så kalderen kan hente den igen.
  @Test
  void registeringAPartReturnsCreatedWithALocationHeader() throws Exception
  {
    when(cuttingService.registerPart(any())).thenReturn(part("PART-1", null));

    mockMvc.perform(post("/animal-parts").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/animal-parts/PART-1"));
  }

  // En del uden vægt er ikke en vejet del, og casen kræver at hver del vejes.
  @Test
  void registeringWithANegativeWeightIsRejectedAsBadRequest() throws Exception
  {
    mockMvc.perform(post("/animal-parts").contentType(MediaType.APPLICATION_JSON).content("""
            {"partId":"PART-2","type":"LOIN","weight":-6.2,"animalRegistrationNumber":9001}
            """))
        .andExpect(status().isBadRequest());
  }

  // En del skal altid kunne føres tilbage til et dyr - uden registreringsnummer er den ubrugelig.
  @Test
  void registeringWithoutAnAnimalIsRejectedAsBadRequest() throws Exception
  {
    mockMvc.perform(post("/animal-parts").contentType(MediaType.APPLICATION_JSON).content("""
            {"partId":"PART-3","type":"LOIN","weight":6.2}
            """))
        .andExpect(status().isBadRequest());
  }

  // Samme part-id to gange er en konflikt med noget der allerede findes.
  @Test
  void registeringAnExistingPartReturnsConflict() throws Exception
  {
    when(cuttingService.registerPart(any())).thenThrow(new DuplicateAnimalPartException("PART-1"));

    mockMvc.perform(post("/animal-parts").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isConflict());
  }

  // Opslag på en bestemt del, med bakken den ligger i.
  @Test
  void aSpecificPartCanBeRead() throws Exception
  {
    when(cuttingService.findPartById("PART-1")).thenReturn(part("PART-1", "TRAY-A"));

    mockMvc.perform(get("/animal-parts/PART-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("LOIN"))
        .andExpect(jsonPath("$.trayId").value("TRAY-A"));
  }

  // En ukendt del skal give 404 og ikke en tom krop med 200.
  @Test
  void anUnknownPartReturnsNotFound() throws Exception
  {
    when(cuttingService.findPartById("PART-X")).thenThrow(new AnimalPartNotFoundException("PART-X"));

    mockMvc.perform(get("/animal-parts/PART-X")).andExpect(status().isNotFound());
  }
}
