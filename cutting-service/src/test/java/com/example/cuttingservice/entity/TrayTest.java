package com.example.cuttingservice.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * Test af bakkens egne regler. Ingen Spring, ingen database - det er ren
 * forretningslogik, og den hører til inde i entiteten selv.
 *
 * Pointen med at lægge reglerne her og ikke i servicen er, at ingen kan
 * omgå dem. Uanset hvem der kalder addPart, gælder de to krav fra opgaven:
 * en bakke indeholder kun én slags del, og den må ikke overfyldes.
 */
class TrayTest
{
  private static final Instant CUT_AT = Instant.parse("2026-10-27T08:30:00Z");

  private AnimalPart part(String id, String type, double weight)
  {
    return new AnimalPart(id, type, weight, 9001, CUT_AT);
  }

  // En ny bakke er tom. Vægten må ikke komme fra den der opretter den.
  @Test
  void aNewTrayStartsEmpty()
  {
    assertThat(new Tray("TRAY-A", "LOIN", 50.0).getCurrentWeight()).isZero();
  }

  // En del der lægges i får bakken som sin, og bakkens vægt stiger med delens.
  @Test
  void addingAPartLinksItAndRaisesTheWeight()
  {
    Tray tray = new Tray("TRAY-A", "LOIN", 50.0);
    AnimalPart loin = part("PART-1", "LOIN", 6.2);

    tray.addPart(loin);

    assertThat(loin.getTray()).isSameAs(tray);
    assertThat(tray.getCurrentWeight()).isEqualTo(6.2);
    assertThat(tray.getParts()).containsExactly(loin);
  }

  // Kravet fra opgaven: en bakke indeholder kun én slags del.
  @Test
  void aPartOfTheWrongTypeIsRejected()
  {
    Tray tray = new Tray("TRAY-A", "LOIN", 50.0);

    assertThatThrownBy(() -> tray.addPart(part("PART-2", "BELLY", 4.5)))
        .isInstanceOf(PartTypeMismatchException.class)
        .hasMessageContaining("LOIN");

    assertThat(tray.getCurrentWeight()).isZero();
  }

  // Og den må ikke overfyldes. Her er der plads til 9 kg, og delen vejer 10.
  @Test
  void aPartThatWouldOverfillTheTrayIsRejected()
  {
    Tray tray = new Tray("TRAY-A", "LOIN", 10.0);
    tray.addPart(part("PART-1", "LOIN", 1.0));

    assertThatThrownBy(() -> tray.addPart(part("PART-2", "LOIN", 10.0)))
        .isInstanceOf(TrayCapacityExceededException.class);

    assertThat(tray.getCurrentWeight()).isEqualTo(1.0);
  }

  // Grænsen er inklusiv - en bakke må fyldes helt op, bare ikke mere.
  @Test
  void aPartThatFillsTheTrayExactlyIsAccepted()
  {
    Tray tray = new Tray("TRAY-A", "LOIN", 10.0);

    tray.addPart(part("PART-1", "LOIN", 10.0));

    assertThat(tray.getCurrentWeight()).isEqualTo(10.0);
  }

  /*
   * Stationen gensender når backenden har været nede, så samme placering kan
   * komme to gange. Anden gang skal intet ske - ellers ville bakken veje det
   * dobbelte af hvad der faktisk ligger i den.
   */
  @Test
  void placingTheSamePartTwiceChangesNothing()
  {
    Tray tray = new Tray("TRAY-A", "LOIN", 50.0);
    AnimalPart loin = part("PART-1", "LOIN", 6.2);

    tray.addPart(loin);
    tray.addPart(loin);

    assertThat(tray.getCurrentWeight()).isEqualTo(6.2);
    assertThat(tray.getParts()).hasSize(1);
  }

  /*
   * En del der allerede ligger i en ANDEN bakke må ikke flyttes. Gjorde man det,
   * ville den første bakkes vægt være forkert bagefter, og sporingen ville pege
   * på en bakke delen ikke længere ligger i.
   */
  @Test
  void aPartThatBelongsToAnotherTrayIsRejected()
  {
    Tray first = new Tray("TRAY-A", "LOIN", 50.0);
    Tray second = new Tray("TRAY-B", "LOIN", 50.0);
    AnimalPart loin = part("PART-1", "LOIN", 6.2);

    first.addPart(loin);

    assertThatThrownBy(() -> second.addPart(loin))
        .isInstanceOf(PartAlreadyPlacedException.class)
        .hasMessageContaining("TRAY-A");

    assertThat(second.getCurrentWeight()).isZero();
  }
}
