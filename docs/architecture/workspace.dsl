workspace "Slaughterhouse" "C4 model for the DSY course assignment: a slaughterhouse with full traceability from animal to product and back." {

    model {
        !impliedRelationships true

        station1Operator = person "Station 1 Operator" {
            description "Receives animals at the arrival end and records what came in."
        }

        station2Operator = person "Station 2 Operator" {
            description "Cuts animals into parts and fills trays."
        }

        station3Operator = person "Station 3 Operator" {
            description "Packs products for distribution."
        }

        qualityOfficer = person "Quality / Recall Officer" {
            description "Investigates problems with a slaughtered animal and decides which products must be recalled."
        }

        supplier = softwareSystem "Animal Supplier" {
            description "External party that delivers living animals. Exchanges happen physically and asynchronously, never through a synchronous call."
            tags "External System"
        }

        logistics = softwareSystem "Transport / Logistics" {
            description "External party that collects packed products and handles distribution."
            tags "External System"
        }

        customer = softwareSystem "Customer / Supermarket" {
            description "External party that orders and receives products."
            tags "External System"
        }

        recallSystem = softwareSystem "External Recall System" {
            description "System outside the slaughterhouse that asks which products may contain parts from a given animal."
            tags "External System"
        }

        slaughterhouse = softwareSystem "Slaughterhouse System" {
            description "Registers and traces animals, parts, trays and products, and supports product recall."

            station1 = container "Station 1 - Registration" {
                description "Registers arriving animals with arrival time, weight, registration number and origin, and exposes them as a RESTful web service. Runs on port 8081."
                technology "Java 21, Spring Boot, REST"

                animalApi = component "Animal REST API" {
                    description "Handles POST /animals and the three required lookups: one animal, all animals arriving on a date, and all animals from an origin. Without a filter it returns every registered animal; the two filters cannot be combined. Validates the request body before it reaches the service."
                    technology "Spring MVC, Bean Validation"
                }

                restExceptionHandler = component "REST Exception Handler" {
                    description "Turns domain exceptions into HTTP status codes: 404 when the animal is unknown, 409 when the registration number is already in use, and 400 when the request body fails validation or combines both filters."
                    technology "Spring @RestControllerAdvice"
                }

                registrationService = component "Animal Registration Service" {
                    description "Registration rules. Rejects a registration number that is already in use, and defaults the arrival time when the caller leaves it out. Writes the animal and the outbox event in one transaction. Knows nothing about HTTP."
                    technology "Spring @Service"
                }

                registrationRepository = component "Animal Repository" {
                    description "Derived queries for lookup by registration number, by arrival date as a half-open interval in the configured slaughterhouse time zone, and by origin."
                    technology "Spring Data JPA"
                }

                registrationEntity = component "Animal Entity" {
                    description "The registration service's own view of an animal: registration number, arrival time, weight and origin. Created on registration; the REST API currently exposes no update operation."
                    technology "Jakarta Persistence"
                }

                registrationOutbox = component "Outbox" {
                    description "Pending events waiting to be forwarded, each with an event id, a type and a JSON payload. Written in the same transaction as the animal, so a registration can never be stored without its event. An event leaves the table only once the broker has confirmed it."
                    technology "Jakarta Persistence, Spring Data JPA"
                }

                registrationPublisher = component "Outbox Publisher" {
                    description "Every five seconds by default, sends up to 100 pending events ordered by occurredAt. Stops at the first event the broker does not confirm, so nothing is deleted before it is safe and the order is kept. Timestamp ties have no explicit ordering; no global station order."
                    technology "Spring @Scheduled"
                }

                registrationClient = component "Traceability Client" {
                    description "Publishes the event payload to the broker's topic exchange under the routing key animal.registered. Waits for the broker to confirm it, and counts a message the broker accepted but could not route to any queue as a failure too, so the event stays in the outbox instead of being deleted."
                    technology "Spring AMQP, RabbitTemplate"
                }

                animalApi -> registrationService "Calls"
                restExceptionHandler -> animalApi "Converts exceptions thrown by"
                registrationService -> registrationRepository "Reads and writes animals via"
                registrationService -> registrationOutbox "Writes the event in the same transaction via"
                registrationRepository -> registrationEntity "Maps rows to"
                registrationPublisher -> registrationOutbox "Reads pending events from"
                registrationPublisher -> registrationClient "Sends each event through"
            }

            station2 = container "Station 2 - Cutting" {
                description "Cuts animals into parts, weighs each part and places it in a tray that holds only one type of part and has a maximum weight. Runs on port 8082."
                technology "Java 21, Spring Boot, REST"

                cuttingApi = component "Part and Tray REST API" {
                    description "Handles POST /animal-parts, POST /trays and PUT /trays/{trayId}/animal-parts/{partId}, plus lookups of a single part or tray and of all of them. The placement is a PUT because the station resends after a failure and the same placement must not count twice."
                    technology "Spring MVC, Bean Validation"
                }

                cuttingExceptionHandler = component "REST Exception Handler" {
                    description "Turns domain exceptions into HTTP status codes: 404 for an unknown part or tray, 409 for a duplicate id and for every broken tray rule, and 400 for an invalid request body."
                    technology "Spring @RestControllerAdvice"
                }

                cuttingService = component "Cutting Service" {
                    description "Registers parts, creates trays and places parts in trays. Writes the part or tray and the outbox event in one transaction. Delegates the tray rules to the entity rather than checking them itself."
                    technology "Spring @Service"
                }

                cuttingRepositories = component "Repositories" {
                    description "AnimalPartRepository and TrayRepository, including a derived query for trays by part type."
                    technology "Spring Data JPA"
                }

                cuttingEntities = component "Part and Tray Entities" {
                    description "The tray owns its own rules: it accepts only parts of its own type, it refuses a part that would take it past its maximum weight, placing the same part twice changes nothing, and a part already in another tray cannot be moved. The service uses addPart to enforce these rules."
                    technology "Jakarta Persistence"
                }

                cuttingOutbox = component "Outbox" {
                    description "Pending events for registered parts, created trays and placements, written in the same transaction as the data they describe."
                    technology "Jakarta Persistence, Spring Data JPA"
                }

                cuttingPublisher = component "Outbox Publisher" {
                    description "Every five seconds by default, sends up to 100 pending events ordered by occurredAt. Stops at the first event the broker does not confirm, so events leave in the order they happened. Redelivery can still repeat an older event, which is why the server never lets a registration undo a placement. No global order across stations."
                    technology "Spring @Scheduled"
                }

                cuttingClient = component "Traceability Client" {
                    description "Publishes parts and trays to the broker's topic exchange, choosing the routing key from the event type: tray.created, part.registered or part.placed. Waits for the broker to confirm each event, and counts an unroutable message as a failure, so the event stays in the outbox."
                    technology "Spring AMQP, RabbitTemplate"
                }

                cuttingApi -> cuttingService "Calls"
                cuttingExceptionHandler -> cuttingApi "Converts exceptions thrown by"
                cuttingService -> cuttingRepositories "Reads and writes parts and trays via"
                cuttingService -> cuttingOutbox "Writes the event in the same transaction via"
                cuttingRepositories -> cuttingEntities "Maps rows to"
                cuttingService -> cuttingEntities "Asks the tray to accept a part, and lets it refuse"
                cuttingPublisher -> cuttingOutbox "Reads pending events from"
                cuttingPublisher -> cuttingClient "Sends each event through"
            }

            station3 = container "Station 3 - Packing" {
                description "Packs products from one or more trays and registers which trays each product was packed from. Runs on port 8083."
                technology "Java 21, Spring Boot, REST"

                packingApi = component "Product REST API" {
                    description "Handles POST /products and lookups of a single product and of all products. Rejects a product that was packed from no trays at all."
                    technology "Spring MVC, Bean Validation"
                }

                packingExceptionHandler = component "REST Exception Handler" {
                    description "Turns domain exceptions into HTTP status codes: 404 for an unknown product, 409 for a duplicate product id, and 400 for an invalid request body."
                    technology "Spring @RestControllerAdvice"
                }

                packingService = component "Packing Service" {
                    description "Packing rules. Rejects a product id that is already in use, and defaults the packing time when the caller leaves it out. Writes the product and the outbox event in one transaction."
                    technology "Spring @Service"
                }

                packingRepository = component "Product Repository" {
                    description "Reads and writes packed products."
                    technology "Spring Data JPA"
                }

                packingEntity = component "Product Entity" {
                    description "A packed product with the tray ids it was packed from, stored as plain values. The station deliberately holds no reference to Station 2's tray objects, so the two can run and fail independently."
                    technology "Jakarta Persistence"
                }

                packingOutbox = component "Outbox" {
                    description "Pending events for packed products, written in the same transaction as the product."
                    technology "Jakarta Persistence, Spring Data JPA"
                }

                packingPublisher = component "Outbox Publisher" {
                    description "Every five seconds by default, sends up to 100 pending events ordered by occurredAt. Stops at the first event the broker does not confirm, so nothing is deleted before it is safe. No global order across stations."
                    technology "Spring @Scheduled"
                }

                packingClient = component "Traceability Client" {
                    description "Publishes packed products to the broker's topic exchange under the routing key product.packed. Waits for the broker to confirm each event, and counts an unroutable message as a failure, so the event stays in the outbox."
                    technology "Spring AMQP, RabbitTemplate"
                }

                packingApi -> packingService "Calls"
                packingExceptionHandler -> packingApi "Converts exceptions thrown by"
                packingService -> packingRepository "Reads and writes products via"
                packingService -> packingOutbox "Writes the event in the same transaction via"
                packingRepository -> packingEntity "Maps rows to"
                packingPublisher -> packingOutbox "Reads pending events from"
                packingPublisher -> packingClient "Sends each event through"
            }

            store1 = container "Registration Database" {
                description "Owns the registered animals and Station 1's outbox. The station has its own store, so a registration can be accepted without any other part of the system being available."
                technology "Local persistent H2 database"
                tags "Database"
            }

            store2 = container "Cutting Database" {
                description "Owns the parts and trays and Station 2's outbox. Lets the station keep cutting while the network or the backend is unavailable."
                technology "Local persistent H2 database"
                tags "Database"
            }

            store3 = container "Packing Database" {
                description "Owns the packed products and Station 3's outbox. Lets the station keep packing while the network or the backend is unavailable."
                technology "Local persistent H2 database"
                tags "Database"
            }

            desktopClient = container "Desktop Client" {
                description "Operator user interface. Logging in as station1, station2, station3 or admin decides what is shown. Calls the stations' own REST APIs and the traceability gRPC service. Not required by the assignment."
                technology "Java 21, JavaFX"
            }

            client = container "Console Client" {
                description "Command line client used to call the traceability service. Uses the client stub generated from traceability.proto."
                technology "Java 21, gRPC, generated client stub"
            }

            broker = container "Message Broker" {
                description "Carries the stations' events to the traceability server. A topic exchange routes each event by its routing key to one queue per aggregate: animal, tray, part and product. Queues and messages are durable, so queued events survive a broker restart."
                technology "RabbitMQ, AMQP 0-9-1"
                tags "Message Broker"
            }

            traceabilityServer = container "Traceability Server" {
                description "Receives what the stations report, stores the whole chain, and answers traceability lookups in both directions. REST on port 8080, gRPC on port 9090."
                technology "Java 21, Spring Boot, REST and gRPC"

                ingestionApi = component "Ingestion REST API" {
                    description "POST /animals, /animal-parts, /trays and /products. The stations no longer call these; they publish to the broker instead. Kept so ingestion can still be exercised by hand with Postman."
                    technology "Spring MVC, Bean Validation"
                }

                eventListener = component "Ingestion Event Listener" {
                    description "Consumes the four queues and hands each event to the ingestion service. Because the stations assign the ids, the same message twice gives the same result, and a repeated registration never clears a placement that already arrived. A temporary failure is retried five times with a growing pause; a message it can never read is rejected without requeue and dead-lettered straight away."
                    technology "Spring AMQP, @RabbitListener"
                }

                backendExceptionHandler = component "Backend Exception Handler" {
                    description "Maps validation errors to 400. A NoSuchElementException maps to 503, but current ingestion creates placeholders for unknown animals and trays."
                    technology "Spring @RestControllerAdvice"
                }

                ingestionService = component "Ingestion Service" {
                    description "Stores what it is told, as an upsert. Creates placeholder animals and trays when references arrive before their details. Later messages fill them in. Tray weight is derived from the parts the server has actually received, so it never claims weight for parts it has not seen."
                    technology "Spring @Service"
                }

                grpcServerRunner = component "gRPC Server Runner" {
                    description "Starts the gRPC server on the configured port and registers both the service and reflection, so Postman and BloomRPC can discover the contract."
                    technology "Spring CommandLineRunner, gRPC"
                }

                grpcApi = component "gRPC API" {
                    description "Implements the server stub generated from traceability.proto: two list lookups and two full traces. Validates input and maps domain errors to status codes: NOT_FOUND, INVALID_ARGUMENT and INTERNAL."
                    technology "gRPC, Protocol Buffers, generated server stub"
                }

                traceabilityService = component "Traceability Service" {
                    description "Traces products to animals and animals to products through trays, and returns the full chain for one animal or one part: every part, its tray and the products that tray reached. Stores a case counter and the latest case in memory. Independent of gRPC."
                    technology "Java"
                }

                persistence = component "Repositories" {
                    description "AnimalRepository, AnimalPartRepository, TrayRepository and ProductRepository, all narrowed to save, findById and findAll by a shared base interface. Derived queries push the tracing down into the database."
                    technology "Spring Data JPA"
                }

                entities = component "Domain Entities" {
                    description "Animal, AnimalPart, Tray and Product, including the references that make tracing possible."
                    technology "Jakarta Persistence"
                }

                demoDataSeeder = component "Demo Data Seeder" {
                    description "Creates demo data on startup. Disabled by default via app.seed-demo-data."
                    technology "Spring CommandLineRunner"
                }

                ingestionApi -> ingestionService "Calls"
                eventListener -> ingestionService "Calls"
                backendExceptionHandler -> ingestionApi "Converts exceptions thrown by"
                ingestionService -> persistence "Stores reported data via"
                grpcServerRunner -> grpcApi "Registers as a gRPC service and starts"
                grpcApi -> traceabilityService "Calls"
                traceabilityService -> persistence "Reads traceability data via"
                demoDataSeeder -> persistence "Writes demo data via"
                persistence -> entities "Maps rows to"
            }

            database = container "Slaughterhouse Database" {
                description "Stores animals, animal parts, trays and products: the whole chain the stations have reported."
                technology "PostgreSQL"
                tags "Database"
            }

            registrationRepository -> store1 "Reads and writes animal registrations" "JPA / JDBC"
            registrationOutbox -> store1 "Stores pending events in" "JPA / JDBC"

            cuttingRepositories -> store2 "Reads and writes parts and trays" "JPA / JDBC"
            cuttingOutbox -> store2 "Stores pending events in" "JPA / JDBC"

            packingRepository -> store3 "Reads and writes packed products" "JPA / JDBC"
            packingOutbox -> store3 "Stores pending events in" "JPA / JDBC"

            cuttingClient -> broker "Publishes queued part and tray events" "AMQP / JSON"
            packingClient -> broker "Publishes queued product events" "AMQP / JSON"
            registrationClient -> broker "Publishes queued animal events" "AMQP / JSON"

            broker -> eventListener "Delivers events from the four durable queues" "AMQP / JSON"

            persistence -> database "Reads and writes data" "JPA / JDBC"

            desktopClient -> animalApi "Registers animals and lists them using" "REST / JSON"
            desktopClient -> cuttingApi "Registers parts and trays using" "REST / JSON"
            desktopClient -> packingApi "Registers packed products using" "REST / JSON"
            desktopClient -> grpcApi "Looks up the chain from animal to product and back using" "gRPC / Protocol Buffers"
        }

        station1Operator -> slaughterhouse "Registers and weighs arriving animals using"
        station2Operator -> slaughterhouse "Registers each part and places it in a tray using"
        station3Operator -> slaughterhouse "Registers packed products and the trays they came from using"
        recallSystem -> slaughterhouse "Direct: requests affected products for an animal"

        station1Operator -> desktopClient "Registers and weighs arriving animals using"
        station2Operator -> desktopClient "Registers each part with its weight and its animal, and places it in a tray, using"
        station3Operator -> desktopClient "Registers packed products and the trays they were packed from, using"

        qualityOfficer -> recallSystem "Requests a recall lookup using"

        supplier -> station1 "Physical delivery of animals"
        station3 -> logistics "Physical handover of packed products"
        logistics -> customer "Physical distribution of products"

        recallSystem -> grpcApi "Direct: requests affected products for an animal" "gRPC / Protocol Buffers"
        client -> grpcApi "Requests traceability information" "gRPC / Protocol Buffers"
    }

    views {

        systemContext slaughterhouse "C1" "C1 - System Context View. Focus is on the types of communication with the outside world rather than on technology." {
            include *
            include qualityOfficer customer
            autolayout tb
        }

        container slaughterhouse "C2" "C2 - Container View. Shows the running parts, the four independent databases, the broker that carries the events, and how each station keeps working when the network is down." {
            include *
            include qualityOfficer customer
            autolayout tb
        }

        component traceabilityServer "C3-Traceability" "C3 - Component View of the Traceability Server. Mirrors the actual code in the server module." {
            include *
            autolayout tb
        }

        component station1 "C3-Registration" "C3 - Component View of Station 1 Registration. Mirrors the actual code in the registration-service module." {
            include *
            autolayout tb
        }

        component station2 "C3-Cutting" "C3 - Component View of Station 2 Cutting. Mirrors the actual code in the cutting-service module." {
            include *
            autolayout tb
        }

        component station3 "C3-Packing" "C3 - Component View of Station 3 Packing. Mirrors the actual code in the packing-service module." {
            include *
            autolayout tb
        }

        styles {
            element "Person" {
                background #08427b
                color #ffffff
                shape person
            }

            element "Software System" {
                background #1168bd
                color #ffffff
            }

            element "Container" {
                background #438dd5
                color #ffffff
                width 520
                height 400
            }

            element "Component" {
                background #85bbf0
                color #000000
                width 520
                height 440
            }

            element "Database" {
                shape cylinder
                width 520
                height 440
            }

            element "Message Broker" {
                background #438dd5
                color #ffffff
                shape pipe
                width 560
                height 440
            }

            element "External System" {
                background #8c8c8c
                color #ffffff
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
