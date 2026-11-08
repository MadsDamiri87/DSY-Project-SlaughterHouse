# DSY-SlaughterHouse

Studieprojekt i distribuerede systemer med registrering, opskæring, pakning og sporbarhed over gRPC.
Hver station har sin egen database og outbox, så den kan gemme lokalt under netværksnedbrud.

Åbn de afsnit, du vil arbejde med. Al dokumentation er samlet her.

<details>
<summary><strong>Kom i gang</strong></summary>


Kræver Java 21, Maven og PostgreSQL.

1. Opret databasen `slaughterhouse`, eller brug projektets `compose.yaml` og `.env.example` til Docker.
2. Kopiér `server/src/main/resources/application.properties.example` til `application.properties`, og angiv `DB_PASSWORD` som miljøvariabel. Tilpas eventuelt `DB_URL` og `DB_USERNAME`.
3. Byg fra projektets rodmappe:

   ```powershell
   mvn install -DskipTests
   ```

4. Start `Server`, `RegistrationServiceApplication`, `CuttingServiceApplication` og `PackingServiceApplication` fra IDE'en. Brug projektets rodmappe som arbejdsmappe, så stationernes H2-filer ligger under `data/`.


</details>

<details>
<summary><strong>Projektets formål</strong></summary>

Studieprojekt til kurset **DSY - Distribuerede Systemer**. Formålet er praktisk erfaring med distribuerede systemer, ikke et produktionsklart slagteri.

- Levende dyr ankommer i den ene ende, færdigpakkede produkter leveres i den anden.
- Undervejs passerer dyret tre stationer: registrering, opskæring og pakning.
- Det centrale krav er **sporbarhed**: viser et dyr sig at være problematisk, skal alle produkter der kan indeholde dele fra det, kunne kaldes tilbage.
- Sporingen skal kunne tilgås udefra og er derfor eksponeret som en gRPC-service.

</details>

<details>
<summary><strong>Domænemodel</strong></summary>

Domænemodellen viser de centrale begreber i slagteriet og relationerne mellem dem.

![Domænemodel](docs/analysis/DomainModel.svg)

Sporingskæden er en vigtig del af modellen:

- Et `Animal` bliver skåret op i flere `AnimalPart`, og hver del husker hvilket dyr den kom fra.
- En `AnimalPart` kan være uden bakke, indtil den lægges i én `Tray`. En bakke indeholder kun én type dele og har en maksimal vægtkapacitet.
- Et `Product` pakkes ud fra en eller flere `Tray`, og gemmer referencer tilbage til de bakker delene kom fra.

Et produkt peger på bakker, ikke på enkelte dele. Derfor er sporingen en **bevidst overvurdering**: den finder alle dyr der *kan* være i produktet.

</details>

<details>
<summary><strong>Arkitektur (C4-modellen)</strong></summary>

Arkitekturen er modelleret i **Structurizr**. Kilden er `docs/architecture/workspace.dsl`, som alle diagrammerne genereres ud fra. Klik på et diagram for at se det i fuld størrelse.

### Tegnforklaring
Samme notation i alle seks diagrammer.

<a href="docs/diagrams/C4-Key.svg" target="_blank" rel="noopener">
  <img src="docs/diagrams/C4-Key.svg" alt="Tegnforklaring til C4-diagrammerne" width="600">
</a>

### C1 - System Context

<a href="docs/diagrams/C1-SystemContext.svg" target="_blank" rel="noopener">
  <img src="docs/diagrams/C1-SystemContext.svg" alt="C1 - System Context View" width="600">
</a>

- Blå figurer er brugere, grå kasser er eksterne parter.
- Leverandør, transport og kunde viser **fysisk vareflow** — ikke softwareintegrationer.
- Kun **External Recall System** taler direkte med systemet, over gRPC.

### C2 - Container View

<a href="docs/diagrams/C2-Container.svg" target="_blank" rel="noopener">
  <img src="docs/diagrams/C2-Container.svg" alt="C2 - Container View" width="600">
