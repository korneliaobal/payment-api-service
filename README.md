# Payment Processing System

Event-driven payment processing system built with Java, Spring Boot, Apache Kafka, Avro, PostgreSQL and Docker.

The project is split into independent services communicating asynchronously through Kafka.

## Architecture

The system consists of five repositories:

- `payment-api-service` – REST API responsible for accepting payment files, basic validation and publishing payment events.
- `payment-orchestrator-service` – coordinates the payment validation process, persists payment state and aggregates validation results.
- `payment-checker-service` – validates payment-level business rules.
- `transaction-checker-service` – validates individual transactions.
- `payment-contracts` – shared Avro schemas used by all Kafka producers and consumers.

Infrastructure:

- Apache Kafka
- Confluent Schema Registry
- PostgreSQL
- Docker Compose

## Processing flow

```text
Client
  |
  v
payment-api-service
  |
  | payment-created
  v
Kafka
  |
  v
payment-orchestrator-service
  |
  +---- payment-validation-request ------> payment-checker-service
  |
  +---- transaction-validation-request --> transaction-checker-service
  |
  <---- payment-validation-result
  |
  <---- transaction-validation-result
  |
  v
PostgreSQL
```

The orchestrator aggregates all validation results and assigns the final payment status:

- `OK`
- `NOT_OK`
- `PENDING`

## Kafka topics

The application uses the following topics:

```text
payment-created
payment-validation-request
payment-validation-result
transaction-validation-request
transaction-validation-result
```

Messages are serialized using Avro and registered in Confluent Schema Registry.

## Requirements

To run the complete system locally you need:

- Docker Desktop
- Git
- access to the `payment-contracts` GitHub Package

The repositories should be located next to each other:

```text
Payment processing/
├── payment-api-service/
├── payment-orchestrator-service/
├── payment-checker-service/
├── transaction-checker-service/
└── payment-contracts/
```

## GitHub Packages authentication

Some services depend on the private `payment-contracts` Maven package.

Your Maven settings should contain GitHub Packages credentials:

```text
~/.m2/settings.xml
```

Docker BuildKit uses this file as a build secret, so credentials are not copied into the final Docker image.

## Running the system

Open the `payment-api-service` directory:

```bash
cd payment-api-service
```

Build and start the entire stack:

```bash
docker compose up -d --build
```

Docker Compose starts:

- Kafka
- Schema Registry
- PostgreSQL
- payment-api-service
- payment-orchestrator-service
- payment-checker-service
- transaction-checker-service

Check running containers:

```bash
docker ps
```

Kafka, Schema Registry and PostgreSQL should report a `healthy` status.

## Stopping the system

```bash
docker compose down
```

The PostgreSQL data is stored in a Docker named volume and is preserved between normal container restarts.

## API

### Upload payment

```http
POST /api/payments/upload
```

Example:

```bash
curl -X POST \
  -F "file=@$HOME/Desktop/payment.json" \
  http://localhost:8080/api/payments/upload
```

## Validation

Payment-level validation includes rules such as:

- debtor name must not be blank
- debtor account number must have the expected length
- payment cannot contain more than 5 transactions

Transaction-level validation includes:

- creditor name must not be blank
- creditor account number must have the expected length
- transaction amount must be greater than 5

If any validation fails, the final payment status becomes `NOT_OK`.

## Docker

Application images use multi-stage Docker builds.

The first stage builds the Spring Boot application using Java 21 and Maven Wrapper.

The second stage contains only the Java runtime and the generated application JAR.

Build secrets are used for Maven authentication when downloading `payment-contracts`.

## CI

GitHub Actions verifies:

- code formatting with Spotless
- Maven tests
- application build
- Docker image build

All Java projects use Maven Wrapper to ensure consistent Maven execution locally and in CI.

## Technology stack

- Java 21
- Spring Boot
- Spring Kafka
- Apache Kafka
- Apache Avro
- Confluent Schema Registry
- PostgreSQL 17
- Docker
- Docker Compose
- Maven
- GitHub Actions
