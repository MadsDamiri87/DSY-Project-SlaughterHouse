package service;

import entity.Animal;
import entity.AnimalPart;
import entity.Product;
import entity.Tray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.AnimalPartRepository;
import repository.AnimalRepository;
import repository.ProductRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Enhedstest af selve sporings-logikken.
 *
 * Her er der ingen database og ingen gRPC. Repositories er Mockito-mocks, så vi selv
 * bestemmer hvad de "finder". Det gør testene lynhurtige og lader os teste logikken
 * isoleret: når en test fejler, ved vi at fejlen ligger i servicen - ikke i SQL.
 */
@ExtendWith(MockitoExtension.class)
class TraceabilityServiceTest
{
  @Mock private ProductRepository productRepository;
  @Mock private AnimalRepository animalRepository;
  @Mock private AnimalPartRepository animalPartRepository;

  // @InjectMocks bygger servicen og sender de tre mocks ovenfor ind i dens constructor.
  @InjectMocks private TraceabilityService service;

  /*
   * Sporing baglæns: produkt -> bakker -> dele -> dyr.
   * Bakken indeholder to dele fra gris 1, men gris 1 må kun optræde én gang i svaret.
   */
  @Test
  void getAnimalsForProductReturnsEachContributingAnimalOnce()
  {
    Animal pig1 = animal(1);
    Animal pig2 = animal(2);
    Tray loinTray = tray("TRAY-LOIN-1");

    Product product = product("PROD-1", List.of(loinTray));

    when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));
    when(animalPartRepository.findByTrayIn(List.of(loinTray))).thenReturn(
        List.of(part("PART-1", pig1, loinTray), part("PART-2", pig2, loinTray),
            part("PART-3", pig1, loinTray)));

    assertThat(service.getAnimalsForProduct("PROD-1")).containsExactly(1, 2);
  }

  // Et ukendt produkt er ikke et tomt svar - det er en fejl. Servicen skal kaste,
  // så gRPC-laget kan oversaette det til statuskoden NOT_FOUND.
  @Test
  void getAnimalsForProductThrowsWhenProductIsUnknown()
  {
    when(productRepository.findById("MISSING")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getAnimalsForProduct("MISSING"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("MISSING");
  }

  /*
   * Et produkt uden bakker skal give en tom liste - ikke et databasekald.
   * "findByTrayIn(tom liste)" bliver til SQL'en "... in ()", som flere databaser afviser.
   * verify(..., never()) dokumenterer at vi bevidst springer kaldet over.
   */
  @Test
  void getAnimalsForProductReturnsEmptyWithoutQueryingPartsWhenProductHasNoTrays()
  {
    when(productRepository.findById("PROD-EMPTY")).thenReturn(Optional.of(product("PROD-EMPTY", List.of())));

    assertThat(service.getAnimalsForProduct("PROD-EMPTY")).isEmpty();
    verify(animalPartRepository, never()).findByTrayIn(anyList());
  }

  /*
   * Sporing forlæns: dyr -> dele -> bakker -> produkter. Det er denne vej tilbagekaldelsen
   * bruger. To af dyrets dele ligger i samme bakke, saa bakken må kun slås op én gang.
   */
  @Test
  void getProductsForAnimalReturnsEveryProductBuiltFromTheAnimalsTrays()
  {
    Animal pig = animal(1);
    Tray loinTray = tray("TRAY-LOIN-1");
    Tray bellyTray = tray("TRAY-BELLY-1");

    when(animalRepository.findById(1)).thenReturn(Optional.of(pig));
    when(animalPartRepository.findByAnimal(pig)).thenReturn(
        List.of(part("PART-1", pig, loinTray), part("PART-2", pig, bellyTray),
            part("PART-3", pig, loinTray)));
    when(productRepository.findDistinctByTraysIn(List.of(loinTray, bellyTray))).thenReturn(
        List.of(product("PROD-1", List.of(loinTray)), product("PROD-2", List.of(loinTray, bellyTray))));

    assertThat(service.getProductsForAnimal(1)).containsExactly("PROD-1", "PROD-2");
  }

  // Samme princip som for ukendt produkt: ukendt dyr er en fejl, ikke et tomt svar.
  @Test
  void getProductsForAnimalThrowsWhenAnimalIsUnknown()
  {
    when(animalRepository.findById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProductsForAnimal(99))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("99");
  }

  /*
   * Vigtig forskel: dyret FINDES, men er ikke skåret op endnu.
   * Det er et lovligt tomt svar - modsat et ukendt dyr, som kaster.
   */
  @Test
  void getProductsForAnimalReturnsEmptyWhenAnimalHasNoRegisteredParts()
  {
    Animal pig = animal(7);

    when(animalRepository.findById(7)).thenReturn(Optional.of(pig));
    when(animalPartRepository.findByAnimal(pig)).thenReturn(List.of());

    assertThat(service.getProductsForAnimal(7)).isEmpty();
    verify(productRepository, never()).findDistinctByTraysIn(anyList());
  }

  // Små hjælpemetoder holder selve testene korte og læsbare.
  private Animal animal(int id)
  {
    return new Animal(id, 100.0);
  }

  private Tray tray(String id)
  {
    Tray tray = new Tray();
    tray.setTrayId(id);
    return tray;
  }

  private AnimalPart part(String id, Animal animal, Tray tray)
  {
    AnimalPart part = new AnimalPart();
    part.setPartId(id);
    part.setAnimal(animal);
    part.setTray(tray);
    return part;
  }

  private Product product(String id, List<Tray> trays)
  {
    Product product = new Product();
    product.setProductId(id);
    product.setTrays(trays);
    return product;
  }
}