</a>

- Alle tre stationer, serveren og de fire databaser er implementeret.
- Hver station har sin egen database og kan arbejde videre uden netværk.
- Forbindelsen til serveren er indirekte: hændelsen skrives i stationens egen outbox og sendes videre over HTTP, når serveren kan nås. Ingen message broker.

### C3 - Traceability Server

<a href="docs/diagrams/C3-Component-Traceability.svg" target="_blank" rel="noopener">
  <img src="docs/diagrams/C3-Component-Traceability.svg" alt="C3 - Traceability Server" width="600">
</a>

- **Ind:** stationernes data over REST, gemt som upsert med pladsholdere for ukendte referencer.
- **Ud:** sporingsopslag over gRPC, kontrakten ligger i `traceability.proto`.

### C3 - Station 1 (Registration)

<a href="docs/diagrams/C3-Component-Registration.svg" target="_blank" rel="noopener">
  <img src="docs/diagrams/C3-Component-Registration.svg" alt="C3 - Station 1 Registration" width="600">
</a>

- Dyret og outbox-hændelsen skrives i **samme transaktion**.
- `Outbox Publisher` tømmer køen hvert femte sekund: 5xx betyder prøv igen, 4xx markeres `FAILED`.
- Alle tre stationer bruger samme mønster.

### C3 - Station 2 (Cutting)

<a href="docs/diagrams/C3-Component-Cutting.svg" target="_blank" rel="noopener">
  <img src="docs/diagrams/C3-Component-Cutting.svg" alt="C3 - Station 2 Cutting" width="600">
</a>

- Bakken håndhæver **selv** sine regler: én deltype, maksimal vægt, samme del tæller kun én gang, og en del kan ikke flyttes mellem bakker.
- Reglerne ligger i entiteten, ikke i servicen.

### C3 - Station 3 (Packing)

<a href="docs/diagrams/C3-Component-Packing.svg" target="_blank" rel="noopener">
  <img src="docs/diagrams/C3-Component-Packing.svg" alt="C3 - Station 3 Packing" width="600">
</a>

- `Product` gemmer bakke-id'er som **værdier**, ikke som referencer til Station 2's klasser.
- Derfor kan de to moduler bygges, køre og fejle uafhængigt af hinanden.

</details>

<details>
<summary><strong>Persistens og lokal infrastruktur</strong></summary>

Ingen af de fire applikationer deler database eller skema:

- Serveren ejer `animal`, `animal_part`, `tray` og `product` i **PostgreSQL**.
- Hver station ejer sine egne tabeller plus sin egen `outbox_entry` i en **H2-fil** under `data/`. H2 er indlejret og kræver ingen opsætning.
- De kan startes, stoppes og udvikles uafhængigt af hinanden.

Opslagene bruger afledte queries, fx `findByTrayIn(...)` og `findByOriginIgnoreCase(...)`, så filtreringen sker i databasen frem for i hukommelsen.

PostgreSQL kører som en lokal installation. Vil du i stedet bruge Docker, skal den lokale tjeneste stoppes først, og `.env` oprettes ud fra `.env.example`:

```powershell
docker compose up -d
```

</details>

<details>
<summary><strong>Sådan afprøver du systemet</strong></summary>

### Traceability over gRPC

Sæt `app.seed-demo-data=true` i `server/src/main/resources/application.properties` første gang. Seederen opretter tre dyr (`1`, `2`, `3`), tre bakker, fire dele og to produkter: `PROD-LOIN-PACK` og `PROD-HALF-ANIMAL`.

Den kører **kun hvis `animal`-tabellen er tom**. Har databasen data fra tidligere, springer den over, og id'erne nedenfor findes ikke. Tjek hvad der faktisk ligger i den:

```powershell
psql -h localhost -U postgres -d slaughterhouse -c "select product_id from product" -c "select animal_id from animal"
```

Start `Server` fra IDE'en og kør konsolklienten med et produkt-id og et dyre-id:

