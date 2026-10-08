# Payment Processing System

Event-driven payment processing system built with Java, Spring Boot, Apache Kafka, Avro, PostgreSQL and Docker.

The project is split into independent services communicating asynchronously through Kafka.

## Architecture

The system consists of six repositories:

- `payment-api-service` – REST API responsible for accepting payment files, basic validation and publishing payment events.
- `payment-orchestrator-service` – coordinates the payment validation process, persists payment state and aggregates validation results.
- `payment-checker-service` – validates payment-level business rules.
- `transaction-checker-service` – validates individual transactions.
- `payment-contracts` – shared Avro schemas used by all Kafka producers and consumers.
- `payment-processing-frontend` – Angular workspace for creating payments and inspecting validation results.

Infrastructure:

- Apache Kafka
- Confluent Schema Registry
- Kafbat UI (read-only Kafka browser)
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
- the sibling `payment-contracts` checkout
- Docker Compose with BuildKit additional-context support

The repositories should be located next to each other:

```text
Payment processing/
├── payment-api-service/
├── payment-orchestrator-service/
├── payment-checker-service/
├── transaction-checker-service/
├── payment-contracts/
└── payment-processing-frontend/
```

## Shared contracts

Docker Compose supplies the sibling `payment-contracts` directory as a named build context. Each Kafka consumer image generates and installs the local contract JAR before building its service. Local Compose builds do not require GitHub Packages credentials and always include the current validation fields.

For services run directly through Maven, first run `./mvnw install` in `payment-contracts`, then build the services. The default standalone Docker build uses the published Maven package with BuildKit credentials. Consumer CI checks out a pinned contracts revision, installs it before testing, and uses the same source directory for its Docker build. Update the pinned revision when changing the shared contract API; the contracts repository also has a package publishing workflow. Local Compose uses `CONTRACTS_SOURCE=local` and does not publish anything.

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
- Kafbat UI
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

## Inspecting Kafka messages

Open http://localhost:8083 to access Kafbat UI. The `ObalFlow` cluster is preconfigured with Kafka and Schema Registry, allowing Avro messages to be decoded. The panel is read-only and exposed on the local machine only. The image version is pinned to `v1.5.0`.

To start only the panel and its Kafka/Schema Registry dependencies, run from `payment-api-service`:

```bash
docker compose up -d kafka-ui
```

Start the backend services and frontend separately when you want to inspect a new payment. After submitting a payment:

1. Open **Topics**, select `payment-created`, then open **Messages**.
2. Read messages from the beginning or a timestamp before the submission; use **String** for the key and **SchemaRegistry** for the value if automatic selection does not decode them.
3. Find the payment ID in the message key. Follow the same ID in `payment-validation-request`, `transaction-validation-request`, `payment-validation-result`, and `transaction-validation-result`.
4. Inspect the result payloads for validation status and `reasonCodes`. A payment with multiple transactions has multiple transaction validation messages.
5. Open **Consumer Groups** to inspect the application consumers and their lag, or **Schema Registry** to inspect the Avro schemas.

Kafbat UI displays messages per topic; it does not automatically create an end-to-end trace diagram. Topics and application consumer groups appear after the relevant services start and publish or consume events.

Stop only the panel with `docker compose stop kafka-ui`. The normal `docker compose down` command stops the panel together with the rest of the stack.

