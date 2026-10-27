package com.example.packing.web;

import com.example.packing.dto.ProductDTO;
import com.example.packing.service.DuplicateProductException;
import com.example.packing.service.PackingService;
import com.example.packing.service.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Test af web-laget alene. PackingService er mocket, så det der testes er
 * stierne, valideringen og at domænefejl bliver til de rigtige statuskoder.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest
{
  @Autowired private MockMvc mockMvc;

  @MockitoBean private PackingService packingService;

  private static final Instant PACKED_AT = Instant.parse("2026-10-26T09:00:00Z");

  private static final String BODY = """
      {"productId":"PROD-1","productType":"LOIN_PACK",
       "packingDateTime":"2026-10-26T09:00:00Z","totalWeight":13.0,"trayIds":["TRAY-A"]}
      """;

  // Et pakket produkt skal give 201 og en Location, så kalderen kan hente det igen.
  @Test
  void packingAProductReturnsCreatedWithALocationHeader() throws Exception
  {
    when(packingService.pack(any())).thenReturn(
        new ProductDTO("PROD-1", "LOIN_PACK", PACKED_AT, 13.0, List.of("TRAY-A")));

    mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/products/PROD-1"));
  }

  // Casen siger at et produkt pakkes fra en eller flere bakker - tom liste er ugyldigt.
  @Test
  void packingWithoutAnyTraysIsRejectedAsBadRequest() throws Exception
  {
    mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
            {"productId":"PROD-2","productType":"LOIN_PACK",
             "packingDateTime":"2026-10-26T09:00:00Z","totalWeight":13.0,"trayIds":[]}
            """))
        .andExpect(status().isBadRequest());
  }

  // Negativ vægt er heller ikke et gyldigt produkt.
  @Test
  void packingWithANegativeWeightIsRejectedAsBadRequest() throws Exception
  {
    mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
            {"productId":"PROD-3","productType":"LOIN_PACK",
             "packingDateTime":"2026-10-26T09:00:00Z","totalWeight":-5.0,"trayIds":["TRAY-A"]}
            """))
        .andExpect(status().isBadRequest());
  }

  // Samme produkt-id to gange er en konflikt med noget der allerede findes.
  @Test
  void packingAnExistingProductReturnsConflict() throws Exception
  {
    when(packingService.pack(any())).thenThrow(new DuplicateProductException("PROD-1"));

    mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isConflict());
  }

  // Opslag på et bestemt produkt.
  @Test
  void aSpecificProductCanBeRead() throws Exception
  {
    when(packingService.findById("PROD-1")).thenReturn(
        new ProductDTO("PROD-1", "LOIN_PACK", PACKED_AT, 13.0, List.of("TRAY-A")));

    mockMvc.perform(get("/products/PROD-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productType").value("LOIN_PACK"));
  }

  // Et ukendt produkt skal give 404 og ikke en tom krop med 200.
  @Test
  void anUnknownProductReturnsNotFound() throws Exception
  {
    when(packingService.findById("PROD-X")).thenThrow(new ProductNotFoundException("PROD-X"));

    mockMvc.perform(get("/products/PROD-X")).andExpect(status().isNotFound());
  }
}
