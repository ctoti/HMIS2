# Master Backlog

Current epic: **Phase 0 — Foundations (Weeks 1–2)**

## Phase 0 — Foundations

- [x] Agent working rules (`.cursorrules`)
- [x] ADR process and template (ADR 0001)
- [ ] BPM engine ADR accepted (ADR 0002, Q1)
- [ ] Event bus ADR (Q2)
- [ ] API gateway ADR (Q3)
- [ ] Monorepo scaffold per `.cursorrules` §3 with root `Makefile` (`make test`, `make up`)
- [ ] CI pipeline: build, test, lint, SAST, dependency scan
- [ ] Local dev stack (`infra/docker`): PostgreSQL 15, Redis 7, Keycloak, event bus
- [ ] Keycloak realm: OIDC clients, facility-scoped roles, MFA for admin + governance
- [ ] `libs/common-java`: RFC 7807 errors, correlation ID, JSON logging with PII redaction, hash-chained audit log
- [ ] `libs/common-ts`: API client, auth, logger, i18n setup
- [ ] Terraform skeleton for dev environment

## Phase 1 — Core Platform

- [ ] integration-service (Camel) with retry + circuit breaker
- [ ] MDM: facilities, positions, cost centres
- [ ] bpm-service with first workflow (leave request)
- [ ] governance-service: KPI library
- [ ] web-portal shell

## Phase 2–4

See `.cursorrules` §11.
