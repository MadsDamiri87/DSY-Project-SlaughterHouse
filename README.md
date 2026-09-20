# DSY Course Assignment - SlaughterHouse
2026-09-20

## Projektets formål
Dette er et studieprojekt og projektet har dermed ikke til formål at bygge et produktionsklart slagterisystem, men at give den studerende (mig) praktisk erfaring med distribuerede systemer, som er et nyt fagområde dette semester.

Repository indeholder en simulering af et slagteri og er en del af kurset **'DSY - Distribuerede Systemer'**. Casen er et slagteri hvor levende dyr ankommer i den ene ende, og færdigpakkede produkter leveres i den anden ende. Undervejs passerer dyret tre stationer: registrering, opskæring i dele, og pakning af produkter.

Det centrale krav er **sporbarhed / tracking**. Hvis det senere viser sig at der er problemer med et slagtet dyr, skal alle produkter der kan indeholde dele fra dyret, kunne kaldes tilbage. Den funktion skal kunne tilgås udefra, og derfor er den eksponeret som en gRPC-service.

## Domænemodel
Domænemodellen viser de centrale begreber i slagteriet og relationerne mellem dem.

![DSY-SlaughterHouse-DomainModel.svg](docs/diagrams/DSY-SlaughterHouse-DomainModel.svg)

Sporingskæden er en vigtig del af modellen:

- Et `Animal` bliver skåret op i flere `AnimalPart`, og hver del husker hvilket dyr den kom fra.
- Hver `AnimalPart` lægges i en `Tray`. En bakke indeholder kun én type dele og har en maksimal vægtkapacitet.
- Et `Product` pakkes ud fra en eller flere `Tray`, og gemmer referencer tilbage til de bakker delene kom fra.

Et produkt peger på bakker, ikke på enkelte dele. Det matcher virkeligheden: når man pakker fra en bakke, ved man hvilke dyr der kan være i bakken, men ikke nødvendigvis hvilken del der er endt i hvilken pakke. Sporingen bliver derfor en bevidst overvurdering — den finder alle dyr der **kan** være i produktet.

## Arkitekturoverblik (C1 - System Context)
C1-diagrammet viser systemet som én blok og fokuserer på **typerne af kommunikation** med omverdenen frem for på teknologi.

![DSY-SlaughterHouse-C1-SystemContext.svg](docs/diagrams/DSY-SlaughterHouse-C1-SystemContext.svg)

- **Animal Supplier**, **Transport / Logistics** og **Customer / Supermarket** kommunikerer **indirekte** med systemet. De udveksler fysiske varer og beskeder, og systemet må ikke gå i stå fordi en af dem er utilgængelig.
- **External Recall System** kommunikerer **direkte** med systemet. Det er her sporbarhedsopslaget bliver kaldt, og det er den del der er implementeret som gRPC-service i dette projekt.

Krav til opgaven er at stationerne skal kunne arbejde så uafhængigt som muligt — arbejdet må ikke stoppe på en station bare fordi netværket er nede. Det er grunden til at kommunikationen udadtil er tegnet som overvejende indirekte.



## Oversigt over systemets struktur
```
DSY-SlaughterHouse/
├── pom.xml                              (parent - styrer versioner for alle moduler)
├── docs/
│   └── diagrams/
│       ├── DSY-SlaughterHouse-DomainModel.svg
│       └── DSY-SlaughterHouse-C1-SystemContext.svg
├── proto/
│   ├── pom.xml
│   └── src/main/proto/
│       └── traceability.proto           (kontrakten - genererer Java-kode)
├── server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   ├── entity/              (Animal, AnimalPart, Tray, Product)
│       │   │   ├── repository/          (Spring Data JPA interfaces)
│       │   │   ├── service/             (TraceabilityService - sporingslogikken)
│       │   │   ├── grpc/                (gRPC-laget og serverens opstart)
│       │   │   └── server/              (Server, DemoDataSeeder)
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│           ├── java/
│           │   ├── service/             (enhedstest + integrationstest)
│           │   └── grpc/                (test af gRPC-laget)
│           └── resources/
│               └── application.properties
└── client/
    ├── pom.xml
    └── src/main/java/com/example/
        └── Client.java                  (lille konsolklient til afprøvning)
```

