# api-gateway

The single entry point for every PHA API call (ADR 0004). Spring Cloud Gateway 5 on Spring Boot 4.1 (WebFlux), Spring Cloud 2025.1.3.

## What it does

| Concern | Behaviour |
|---------|-----------|
| Authentication | Validates the Keycloak access token on every request (issuer and `pha-hmg-api` audience). Missing or invalid tokens get `401` with an RFC 7807 body and `WWW-Authenticate: Bearer`. Backend services still validate the token themselves. |
| Correlation IDs | Accepts a valid `X-Correlation-Id` or creates one, forwards it to the backend and returns it, including on rejected requests. |
| Rate limiting | Redis token bucket per signed-in user (per client IP when there is no user). Defaults: 10 requests/second, bursts of 20. Over the limit returns `429`. |
| Resilience | `GET` requests are retried twice on 5xx or connection errors (100 ms, then 200 ms). A circuit breaker wraps every route; when a backend is down or slower than `BACKEND_TIMEOUT`, callers get `503` with `Retry-After: 30` and an RFC 7807 body. |
| CORS | Only origins in `PORTAL_ORIGINS` may call the API from a browser. |
| Health | `/actuator/health` (with liveness and readiness probes) is public; everything else needs a token. |
| Logs | JSON with PII masking, via `common-java`. |

## Routes

| Path | Service | Default upstream |
|------|---------|------------------|
| `/api/v1/hr/**` | hr-service | `http://localhost:8081` |
| `/api/v1/facilities/**` | facilities-service | `http://localhost:8082` |
| `/api/v1/projects/**` | projects-service | `http://localhost:8083` |
| `/api/v1/governance/**` | governance-service | `http://localhost:8084` |
| `/api/v1/bpm/**` | bpm-service | `http://localhost:8085` |
| `/api/v1/sync/**` | sync-service | `http://localhost:8086` |
| `/api/v1/notifications/**` | notification-service | `http://localhost:8087` |

Paths are forwarded unchanged. Override each upstream with `<NAME>_SERVICE_URI` (for example `HR_SERVICE_URI`).

## Configuration

| Variable | Default |
|----------|---------|
| `PORT` | `8080` |
| `OIDC_ISSUER_URI` | `http://localhost:8180/realms/pha-hmg` |
| `OIDC_AUDIENCE` | `pha-hmg-api` |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | `localhost`, `6379`, empty |
| `RATE_LIMIT_PER_SECOND`, `RATE_LIMIT_BURST` | `10`, `20` |
| `BACKEND_TIMEOUT` | `10s` |
| `PORTAL_ORIGINS` | `http://localhost:5173` (comma-separated) |

## Run locally

```sh
make up                                   # Redis and Keycloak from infra/docker
export REDIS_PASSWORD=<value from infra/docker/.env>
mvn -pl services/api-gateway -am spring-boot:run
```

## Test

```sh
mvn -pl services/api-gateway -am verify   # needs Docker for the Redis Testcontainer
```

## Container image

```sh
mvn -B -pl services/api-gateway -am package -DskipTests
docker build -t pha/api-gateway services/api-gateway
```
