# Master Backlog

Current epic: **Phase 0 — Foundations (Weeks 1–2)**

## Phase 0 — Foundations

- [x] Agent working rules (`.cursorrules`)
- [x] ADR process and template (ADR 0001)
- [ ] BPM engine ADR accepted (ADR 0002, Q1)
- [x] Event bus ADR proposed (ADR 0003, Q2) — awaiting acceptance
- [x] API gateway ADR proposed (ADR 0004, Q3) — awaiting acceptance
- [x] Monorepo tooling: Maven aggregator, npm workspaces, root `Makefile` (ADR 0005)
- [x] Spring Boot 4.1 baseline (ADR 0006)
- [x] CI pipeline: build, test, lint, gitleaks, Semgrep, OSV-Scanner (`.github/workflows/ci.yml`)
- [x] CodeQL code scanning for Java and TypeScript (`.github/workflows/codeql.yml`)
- [x] Local dev stack (`infra/docker`): PostgreSQL 15, Redis 7, Keycloak 26, Kafka 3.9
- [x] Keycloak realm: roles, facility groups claim, PKCE web client, TOTP for admin + governance
- [x] `libs/common-java`: RFC 7807 errors, correlation ID, JSON logging with PII redaction, hash-chained audit log
- [x] `libs/common-ts`: logger, PII redaction, problem details, correlation ID
- [x] `libs/common-ts`: API client (bearer token, correlation ID, Idempotency-Key, RFC 7807 errors, retry with backoff)
- [x] `apps/web-portal` shell: OIDC sign-in, role-based navigation, i18n (en, tpi), offline banner (ADR 0007)
- [x] `services/api-gateway`: Spring Cloud Gateway with JWT validation, per-user rate limiting, retries, circuit breaker, CORS (ADR 0004 still Proposed)
- [x] Audit log persistence: `libs/common-audit` — append-only PostgreSQL table, DB-enforced, hash-chained, verifiable (ADR 0008)
- [ ] First service to record audited writes with `AuditLog` (hr-service, Phase 2)
- [ ] Terraform skeleton for dev environment (blocked on Q5: hosting target)

## Phase 1 — Core Platform

- [ ] integration-service (Camel) with retry + circuit breaker
- [ ] MDM: facilities, positions, cost centres
- [ ] bpm-service with first workflow (leave request)
- [ ] governance-service: KPI library

## Phase 2–4

See `.cursorrules` §11.