```powershell
mvn -pl client exec:java "-Dexec.mainClass=com.example.Client" "-Dexec.args=PROD-LOIN-PACK 1"
```

Med demodata svarer den `Animals in product PROD-LOIN-PACK: [1, 2]` og `Products from animal 1: [PROD-HALF-ANIMAL, PROD-LOIN-PACK]` — begge sporingsretninger på én gang.

Et ukendt id giver `NOT_FOUND`. Et kendt dyr der endnu ikke er pakket giver `OK` med en tom liste. Servicen kan også kaldes fra Postman eller BloomRPC — gRPC reflection er slået til, så kontrakten kan hentes automatisk.

### Registrering over REST

Start `RegistrationServiceApplication` fra IDE'en.

```powershell
Invoke-RestMethod -Uri http://localhost:8081/animals -Method Post -ContentType "application/json" -Body '{"registrationNumber":9001,"weight":97.5,"origin":"Noerregaard"}'
```

Svaret er `201 Created` med en `Location`-header. Udelades `arrivalDateTime`, sætter servicen den til nu, så datoopslaget nedenfor skal bruge dagens dato:

```powershell
Invoke-RestMethod -Uri http://localhost:8081/animals/9001
Invoke-RestMethod -Uri "http://localhost:8081/animals?date=$(Get-Date -Format yyyy-MM-dd)"
Invoke-RestMethod -Uri "http://localhost:8081/animals?origin=Noerregaard"
```

Samme registreringsnummer to gange giver `409 Conflict`, et ukendt nummer giver `404 Not Found`, og en ugyldig body giver `400 Bad Request`.

### Hele kæden

Start alle fire applikationer og kør kæden igennem. Stationernes outbox forsøger normalt levering hvert femte sekund. Vent på synkroniseringen, før du kører gRPC-opslaget.

```powershell
Invoke-RestMethod -Uri http://localhost:8081/animals -Method Post -ContentType "application/json" -Body '{"registrationNumber":9001,"weight":97.5,"origin":"Noerregaard"}'
Invoke-RestMethod -Uri http://localhost:8082/trays -Method Post -ContentType "application/json" -Body '{"trayId":"TRAY-A","partType":"LOIN","maxWeight":50.0}'
Invoke-RestMethod -Uri http://localhost:8082/animal-parts -Method Post -ContentType "application/json" -Body '{"partId":"PART-A","type":"LOIN","weight":6.2,"animalRegistrationNumber":9001}'
Invoke-RestMethod -Uri http://localhost:8082/trays/TRAY-A/animal-parts/PART-A -Method Put
Invoke-RestMethod -Uri http://localhost:8083/products -Method Post -ContentType "application/json" -Body '{"productId":"PROD-A","productType":"LOIN_PACK","totalWeight":6.2,"trayIds":["TRAY-A"]}'

mvn -pl client exec:java "-Dexec.mainClass=com.example.Client" "-Dexec.args=PROD-A 9001"
```

Luk serveren og gentag de to første kald med **nye id’er**: stationerne svarer stadig `201`, og hændelserne bliver liggende i deres outbox indtil serveren er tilbage.

</details>

<details>
<summary><strong>Oversigt over systemets struktur</strong></summary>

```
DSY-SlaughterHouse/
├── pom.xml                 parent, styrer versioner for alle moduler
├── compose.yaml            PostgreSQL
├── proto/                  gRPC-kontrakten
├── server/                 Traceability: REST ind, gRPC ud, PostgreSQL
├── registration-service/   Station 1: registrering, egen H2-database
├── cutting-service/        Station 2: opskæring, egen H2-database
├── packing-service/        Station 3: pakning, egen H2-database
├── client/                 konsolklient til gRPC
└── docs/
    ├── analysis/           domænemodel
    ├── architecture/       Structurizr workspace
    └── diagrams/           eksporterede C4-diagrammer
```

`proto` indeholder kun gRPC-kontrakten. De fire øvrige er selvstændige Spring Boot-applikationer med hver sin database. Ingen af dem afhænger af hinanden.

