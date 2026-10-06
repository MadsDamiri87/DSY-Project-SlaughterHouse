workspace "Slaughterhouse" "C4 model for the DSY course assignment: a slaughterhouse with full traceability from animal to product and back." {

    !docs docs

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
                description "Registers arriving animals with date, weight, registration number and origin, and exposes them as a RESTful web service."
                technology "Java 21, Spring Boot, REST"

                animalApi = component "Animal REST API" {
                    description "Handles POST /animals and the three required lookups: one animal, all animals arriving on a date, and all animals from an origin. Validates the request body before it reaches the service."
                    technology "Spring MVC, Bean Validation"
                }

                restExceptionHandler = component "REST Exception Handler" {
                    description "Turns domain exceptions into HTTP status codes: 404 when the animal is unknown, 409 when the registration number is already in use, and 400 when the request body fails validation."
                    technology "Spring @RestControllerAdvice"
                }

                registrationService = component "Animal Registration Service" {
                    description "Registration rules. Rejects a registration number that is already in use, and defaults the arrival time when the caller leaves it out. Knows nothing about HTTP."
                    technology "Spring @Service"
                }

                registrationRepository = component "Animal Repository" {
                    description "Derived queries for lookup by registration number, by arrival date as a half-open interval, and by origin."
                    technology "Spring Data JPA"
                }

                registrationEntity = component "Animal Entity" {
                    description "The registration service's own view of an animal: registration number, arrival date, weight and origin."
                    technology "Jakarta Persistence"
                }

                registrationOutbox = component "Registration Outbox" {
                    description "Stores AnimalRegistered events in the same transaction as the animal, so registrations are not lost when RabbitMQ is unavailable."
                    technology "Spring Data JPA, H2"
                }

                outboxPublisher = component "Outbox Publisher" {
                    description "Retries pending AnimalRegistered events and removes them from the outbox only after they have been sent to RabbitMQ."
                    technology "Spring Scheduler, Spring AMQP"
                }

                animalApi -> registrationService "Calls"
                restExceptionHandler -> animalApi "Converts exceptions thrown by"
                registrationService -> registrationRepository "Reads and writes animals via"
                registrationService -> registrationOutbox "Stores an AnimalRegistered event atomically in"
                registrationRepository -> registrationEntity "Maps rows to"
                outboxPublisher -> registrationOutbox "Reads pending events from"
            }

            station2 = container "Station 2 - Cutting" {
                description "Cuts animals into parts, weighs each part and places it in a tray holding only one type of part. Planned."
                technology "Java 21, Spring Boot"
                tags "Planned"
            }

            station3 = container "Station 3 - Packing" {
                description "Packs products from one or more trays and registers which trays were used. Planned."
                technology "Java 21, Spring Boot"
                tags "Planned"
            }

            store1 = container "Registration Database" {
                description "Owns registered animals and the transactional outbox. Lets Station 1 accept registrations while RabbitMQ or the backend is unavailable."
                technology "Local persistent H2 database"
                tags "Database"
            }

            store2 = container "Local Store - Station 2" {
                description "Local data store and outbound queue. Lets the station keep working during a partial failure, when the network or the backend is unavailable. Planned."
                technology "Embedded database, e.g. H2 or SQLite"
                tags "Planned,Database"
            }

            store3 = container "Local Store - Station 3" {
                description "Local data store and outbound queue. Lets the station keep working during a partial failure, when the network or the backend is unavailable. Planned."
                technology "Embedded database, e.g. H2 or SQLite"
                tags "Planned,Database"
            }

            broker = container "Message Broker" {
                description "Carries station events asynchronously. Holds a dead-letter queue, so an event the consumer rejects is parked for inspection instead of being redelivered forever. AnimalRegistered is implemented; events from Station 2 and Station 3 are planned."
                technology "RabbitMQ, topic exchange and dead-letter queue"
            }

            client = container "Client" {
                description "Client application used to call the traceability service. Uses the client stub generated from traceability.proto."
                technology "Java 21, gRPC, generated client stub"
            }

            traceabilityServer = container "Traceability Server" {
                description "Consumes animal registrations and provides traceability lookups between animals and products."
                technology "Java 21, Spring Boot, gRPC"

                grpcServerRunner = component "gRPC Server Runner" {
                    description "Starts the gRPC server on the configured port and registers both the service and reflection, so Postman and BloomRPC can discover the contract."
                    technology "Spring CommandLineRunner, gRPC"
                }

                grpcApi = component "gRPC API" {
                    description "Implements the server stub generated from traceability.proto. Validates input and maps domain errors to status codes: NOT_FOUND, INVALID_ARGUMENT and INTERNAL."
                    technology "gRPC, Protocol Buffers, generated server stub"
                }

                traceabilityService = component "Traceability Service" {
                    description "Business logic for tracing product to animals and animal to products. Both directions go through the trays. Knows nothing about JPA or gRPC."
                    technology "Java"
                }

                persistence = component "Repositories" {
                    description "AnimalRepository, AnimalPartRepository, TrayRepository and ProductRepository. Derived queries push the tracing down into the database."
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

                animalEventConsumer = component "Animal Registered Consumer" {
                    description "Consumes AnimalRegistered events idempotently and maps the shared registration number to the server's Animal identity."
                    technology "Spring AMQP"
                }

                grpcServerRunner -> grpcApi "Registers as a gRPC service and starts"
                grpcApi -> traceabilityService "Calls"
                traceabilityService -> persistence "Reads traceability data via"
                demoDataSeeder -> persistence "Writes demo data via"
                animalEventConsumer -> persistence "Upserts registered animals via"
                persistence -> entities "Maps rows to"
            }

            database = container "Slaughterhouse Database" {
                description "Stores animals, animal parts, trays and products."
                technology "PostgreSQL"
                tags "Database"
            }

            registrationRepository -> store1 "Reads and writes animal registrations" "JPA / JDBC"
            registrationOutbox -> store1 "Stores pending events in" "JPA / JDBC"

            station2 -> store2 "Writes registered parts and trays to" "Local persistence" "Planned"
            station3 -> store3 "Writes packed products to" "Local persistence" "Planned"

            outboxPublisher -> broker "Publishes AnimalRegistered events when available" "AMQP"
            store2 -> broker "Forwards queued events when the network is available" "Asynchronous messaging" "Planned"
            store3 -> broker "Forwards queued events when the network is available" "Asynchronous messaging" "Planned"

            broker -> animalEventConsumer "Delivers AnimalRegistered events to" "AMQP"
            animalEventConsumer -> broker "Rejects invalid events to the dead-letter queue" "AMQP"

            persistence -> database "Reads and writes data" "JPA / JDBC"
        }

        station1Operator -> slaughterhouse "Registers and weighs arriving animals using"
        recallSystem -> slaughterhouse "Direct: requests affected products for an animal"

        station1Operator -> animalApi "Registers and weighs arriving animals using" "REST / JSON"
        station2Operator -> station2 "Registers each part with its weight, its animal and the tray it goes into, using"
        station3Operator -> station3 "Registers packed products and the trays they were packed from, using"

        qualityOfficer -> recallSystem "Requests a recall lookup using"

        supplier -> station1 "Indirect: delivers animals"
        station3 -> logistics "Indirect: hands over packed products"
        logistics -> customer "Indirect: distributes products"

        recallSystem -> grpcApi "Direct: requests affected products for an animal" "gRPC / Protocol Buffers"
        client -> grpcApi "Requests traceability information" "gRPC / Protocol Buffers"
    }

    views {

        systemContext slaughterhouse "C1" "C1 - System Context View. Focus is on the types of communication with the outside world rather than on technology." {
            include *
            include qualityOfficer customer
            autolayout tb
        }

        container slaughterhouse "C2" "C2 - Container View. Shows the running parts and how each station keeps working when the network is down." {
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
            }

            element "Component" {
                background #85bbf0
                color #000000
            }

            element "Database" {
                shape cylinder
            }

            element "External System" {
                background #8c8c8c
                color #ffffff
            }

            element "Planned" {
                background #b3b3b3
                color #000000
                border dashed
            }

            relationship "Planned" {
                dashed true
                color #808080
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