References: [Kafbat configuration](https://ui.docs.kafbat.io/configuration/misc-configuration-properties) and [Schema Registry deserialization](https://ui.docs.kafbat.io/configuration/serialization-serde/built-in-serdes).

## Running the frontend

Docker Compose starts the backend and infrastructure. Start the Angular frontend separately in a second terminal, using Node.js 22.22.3+ (or another version supported by the installed Angular CLI):

```bash
cd payment-processing-frontend
npm ci
npm start
```

The path above is relative to the workspace root; from `payment-api-service`, use `cd ../payment-processing-frontend`.

Open http://localhost:4200. The frontend creates a JSON file from the payment form, uploads it to the existing API, and polls the orchestrator for the validation results.

The Angular development server reads `payment-processing-frontend/src/proxy.conf.json` at startup:

- `/api/payments/**` forwards to `http://localhost:8080`.
- `/api/payment-status/**` and `/api/payment-history` forward to `http://localhost:8082`.

After changing the proxy configuration or `angular.json`, stop the frontend with Ctrl+C and run `npm start` again. Reloading the browser alone does not reload the proxy configuration.

If uploading returns `404` with `Cannot POST /api/payments/upload`, the request is being handled by the frontend server instead of the backend. Check that the frontend was restarted and that only one server is running on port 4200. On macOS, check with:

```bash
lsof -nP -iTCP:4200 -sTCP:LISTEN
```

Avoid simultaneously running separate frontend servers bound to `127.0.0.1` and `::1`: `localhost` may reach a different server than `127.0.0.1`.

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

### Read payment status

The orchestrator exposes a read-only endpoint on port 8082:

```http
GET /api/payment-status/{paymentId}
```

The response contains `paymentId`, `status`, `paymentValidationStatus`, payment-level `reasonCodes`, debtor details, currency, total amount, transaction count, and the persistence timestamp. Each transaction includes its identifier, status, recipient, amount, and `reasonCodes`. Missing metadata in older records remains null. A temporary `404` is expected before the payment-created event reaches the orchestrator and the payment is persisted.

The frontend polls this endpoint. A successful upload response does not contain the final validation result.

### Read payment history

```http
GET /api/payment-history?page=0&size=20&status=NOT_OK
```

`status` is optional (`PENDING`, `OK`, or `NOT_OK`). Pages start at zero, and `size` must be between 1 and 100. The response contains `content`, `totalElements`, `totalPages`, `number`, and `size`. Payments are ordered by persistence time descending, followed by identifier, with legacy rows without a timestamp last.

History is read from PostgreSQL, not browser storage. Open a payment's status URL to read its individual transactions and reasons. Existing rows are retained; details that were never stored cannot be reconstructed.

## Authorization persistence

Payment details remain in `payment_entity`; transaction details remain in `transaction_entity`. Current statuses live in two separate tables:

- `payment_authorizations`: `payment_id` (primary key and foreign key), `status`, and `payment_validation_status`.
- `transaction_authorizations`: `transaction_id` (primary key and foreign key), `status`, and `reason_codes`.

Each payment or transaction has one authorization record. These tables contain the latest automatic validation decision, not a status history or manual approval. Payment rejection reason codes remain in `payment_entity`; transaction rejection reason codes live in `transaction_authorizations`. The status and history API response formats remain unchanged.

The first migration creates the original schema for a new database. An existing database is baselined at version 1, then version 2 copies statuses and removes their old columns. Version 3 copies transaction rejection reason codes into `transaction_authorizations` and removes their original column. If a transaction has no authorization record, version 3 creates one with `PENDING` status before copying its reason codes. A legacy null status becomes `PENDING`. Schema changes are versioned with Flyway; Hibernate validates the schema instead of altering it.

Rebuild and restart the orchestrator to apply the migration:

```bash
docker compose up -d --build payment-orchestrator-service
```

Do not run an older orchestrator image after the migration: it expects the removed status columns. Payment creation, validation updates, and authorization writes use database transactions. Updates for the same payment are serialized using a row lock before final status aggregation.

## Validation

Payment-level validation includes rules such as:

- debtor name must not be blank
- debtor account must be a Polish IBAN or NRB with a valid MOD-97 checksum
- payment must contain between 1 and 5 transactions

Transaction-level validation includes:

- creditor name must not be blank
- creditor account must be a Polish IBAN or NRB with a valid MOD-97 checksum
- transaction amount must be greater than 5

If any validation fails, the final payment status becomes `NOT_OK`. Checkers return all detected violations in `reasonCodes`, including `DEBTOR_ACCOUNT_INVALID`, `CREDITOR_ACCOUNT_INVALID`, and `AMOUNT_BELOW_MINIMUM`. The orchestrator persists these codes, and the frontend translates them into operator-facing explanations. The transaction minimum is a demonstration business rule, not a universal banking requirement.

Both the frontend and checkers accept spaces and lowercase `pl`, or 26-digit national NRB numbers. The frontend sends canonical `PL` IBAN values. The validator checks structure and checksum; it does not establish whether an account exists or who owns it. References: [NBP account numbering](https://nbp.pl/wp-content/uploads/2022/07/obwieszczenie-Prezesa-NBP-30-08-2019.pdf) and [SWIFT IBAN checksum documentation](https://www.swift.com/sites/default/files/documents/swift_solutions_faq_ibanplus.pdf).

## Docker

Application images use multi-stage Docker builds.

The first stage builds the Spring Boot application using Java 21 and Maven Wrapper.

The second stage contains only the Java runtime and the generated application JAR.

For individual consumer image builds using local contracts, provide `--build-arg CONTRACTS_SOURCE=local --build-context payment-contracts=../payment-contracts`. Compose supplies this context automatically. The orchestrator uses Flyway migrations and Hibernate `ddl-auto=validate`. Existing payment and transaction statuses are copied into `payment_authorizations` and `transaction_authorizations` before their old columns are removed. Existing records and the PostgreSQL volume are preserved.

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
