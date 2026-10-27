package com.example.cuttingservice.web;

import com.example.cuttingservice.dto.AnimalPartDTO;
import com.example.cuttingservice.dto.TrayDTO;
import com.example.cuttingservice.entity.PartTypeMismatchException;
import com.example.cuttingservice.entity.TrayCapacityExceededException;
import com.example.cuttingservice.service.AnimalPartNotFoundException;
import com.example.cuttingservice.service.CuttingService;
import com.example.cuttingservice.service.DuplicateTrayException;
import com.example.cuttingservice.service.TrayNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test af bakke-endpointerne. Servicen er mocket, så det der testes er stierne
 * og oversættelsen af bakkens regler til statuskoder.
 *
 * Placeringen er en PUT og ikke en POST, fordi den skal kunne gentages: stationen
 * gensender når backenden har været nede, og at lægge PART-1 i TRAY-A skal give
 * samme resultat om det sker en eller fem gange.
 */
@WebMvcTest(TrayController.class)
class TrayControllerTest
{
  @Autowired private MockMvc mockMvc;

  @MockitoBean private CuttingService cuttingService;

  private static final Instant CUT_AT = Instant.parse("2026-10-27T08:30:00Z");

  private static final String BODY = """
      {"trayId":"TRAY-A","partType":"LOIN","maxWeight":50.0,"currentWeight":0.0}
      """;

  // En oprettet bakke skal give 201 og en Location.
  @Test
  void creatingATrayReturnsCreatedWithALocationHeader() throws Exception
  {
    when(cuttingService.createTray(any())).thenReturn(new TrayDTO("TRAY-A", "LOIN", 50.0, 0.0));

    mockMvc.perform(post("/trays").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/trays/TRAY-A"));
  }

  // En bakke uden en maksimal vægt kan ikke håndhæve sin kapacitet.
  @Test
  void creatingATrayWithoutAMaxWeightIsRejectedAsBadRequest() throws Exception
  {
    mockMvc.perform(post("/trays").contentType(MediaType.APPLICATION_JSON).content("""
            {"trayId":"TRAY-B","partType":"LOIN","maxWeight":0.0}
            """))
        .andExpect(status().isBadRequest());
  }

  // En bakke uden en deltype kan ikke håndhæve kravet om én slags del.
  @Test
  void creatingATrayWithoutAPartTypeIsRejectedAsBadRequest() throws Exception
  {
    mockMvc.perform(post("/trays").contentType(MediaType.APPLICATION_JSON).content("""
            {"trayId":"TRAY-C","maxWeight":50.0}
            """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void creatingAnExistingTrayReturnsConflict() throws Exception
  {
    when(cuttingService.createTray(any())).thenThrow(new DuplicateTrayException("TRAY-A"));

    mockMvc.perform(post("/trays").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isConflict());
  }

  @Test
  void aSpecificTrayCanBeRead() throws Exception
  {
    when(cuttingService.findTrayById("TRAY-A"))
        .thenReturn(new TrayDTO("TRAY-A", "LOIN", 50.0, 6.2));

    mockMvc.perform(get("/trays/TRAY-A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partType").value("LOIN"))
        .andExpect(jsonPath("$.currentWeight").value(6.2));
  }

  @Test
  void anUnknownTrayReturnsNotFound() throws Exception
  {
    when(cuttingService.findTrayById("TRAY-X")).thenThrow(new TrayNotFoundException("TRAY-X"));

    mockMvc.perform(get("/trays/TRAY-X")).andExpect(status().isNotFound());
  }

  // En placering giver 200 og den opdaterede del - der oprettes ingen ny ressource.
  @Test
  void placingAPartInATrayReturnsTheUpdatedPart() throws Exception
  {
    when(cuttingService.placeInTray("PART-1", "TRAY-A"))
        .thenReturn(new AnimalPartDTO("PART-1", "LOIN", 6.2, 9001, CUT_AT, "TRAY-A"));

    mockMvc.perform(put("/trays/TRAY-A/animal-parts/PART-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trayId").value("TRAY-A"));
  }

  // Bakkens to regler skal nå helt ud til kalderen som 409 og ikke som 500.
  @Test
  void placingAPartOfTheWrongTypeReturnsConflict() throws Exception
  {
    when(cuttingService.placeInTray("PART-1", "TRAY-A"))
        .thenThrow(new PartTypeMismatchException("TRAY-A", "LOIN", "BELLY"));

    mockMvc.perform(put("/trays/TRAY-A/animal-parts/PART-1")).andExpect(status().isConflict());
  }

  @Test
  void placingAPartThatOverfillsTheTrayReturnsConflict() throws Exception
  {
    when(cuttingService.placeInTray("PART-1", "TRAY-A"))
        .thenThrow(new TrayCapacityExceededException("TRAY-A", 50.0, 56.2));

    mockMvc.perform(put("/trays/TRAY-A/animal-parts/PART-1")).andExpect(status().isConflict());
  }

  @Test
  void placingAnUnknownPartReturnsNotFound() throws Exception
  {
    when(cuttingService.placeInTray("PART-X", "TRAY-A"))
        .thenThrow(new AnimalPartNotFoundException("PART-X"));

    mockMvc.perform(put("/trays/TRAY-A/animal-parts/PART-X")).andExpect(status().isNotFound());
  }
}
