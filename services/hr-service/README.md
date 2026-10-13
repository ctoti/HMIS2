# hr-service

Staff records and leave requests for the PHA (Phase 2). Spring Boot 4.1 (servlet), PostgreSQL 15 with Flyway, called
through the API gateway at `/api/v1/hr/**`.

## API

The OpenAPI 3.0 document is [`src/main/resources/openapi/hr-service.yaml`](src/main/resources/openapi/hr-service.yaml).
`OpenApiContractTest` fails the build if it and the controllers list different endpoints.

| Endpoint | Who may call it |
|----------|-----------------|
| `POST /api/v1/hr/staff` | `hr-officer` at the record's facility, or `platform-admin` |
| `GET /api/v1/hr/staff?facilityCode=` | `hr-officer` at that facility, or `platform-admin` |
| `GET /api/v1/hr/staff/me` | Any signed-in user with a linked staff record |
| `GET /api/v1/hr/staff/{id}` | The staff member, `hr-officer` at their facility, or `platform-admin` |
| `POST /api/v1/hr/leave-requests` | `staff`, for their own record; needs an `Idempotency-Key` header |
| `GET /api/v1/hr/leave-requests` | Own requests; with `facilityCode`, `hr-officer` at that facility or `platform-admin` |
| `GET /api/v1/hr/leave-requests/{id}` | The requester, `hr-officer` at the request's facility, or `platform-admin` |
| `POST /api/v1/hr/leave-requests/{id}/cancel` | The requester, while the request is `SUBMITTED` |

Lists take `page`, `size` (default 20, at most 100) and `sort=field,asc|desc`.

## Behaviour

| Concern | Behaviour |
|---------|-----------|
| Authentication | Validates the Keycloak access token itself (issuer and `pha-hmg-api` audience), even behind the gateway. 401 and 403 are RFC 7807 bodies. |
| Authorisation | Realm roles from `realm_access.roles`; facility scope from `/facilities/<code>` groups. Checked with `@PreAuthorize` and in the services for record-level rules. |
| Leave lifecycle | `SUBMITTED` → `CANCELLED` only. Approval will come from the BPM workflow (ADR 0009). Overlapping submitted requests for one person are rejected. Days are counted as calendar days (Q11). |
| Idempotency | Same `Idempotency-Key` and body returns the original request with `200`; same key with a different body is `409`. Concurrent retries create one request. |
| Audit | Every write appends to the hash-chained audit log (`libs/common-audit`) in the same transaction. Entries hold IDs, facility codes, leave types and dates, never names. |
| Errors | RFC 7807 with `correlationId`. Business conflicts are `409` with a `urn:pha:problem:hr:<reason>` type. |
| Logs | JSON with PII masking, via `common-java`. |

## Configuration

| Variable | Default |
|----------|---------|
| `PORT` | `8081` |
| `HR_DB_URL` | `jdbc:postgresql://localhost:5432/hr` |
| `HR_DB_USERNAME`, `HR_DB_PASSWORD` | `pha`, empty |
| `OIDC_ISSUER_URI` | `http://localhost:8180/realms/pha-hmg` |
| `OIDC_AUDIENCE` | `pha-hmg-api` |

Flyway applies `db/migration` (tables `staff`, `leave_request`) and `common-audit` its own `audit` schema at startup.

## Run locally

```sh
make up                                   # PostgreSQL and Keycloak from infra/docker
export HR_DB_PASSWORD=<POSTGRES_PASSWORD from infra/docker/.env>
mvn -pl services/hr-service -am spring-boot:run
```

Link a staff record to a Keycloak user by sending that user's ID as `userId` when creating the record.

## Test

```sh
mvn -pl services/hr-service -am verify    # needs Docker for the PostgreSQL Testcontainer
```

## Container image

```sh
mvn -B -pl services/hr-service -am package -DskipTests
docker build -t pha/hr-service services/hr-service
```
