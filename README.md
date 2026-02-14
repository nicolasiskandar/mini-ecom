# Mini ECommerce

A Spring Boot 4 REST API for a mini e-commerce backend with:
- Product catalog management
- Order lifecycle management
- Stock reservation/reconciliation
- Order payment processing
- OpenAPI/Swagger documentation
- Unit, integration, and BDD (Cucumber) tests

## Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Web + Validation
- Spring Data JPA (Hibernate)
- PostgreSQL 16
- Springdoc OpenAPI (Swagger UI)
- JUnit 5, Mockito
- Testcontainers (PostgreSQL)
- Cucumber (BDD)
- JaCoCo (coverage check)

## Project Structure

```text
src/main/java/org/example/miniecom
|- common        # shared error model + global exception handling
|- config        # OpenAPI configuration
|- order         # order domain, DTOs, repository, service, controller
|- payment       # payment domain, DTOs, repository, service, gateway
|- product       # product domain, DTOs, repository, service, controller

src/test
|- java          # unit + integration + cucumber step definitions
|- resources
   |- application-test.yaml
   |- features   # cucumber feature files
```

## Domain Overview

- `Product`: name, price, stock
- `Order`: userId, status (`PENDING`, `PAID`, `CANCELLED`), totalAmount, items
- `OrderItem`: productId, quantity, price snapshot
- `Payment`: order, method, status, amount, transaction/failure metadata

### Stock Behavior

- Creating an order reserves stock.
- Updating an order reconciles stock deltas.
- Deleting an order restores reserved stock.
- Order creation/update fails when requested quantity exceeds available stock.

### Payment Behavior

- Only `PENDING` orders can be paid.
- Successful payment transitions order to `PAID`.
- Gateway failures are returned as `502 Bad Gateway`.
- Simulated gateway behavior:
  - `CREDIT_CARD` with token starting `cc_fail` -> failure
  - `PAYPAL` with token starting `pp_fail` -> failure

## Prerequisites

- JDK 21+
- Docker + Docker Compose (for local PostgreSQL)
- (Optional) Maven 3.9+ if you do not use the wrapper

## Configuration

Application defaults are in `src/main/resources/application.yaml`.

Environment variables (or defaults):

- `POSTGRES_DB` (default: `mini_ecom`)
- `POSTGRES_USER` (default: `mini_ecom`)
- `POSTGRES_PASSWORD` (default: `mini_ecom`)

Example `.env`:

```env
POSTGRES_DB=mini_ecom
POSTGRES_USER=mini_ecom
POSTGRES_PASSWORD=mini_ecom
```

## Running Locally

1. Start PostgreSQL:

```bash
docker compose up -d
```

2. Run the API:

```bash
./mvnw spring-boot:run
```

3. API base URL:

```text
http://localhost:8080
```

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Testing

### Run All Tests

```bash
./mvnw test
```

### Run BDD/Smoke Scenarios Only

```bash
./mvnw test -Dcucumber.filter.tags="@smoke"
```

### Coverage Report and Threshold Check

```bash
./mvnw verify
```

JaCoCo enforces a minimum line coverage ratio of `0.80` at bundle level.

### Test Types in This Project

- Unit tests for services/controllers/gateway
- Integration tests with Testcontainers PostgreSQL
- Cucumber scenarios in `src/test/resources/features`

Cucumber reports are generated under:

- `target/cucumber-reports/cucumber.html`
- `target/cucumber-reports/cucumber.json`

## Notes

- Runtime uses virtual threads (`spring.threads.virtual.enabled=true`).
- Local JPA schema mode is `update`.
- Test schema mode is `create-drop`.

## Useful Commands

```bash
# start DB
docker compose up -d

# stop DB
docker compose down

# run app
./mvnw spring-boot:run

# run all tests
./mvnw test

# run verify (tests + jacoco checks)
./mvnw verify
```
