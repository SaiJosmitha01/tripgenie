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

## Backend Configuration

The default backend profile is `local`. Local development keeps localhost PostgreSQL, Redis, and Kafka defaults so the existing Docker Compose workflow continues to work.

Production uses `SPRING_PROFILES_ACTIVE=prod` and reads cloud configuration from environment variables. Do not commit real secrets in `.env`, `.env.example`, `render.yaml`, or service YAML files.

Required production variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `JWT_SECRET`
- `DATABASE_URL`, or `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`
- `CORS_ALLOWED_ORIGINS`, set to the future React frontend origin

Trip service also requires:

- `GROQ_API_KEY`
- `GROQ_MODEL`
- `GOOGLE_MAPS_API_KEY`

Optional production variables:

- `REDIS_URL`
- `CACHE_ENABLED`, defaults to `false` in prod for safe startup without Redis
- `REDIS_HEALTH_ENABLED`, defaults to `false` in prod
- `KAFKA_ENABLED`, defaults to `false` in prod
- `KAFKA_BOOTSTRAP_SERVERS`
- `NOTIFICATION_KAFKA_GROUP`
- `TRIP_AUDIT_KAFKA_GROUP`

Spring Boot uses Render's `PORT` variable in the `prod` profile. Local fallback ports are:

- auth-service: `8081`
- user-service: `8082`
- trip-service: `8083`
- notification-service: `8085`

Deployment-friendly health checks:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/metrics`
- `/actuator/prometheus`

Actuator health, liveness, readiness, metrics, structured logging, and `X-Correlation-Id` support remain enabled. Production health details are not exposed.

## Production Docker Images

Build images from the repository root. Each Dockerfile builds only the required Maven module and dependencies, then copies the Spring Boot jar into a Java 21 runtime image that runs as a non-root user.

```sh
docker build -f apps/backend/auth-service/Dockerfile -t tripgenie-auth-service .
docker build -f apps/backend/user-service/Dockerfile -t tripgenie-user-service .
docker build -f apps/backend/trip-service/Dockerfile -t tripgenie-trip-service .
docker build -f apps/backend/notification-service/Dockerfile -t tripgenie-notification-service .
```

## Render Preparation

`render.yaml` is a deploy-later blueprint for backend services only. It does not deploy the frontend and should not be applied until the React frontend is complete and the final service topology is confirmed.

The blueprint defines:

- four backend Docker web services
- one managed PostgreSQL database reference
- one managed Render Key Value Redis-compatible reference for trip-service caching
- health checks at `/actuator/health/readiness`
- placeholder secret variables with `sync: false`
- `KAFKA_ENABLED=false` by default because no broker is provisioned in the blueprint

PostgreSQL can be configured with Render's `DATABASE_URL`. TripGenie converts `postgres://...` and `postgresql://...` URLs into JDBC datasource URLs at startup. Explicit Spring datasource variables still work and take precedence.

Redis is optional in production. Set `REDIS_URL`, `CACHE_ENABLED=true`, and `REDIS_HEALTH_ENABLED=true` when Redis is provisioned. Leave caching disabled if Redis is not available.

Kafka is optional in production. Local development keeps Kafka enabled by default. In production, leave `KAFKA_ENABLED=false` until a broker is configured; trip-service and notification-service will start without listeners, topic creation, or publish attempts.

Future Confluent Cloud setup:

- set `KAFKA_ENABLED=true`
- set `KAFKA_BOOTSTRAP_SERVERS` to the Confluent bootstrap server list
- add the required SASL/SSL Spring Kafka properties as environment variables
- set `NOTIFICATION_KAFKA_GROUP` and `TRIP_AUDIT_KAFKA_GROUP`
- keep retry and DLT behavior unchanged

Future frontend deployment flow:

- complete and build the React app
- deploy the frontend separately
- set `CORS_ALLOWED_ORIGINS` on each backend service to the frontend URL
- configure frontend API base URLs for the backend service URLs
- only then apply or adapt the Render backend blueprint

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

## Admin Audit and Dashboard APIs

Trip service exposes admin-only operational APIs:

- `GET /admin/audit-logs`: paginated audit log search
- `GET /admin/audit-logs/{id}`: single audit log lookup
- `GET /admin/dashboard/summary`: operational counts and recent failures

Audit log search supports these optional query parameters:

- `action`: one of `USER_REGISTERED`, `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `TRIP_CREATED`, `TRIP_UPDATED`, `TRIP_DELETED`, `ITINERARY_GENERATED`, `LOCATIONS_ENRICHED`, `NOTIFICATION_PROCESSED`
- `userId`: user UUID
- `entityType`: one of `USER`, `TRIP`, `AI_GENERATION`, `LOCATION_ENRICHMENT`, `NOTIFICATION`
- `from` / `to`: ISO-8601 timestamps
- `status`: `SUCCESS` or `FAILURE`
- `page` / `size`: zero-based pagination, default `0` and `20`

Admin endpoints require a JWT with `ADMIN` role. A JWT with only `USER` role is denied for `/admin/**`; existing user-facing APIs keep their current `USER` role behavior.

Audit rows include user id, action type, entity type, entity id, timestamp, status, correlation id, and optional JSON metadata. Trip service records trip create/update/delete, AI itinerary generation, location enrichment, and processed notification events. Auth service writes registration and login success/failure audit rows using the same `audit_logs` table shape.

The dashboard summary returns total trips, total AI itinerary generations, total successful location enrichment operations, total processed notification events, and the 10 most recent failed audit records.

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
