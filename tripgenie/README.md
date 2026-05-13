# TripGenie Enterprise Platform

TripGenie is organized as a monorepo with a modular-monolith-first backend, a React frontend, and infrastructure scaffolding for local and future deployment workflows.

## Structure

```text
apps/
  backend/
    api-gateway/
    auth-service/
    user-service/
    trip-service/
    ai-planner-service/
    notification-service/
    common-library/
  frontend/
infra/
  docker/
  k8s/
docs/
.github/workflows/
```

## Backend

Requirements:

- Java 21
- Maven 3.9+

Build all backend modules:

```sh
mvn clean verify
```

Run a service locally:

```sh
mvn -pl apps/backend/api-gateway spring-boot:run
```

Each Spring Boot service includes Actuator and OpenAPI. Once a service is running:

- Health: `http://localhost:8080/actuator/health`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`

## Frontend

Requirements:

- Node.js 20+
- npm 10+

```sh
cd apps/frontend
npm ci
npm run build
```

## Local Infrastructure

Start PostgreSQL, Redis, and Kafka:

```sh
docker compose -f infra/docker/docker-compose.yml up -d
```

Validate the Compose file:

```sh
docker compose -f infra/docker/docker-compose.yml config
```

## Phase 0 Scope

This foundation intentionally avoids authentication, AI planning, and business logic. Those features should start in later phases after platform boundaries, CI, and runtime conventions are stable.
