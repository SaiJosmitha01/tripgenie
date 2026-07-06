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

## Observability, Caching, and Resilience

Trip service uses Redis-backed Spring caching for read-heavy backend paths:

- `tripById`: owned trip detail responses, keyed as `user:{userId}:trip:{tripId}`
- `tripLists`: authenticated user's trip list queries, keyed by user id, filters, page, size, and sort
- `placeResolutions`: successful Google Maps place lookups, keyed by normalized place query

Cache entries are evicted when trip details, itineraries, budgets, AI-generated itineraries, or enriched location metadata change. JWTs, API keys, and secrets are never cached.

Runtime cache configuration:

- `REDIS_HOST`: Redis host, default `localhost`
- `REDIS_PORT`: Redis port, default `6379`
- `REDIS_PASSWORD`: optional Redis password
- `REDIS_TIMEOUT`: Redis command timeout, default `2s`
- `TRIP_CACHE_TTL`: trip detail cache TTL, default `10m`
- `TRIP_LIST_CACHE_TTL`: trip list cache TTL, default `5m`
- `PLACE_RESOLUTION_CACHE_TTL`: Google Maps place cache TTL, default `7d`

Every backend request supports `X-Correlation-Id`. If the caller sends the header, TripGenie keeps it; otherwise a UUID is generated. The same value is returned in the response header and added to application logs through MDC as `correlationId`.

Trip service external client resilience settings:

- `AI_CONNECT_TIMEOUT`: Groq connect timeout, default `5s`
- `AI_READ_TIMEOUT`: Groq read timeout, default `60s`
- `AI_RETRY_ATTEMPTS`: transient Groq retry attempts, default `2`
- `GOOGLE_MAPS_CONNECT_TIMEOUT`: Google Maps connect timeout, default `5s`
- `GOOGLE_MAPS_READ_TIMEOUT`: Google Maps read timeout, default `20s`
- `GOOGLE_MAPS_RETRY_ATTEMPTS`: transient Google Maps retry attempts, default `2`

Transient 5xx, 429, and network access failures are retried. Validation, missing configuration, and 4xx authentication or caller errors are not retried.

Useful local trip-service health and metrics endpoints:

- `http://localhost:8083/actuator/health`
- `http://localhost:8083/actuator/health/liveness`
- `http://localhost:8083/actuator/health/readiness`
- `http://localhost:8083/actuator/metrics`
- `http://localhost:8083/actuator/prometheus`

The trip service emits Micrometer timers for trip CRUD operations, AI itinerary generation, and Google Maps enrichment.

## Event-Driven Notifications

TripGenie uses Kafka for trip and itinerary notification events.

Configured topics:

- `trip-created`
- `trip-updated`
- `itinerary-generated`
- `notifications`

The trip service publishes typed JSON events when trips are created, trips are updated, and AI itineraries are generated. The notification service consumes those events, logs simulated email delivery through `EmailService`, and republishes a typed notification event to `notifications`.

Runtime configuration:

- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker list, default `localhost:9092`
- `NOTIFICATION_KAFKA_GROUP`: notification consumer group, default `notification-service`

Kafka consumers retry failed records 3 times with a 1 second fixed backoff, then publish failed records to `<source-topic>.DLT`.

## Phase 0 Scope

This foundation intentionally avoids authentication, AI planning, and business logic. Those features should start in later phases after platform boundaries, CI, and runtime conventions are stable.
