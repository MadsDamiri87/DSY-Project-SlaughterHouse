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
                description "Receives, weighs and registers arriving animals. Planned."
                technology "Java 21, Spring Boot"
                tags "Planned"
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

            store1 = container "Local Store - Station 1" {
                description "Local data store and outbound queue. Lets the station keep working during a partial failure, when the network or the backend is unavailable. Planned."
                technology "Embedded database, e.g. H2 or SQLite"
                tags "Planned,Database"
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
                description "Middleware that carries events from the stations asynchronously. Indirect communication decouples the stations, so a partial failure in one does not stop the others. Planned."
                technology "Message broker, e.g. RabbitMQ"
                tags "Planned"
            }

            client = container "Client" {
                description "Client application used to call the traceability service. Uses the client stub generated from traceability.proto."
                technology "Java 21, gRPC, generated client stub"
            }

            traceabilityServer = container "Traceability Server" {
                description "Provides traceability lookups between animals and products. The read side is implemented; consuming station events is planned."
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

                grpcServerRunner -> grpcApi "Registers as a gRPC service and starts"
                grpcApi -> traceabilityService "Calls"
                traceabilityService -> persistence "Reads traceability data via"
                demoDataSeeder -> persistence "Writes demo data via"
                persistence -> entities "Maps rows to"
            }

            database = container "Slaughterhouse Database" {
                description "Stores animals, animal parts, trays and products."
                technology "PostgreSQL"
                tags "Database"
            }

            station1 -> store1 "Writes registered animals to" "Local persistence" "Planned"
            station2 -> store2 "Writes registered parts and trays to" "Local persistence" "Planned"
            station3 -> store3 "Writes packed products to" "Local persistence" "Planned"

            store1 -> broker "Forwards queued events when the network is available" "Asynchronous messaging" "Planned"
            store2 -> broker "Forwards queued events when the network is available" "Asynchronous messaging" "Planned"
            store3 -> broker "Forwards queued events when the network is available" "Asynchronous messaging" "Planned"

            broker -> traceabilityServer "Delivers animal, part, tray and product events to" "Asynchronous messaging" "Planned"

            persistence -> database "Reads and writes data" "JPA / JDBC"
        }

        station1Operator -> station1 "Registers and weighs arriving animals using"
        station2Operator -> station2 "Registers each part with its weight, its animal and the tray it goes into, using"
        station3Operator -> station3 "Registers packed products and the trays they were packed from, using"

        qualityOfficer -> recallSystem "Requests a recall lookup using"

        supplier -> station1 "Indirect: delivers animals" "Physical delivery"
        station3 -> logistics "Indirect: hands over packed products" "Manual / physical process"
        logistics -> customer "Distributes products" "Physical delivery"

        recallSystem -> grpcApi "Direct: requests affected products for an animal" "gRPC / Protocol Buffers"
        client -> grpcApi "Requests traceability information" "gRPC / Protocol Buffers"
    }

    views {

        systemContext slaughterhouse "C1" "System Context View. Focus is on the types of communication with the outside world rather than on technology." {
            include *
            include qualityOfficer customer
            autolayout tb
        }

        container slaughterhouse "C2" "Container View. Shows the running parts and how each station keeps working when the network is down." {
            include *
            include qualityOfficer customer
            autolayout tb
        }

        component traceabilityServer "C3" "Component View of the Traceability Server. Mirrors the actual code in the server module." {
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