Opdelingen i tre moduler er med vilje. `proto` indeholder kun kontrakten og kender hverken server eller klient. Både `server` og `client` afhænger af `proto`, men ikke af hinanden. Det gør det tydeligt at kontrakten er det eneste de to parter deler — hvilket er hele pointen i et distribueret system.

## Tracking 
Sporingen kan gå begge veje, og begge veje går gennem bakkerne.

**Fra produkt til dyr** — `getAnimalsForProduct`:

```
Product -> Tray(s) -> AnimalPart(s) -> Animal(s)
```

Bruges når man har et produkt og vil vide hvilke dyr der kan indgå i det.

**Fra dyr til produkt** — `getProductsForAnimal`:

```
Animal -> AnimalPart(s) -> Tray(s) -> Product(s)
```

Det er denne vej en tilbagekaldelse bruger: der er problemer med dyr nr. 1, hvilke produkter skal kaldes tilbage?

Logikken ligger i `TraceabilityService` og er bevidst holdt fri for både JPA-detaljer og gRPC. Servicen kender kun domænet, hvilket gør den nem at teste isoleret.

## gRPC-API
Kontrakten er defineret i `proto/src/main/proto/traceability.proto`:

```protobuf
service TraceabilityService {
  rpc GetAnimalsForProduct(GetAnimalsForProductRequest)
      returns (GetAnimalsForProductResponse);

  rpc GetProductsForAnimal(GetProductsForAnimalRequest)
      returns (GetProductsForAnimalResponse);
}
```

Maven genererer Java-klasser ud fra filen ved build, så både server og klient arbejder mod den samme kontrakt.

Fejl oversættes til gRPC-statuskoder i stedet for at boble op som tekniske exceptions:

| Situation | Statuskode |
| --- | --- |
| Opslaget lykkedes | `OK` |
| Ukendt `productId` eller `animalId` | `NOT_FOUND` |
| Tomt `productId`, eller `animalId` der ikke er positivt | `INVALID_ARGUMENT` |
| Uventet fejl, f.eks. databasen er nede | `INTERNAL` |

Et dyr der findes, men endnu ikke er pakket, er ikke en fejl. Det giver `OK` med en tom liste.

## Persistens
Data hentes fra en **PostgreSQL**-database via Spring Data JPA. De fire entities er `Animal`, `AnimalPart`, `Tray` og `Product`.

Selve opslagene bruger afledte queries, f.eks. `findByTrayIn(...)` og `findDistinctByTraysIn(...)`, så sporingen sker i databasen frem for i hukommelsen.

## Test
Projektet har 19 tests fordelt på tre klasser, som tester hvert sit niveau:

| Testklasse | Hvad den tester |
| --- | --- |
| `TraceabilityServiceTest` | Sporingslogikken isoleret. Repositories er Mockito-mocks, så der er hverken database eller netværk. |
| `TraceabilityServiceJpaTest` | Servicen mod en rigtig database (H2 i hukommelsen). Beviser at de afledte queries faktisk oversættes til SQL der virker. |
| `TraceabilityGrpcServiceTest` | gRPC-laget. Starter en rigtig gRPC-server "in-process" og kontrollerer at statuskoderne er rigtige. |

Testene er kommenteret på dansk, da de også fungerer som mine egne noter til hvad der bliver testet og hvorfor.


## Teknologier

- Java 21
- Maven multi-modul projekt
- gRPC og Protocol Buffers
- Spring Boot og Spring Data JPA
- PostgreSQL (H2 i tests)
- JUnit 5, Mockito og AssertJ
- Astah til domænemodel og C4-diagrammer

## Status
Implementeret:

- Domænemodel
- C1 - System Context View
- gRPC-service med begge sporingsopslag
- Persistens i PostgreSQL via Spring Data JPA
- Eksplicit fejlhåndtering med gRPC-statuskoder
- 19 tests på tre niveauer
- Konsolklient til afprøvning
- gRPC reflection, så servicen kan testes med Postman og BloomRPC
- Demodata-seeder, der kan slås til og fra

Ikke implementeret endnu:

- C2 - Container View og C3 - Component View
- De tre stationer som selvstændige, kørende enheder
- Offline-drift, hvor en station kan arbejde videre uden netværk og synkronisere bagefter
- Håndhævelse af bakkernes maksimale vægtkapacitet
- Autentificering og autorisation
