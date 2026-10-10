# Runbook — Local Development Stack

Runs PostgreSQL 15, Redis 7, Kafka 3.9 (KRaft) and Keycloak 26 with Docker Compose. Local traffic is plaintext on `127.0.0.1` only; deployed environments use TLS 1.3 / mTLS.

## Prerequisites

- Docker with the Compose plugin
- Java 17+ and Maven 3.9+
- Node.js 20+

## First run

```sh
cp infra/docker/.env.example infra/docker/.env
# Set your own values for every empty variable in infra/docker/.env
make up
```

`make up` waits until every container reports healthy.

| Service    | Address                  | Notes |
|------------|--------------------------|-------|
| PostgreSQL | `localhost:5432`         | One database per service: `hr`, `facilities`, `projects`, `governance`, `bpm`, `sync`, plus `keycloak` |
| Redis      | `localhost:6379`         | Password from `REDIS_PASSWORD` |
| Kafka      | `localhost:9092`         | Auto topic creation is off; services declare their topics |
| Keycloak   | `http://localhost:8180`  | Admin console login from `KEYCLOAK_ADMIN_USER` / `KEYCLOAK_ADMIN_PASSWORD` |

## Keycloak realm

The `pha-hmg` realm is imported from `infra/docker/keycloak/pha-hmg-realm.json` on first start:

- Realm roles: `platform-admin`, `governance-officer`, `hr-officer`, `facilities-officer`, `projects-officer`, `staff` (default).
- Facility scope: add users to subgroups of `/facilities` (e.g. `/facilities/PMGH`). Group paths are emitted in the `groups` token claim.
- MFA: the `browser-mfa` login flow requires TOTP for users with `platform-admin` or `governance-officer`; users are prompted to enrol on first login.
- Client `web-portal`: public client with PKCE (S256), redirect `http://localhost:5173/*`, access tokens carry audience `pha-hmg-api`.

No users are imported. Create test users in the admin console (realm `pha-hmg` → Users).

The import only runs when the realm does not exist yet. To re-import after editing the JSON, run `docker compose -f infra/docker/compose.yaml down -v` (this deletes all local data).

## Common commands

```sh
make logs   # follow all container logs
make down   # stop containers, keep data
```
