package com.example.desktop.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdsTest
{
  @Test
  @DisplayName("Første id bliver nummer 1 når stationen er tom")
  void foersteIdBliverNummerEt()
  {
    assertEquals("PART-00001", Ids.next(List.of(), Ids.PART_PREFIX));
  }

  @Test
  @DisplayName("Næste id er højeste eksisterende plus én, ikke antallet af rækker")
  void naesteIdErHoejestePlusEn()
  {
    // Hul i rækken: 2 er slettet eller nåede aldrig frem. Tæller vi rækker, rammer vi 3 igen.
    List<String> eksisterende = List.of("PART-00001", "PART-00003");

    assertEquals("PART-00004", Ids.next(eksisterende, Ids.PART_PREFIX));
  }

  @Test
  @DisplayName("Id'er fra andre stationer tæller ikke med")
  void fremmedePraefikserIgnoreres()
  {
    List<String> blandet = List.of("TRAY-00009", "PROD-00042", "PART-00002");

    assertEquals("PART-00003", Ids.next(blandet, Ids.PART_PREFIX));
  }

  @Test
  @DisplayName("Et id der ikke kan læses som tal vælter ikke tællingen")
  void ulaeseligtIdSpringesOver()
  {
    List<String> medSkrald = List.of("PART-00005", "PART-manuel");

    assertEquals("PART-00006", Ids.next(medSkrald, Ids.PART_PREFIX));
    assertNull(Ids.numberIn("PART-manuel", Ids.PART_PREFIX));
  }

  @Test
  @DisplayName("Dyrenummeret er et heltal, men vises med præfiks og nuller")
  void dyrenummerFormateres()
  {
    assertEquals(18, Ids.nextRegistrationNumber(List.of(3, 17, 9)));
    assertEquals("ANIMAL-00018", Ids.animalLabel(18));
  }
}