</details>

<details>
<summary><strong>gRPC-API</strong></summary>

Kontrakten er defineret i `proto/src/main/proto/traceability.proto`:

```protobuf
service TraceabilityService {
  rpc GetAnimalsForProduct(GetAnimalsForProductRequest)
      returns (GetAnimalsForProductResponse);

  rpc GetProductsForAnimal(GetProductsForAnimalRequest)
      returns (GetProductsForAnimalResponse);

  rpc GetAnimalTrace(GetAnimalTraceRequest)
      returns (GetAnimalTraceResponse);

  rpc GetPartTrace(GetPartTraceRequest)
      returns (GetPartTraceResponse);
}
```

De to første giver en flad liste af id'er. `GetAnimalTrace` og `GetPartTrace` giver hele kæden med mellemleddene i behold: hvilke dele et dyr blev skåret i, hvilken bakke hver del havnede i, og hvilke produkter den bakke blev pakket til. `GetPartTrace` går den modsatte vej, fra én del tilbage til dyret.

Fejl oversættes til gRPC-statuskoder i stedet for at boble op som tekniske exceptions:

| Situation | Statuskode |
| --- | --- |
| Opslaget lykkedes | `OK` |
| Ukendt `product_id`, `registration_number` eller `part_id` | `NOT_FOUND` |
| Tomt `product_id` eller `part_id`, eller `registration_number` der ikke er positivt | `INVALID_ARGUMENT` |
| Uventet fejl, f.eks. databasen er nede | `INTERNAL` |

- Et dyr der findes, men endnu ikke er pakket, giver `OK` med en tom liste.
- `product_ids` på en del betyder *may contain*, ikke *does contain*.
- Protobuf har ingen `null`: en del uden bakke får et tomt `tray_id`.

</details>

<details>
<summary><strong>REST-API</strong></summary>

De tre stationer ligger på port 8081, 8082 og 8083.

**Station 1 — registrering (8081)**

| Metode og sti | Betydning | Svar |
| --- | --- | --- |
| `POST /animals` | Registrér et dyr | `201` med `Location` |
| `GET /animals/{registrationNumber}` | Ét bestemt dyr | `200` eller `404` |
| `GET /animals?date=2026-10-20` | Alle dyr ankommet den dato | `200` |
| `GET /animals?origin=Noerregaard` | Alle dyr fra den gård | `200` |
| `GET /animals` | Alle dyr | `200` |

De to filtre kan ikke kombineres — `?date=…&origin=…` svarer `400` i stedet for at ignorere det ene i stilhed.

**Station 2 — opskæring (8082)**

| Metode og sti | Betydning | Svar |
| --- | --- | --- |
| `POST /animal-parts` | Registrér en del | `201` med `Location` |
| `POST /trays` | Opret en bakke | `201` med `Location` |
| `PUT /trays/{trayId}/animal-parts/{partId}` | Læg en del i en bakke | `200`, `404` eller `409` |
| `GET /animal-parts`, `GET /animal-parts/{partId}` | Alle dele, eller én | `200` eller `404` |
| `GET /trays`, `GET /trays/{trayId}` | Alle bakker, eller én | `200` eller `404` |

Placeringen er en `PUT` fordi den skal kunne gentages: stationen gensender efter en netværksfejl, og den samme placering må ikke tælle to gange.

**Station 3 — pakning (8083)**

| Metode og sti | Betydning | Svar |
| --- | --- | --- |
| `POST /products` | Pak et produkt fra en eller flere bakker | `201` med `Location` |
| `GET /products`, `GET /products/{productId}` | Alle produkter, eller ét | `200` eller `404` |

Serveren har de samme fire stier på port 8080, men de bruges kun af stationernes outbox og ikke af mennesker.

Fejl følger samme princip som på gRPC-siden — domænefejl bliver til statuskoder, ikke stacktraces:

