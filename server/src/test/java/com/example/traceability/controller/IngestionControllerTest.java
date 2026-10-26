package com.example.traceability.controller;

import com.example.traceability.service.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test af backendens modtagende endpoints.
 *
 * @WebMvcTest starter kun web-laget — ingen database. IngestionService er mocket,
 * så det der testes er stierne, valideringen og statuskoderne.
 */
@WebMvcTest({AnimalController.class, AnimalPartController.class, TrayController.class,
    ProductController.class})
class IngestionControllerTest
{
  @Autowired private MockMvc mockMvc;

  @MockitoBean private IngestionService ingestionService;

  // Et gyldigt dyr fra Station 1 skal tages imod og sendes videre til servicen.
  @Test
  void aReportedAnimalIsAccepted() throws Exception
  {
    mockMvc.perform(post("/animals").contentType(MediaType.APPLICATION_JSON).content("""
            {"registrationNumber":9001,"arrivalDateTime":"2026-10-25T08:30:00Z",
             "weight":97.5,"origin":"Noerregaard"}
            """))
        .andExpect(status().isOk());

    verify(ingestionService).receiveAnimal(any());
  }

  /*
   * Ingen vægt er en ugyldig besked. Den bliver aldrig gyldig uanset hvor mange gange
   * stationen prøver, så 400 er det rigtige: stationen markerer den som FAILED
   * i stedet for at blokere køen bag den.
   */
  @Test
  void anAnimalWithoutAWeightIsRejectedAsBadRequest() throws Exception
  {
    mockMvc.perform(post("/animals").contentType(MediaType.APPLICATION_JSON).content("""
            {"registrationNumber":9001,"arrivalDateTime":"2026-10-25T08:30:00Z",
             "origin":"Noerregaard"}
            """))
        .andExpect(status().isBadRequest());
  }

  /*
   * Her er den vigtige forskel. En del hvis dyr endnu ikke er nået frem er IKKE en
   * permanent fejl — dyret kommer om lidt fra Station 1's egen outbox. Derfor 503 og
   * ikke 404: stationen skal opfatte det som "prøv igen", ikke som "giv op".
   */
  @Test
  void aMissingReferenceIsReportedAsTemporarilyUnavailable() throws Exception
  {
    doThrow(new NoSuchElementException("No animal with registration number 99999"))
        .when(ingestionService).receivePart(any());

    mockMvc.perform(post("/animal-parts").contentType(MediaType.APPLICATION_JSON).content("""
            {"partId":"PART-A","type":"LOIN","weight":6.2,"animalRegistrationNumber":99999,
             "registrationDateTime":"2026-10-25T08:30:00Z"}
            """))
        .andExpect(status().isServiceUnavailable());
  }

  // Bakker og produkter har hver sin sti, og de skal ramme hver sin metode.
  @Test
  void traysAndProductsHaveTheirOwnEndpoints() throws Exception
  {
    mockMvc.perform(post("/trays").contentType(MediaType.APPLICATION_JSON).content("""
            {"trayId":"TRAY-A","partType":"LOIN","maxWeight":50.0}
            """))
        .andExpect(status().isOk());

    mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
            {"productId":"PROD-A","productType":"LOIN_PACK",
             "packingDateTime":"2026-10-25T08:30:00Z","totalWeight":13.0,"trayIds":["TRAY-A"]}
            """))
        .andExpect(status().isOk());

    verify(ingestionService).receiveTray(any());
    verify(ingestionService).receiveProduct(any());
  }
}
