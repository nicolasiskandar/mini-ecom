# Mini Ecom

Mini e-commerce backend built with Spring Boot and PostgreSQL.

## What it does

- Manages products (`/products`)
- Manages order lifecycle (`/orders`)
- Processes order payments (`/orders/{id}/pay`)
- Adjusts stock on order create/update/delete
- Exposes OpenAPI docs with Swagger UI

## Tech stack

- Java 21
- Spring Boot 4
- Spring Web + Spring Data JPA + Bean Validation
- PostgreSQL 16
- Maven Wrapper
- Testcontainers, JUnit 5, Cucumber BDD
- JaCoCo

## Prerequisites

- JDK 21
- Docker (required for PostgreSQL and integration tests)

## Configuration

The app reads DB settings from environment variables:

- `POSTGRES_HOST` (default: `localhost`)
- `POSTGRES_DB` (default: `mini_ecom`)
- `POSTGRES_USER` (default: `mini_ecom`)
- `POSTGRES_PASSWORD` (default: `mini_ecom`)

You can start from:

```bash
cp .env.example .env
```

Then set values as needed.

## Run locally

1. Start PostgreSQL:

```bash
docker compose up -d postgres
```

2. Start the API:

```bash
./mvnw spring-boot:run
```

API base URL: `http://localhost:8080`

## Run with Docker Compose (app container)

```bash
docker compose --profile app up -d postgres app
```

This builds/runs the app image and starts both services.

## API docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Quick API examples

Create product:

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Keyboard","price":79.99,"stock":10}'
```

Create order:

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":101,"items":[{"productId":1,"amount":2}]}'
```

Pay order:

```bash
curl -X POST http://localhost:8080/orders/1/pay \
  -H "Content-Type: application/json" \
  -d '{"method":"CREDIT_CARD","paymentToken":"tok_visa_123"}'
```

Payment simulation behavior:

- `CREDIT_CARD` fails when token starts with `cc_fail`
- `PAYPAL` fails when token starts with `pp_fail`

## Testing

Run all tests (unit + integration + BDD) and coverage checks:

```bash
./mvnw clean verify
```

Run only smoke-tagged Cucumber scenarios:

```bash
./mvnw test -Dcucumber.filter.tags='@smoke'
```

Generated reports:

- Cucumber HTML: `target/cucumber-reports/cucumber.html`
- JaCoCo report: `target/site/jacoco/index.html`

## CI/CD

`Jenkinsfile` pipeline stages:

1. Checkout
2. `./mvnw -B clean verify`
3. Build Docker image (`mini-ecom-app`)
4. Deploy on `main` branch with Docker Compose profile `app`