| Situation | Statuskode |
| --- | --- |
| Ukendt registreringsnummer | `404 Not Found` |
| Registreringsnummer allerede i brug | `409 Conflict` |
| Manglende eller ugyldigt felt i body | `400 Bad Request` |
| `date` og `origin` i samme kald | `400 Bad Request` |

Datoopslaget bruger den konfigurerede `app.slaughterhouse.timezone` og et halvåbent interval `[dagen 00:00, dagen efter 00:00)`, så et dyr der ankom 23:59 tæller med på sin egen dag. Opslag på gård er ikke versalfølsomt.

</details>

<details>
<summary><strong>Test</strong></summary>

Projektets tests er delt op efter lag: forretningslogikken isoleret med mocks, servicerne mod en rigtig database, og yderlagene hver for sig — gRPC in-process og REST med MockMvc. Alle fire applikationer har desuden en `contextLoads`-test, der beviser at de overhovedet starter.

Bakkens regler er testet som ren forretningslogik uden Spring, og hver stations outbox er testet på de tre udfald: sendt, midlertidigt utilgængelig, permanent afvist.

Serverens tests kører mod PostgreSQL og ikke mod en database i hukommelsen, så de afledte queries bliver oversat til samme SQL-dialekt som i produktionen. Stationerne tester mod H2 i hukommelsen, som er den samme motor de kører på i drift. Serverens tests kræver en lokal PostgreSQL, og at databasen er oprettet én gang:

```sql
CREATE ROLE slaughterhouse_test WITH LOGIN PASSWORD 'slaughterhouse_test';
CREATE DATABASE slaughterhouse_test OWNER slaughterhouse_test;
```

Brugeren er bevidst en engangsbruger uden adgang til andet end testdatabasen, så dens kodeord kan stå i `src/test/resources/application.properties`. Skemaet oprettes og droppes af Hibernate ved hver kørsel.

Testene er kommenteret på dansk, da de også fungerer som mine egne noter til hvad der testes og hvorfor.

</details>

<details>
<summary><strong>Teknologier</strong></summary>

- Java 21
- Maven multi-modul projekt
- gRPC og Protocol Buffers
- Spring Boot og Spring Data JPA
- PostgreSQL og H2
- JUnit 5, Mockito og AssertJ
- Astah til domænemodel
- Structurizr (DSL) til C4-diagrammerne

</details>

<details>
<summary><strong>Status</strong></summary>

Implementeret:

- Domænemodel
- C1, C2 og fire C3-diagrammer modelleret i Structurizr
- Alle tre stationer som selvstændige REST-services med hver sin database
- Synkronisering op til serveren via transactional outbox, med tidsordnet behandling pr. station og status `FAILED` ved permanent afvisning
- Serveren modtager stationernes data idempotent som upsert
- Håndhævelse af bakkens deltype og maksimale vægt
- gRPC-service med begge sporingsopslag og fuld sporing af et dyr eller en del, plus reflection til Postman og BloomRPC
- Persistens i PostgreSQL og H2 via Spring Data JPA
- Eksplicit fejlhåndtering: 201, 400, 404, 409 og 503 på REST, og NOT_FOUND, INVALID_ARGUMENT og INTERNAL på gRPC
- Tests fordelt på lagene
- Konsolklient og demodata-seeder til afprøvning

Ikke implementeret:

- Grafisk brugerflade
- Autentificering og autorisation
- Rettelse og sletning af data der allerede er registreret
- Automatisk tildeling af registreringsnummer; `species` og `slaughterDateTime` udfyldes ikke af registreringsflowet

Kendte begrænsninger:

- `placeInTray` sender ingen opdateret bakkevægt, så serverens `Tray.currentWeight` kan forblive 0.
- Serveren accepterer pladsholdere, så et sporingsopslag kan give `OK` med et ufuldstændigt resultat midt i en synkronisering.
- `FAILED`-hændelser springes over og kan hverken ses eller gensendes.
- Outboxen sorterer kun efter `occurredAt` og garanterer ikke rækkefølgen ved samme tidsstempel eller på tværs af stationer.

</details>


