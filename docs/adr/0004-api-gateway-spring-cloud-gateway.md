# 0004 — Use Spring Cloud Gateway as the API Gateway

- Status: Proposed
- Date: 2026-10-10
- Deciders: Platform engineering lead, PHA ICT manager (pending review)
- Context: `.cursorrules` requires a gateway (Kong or Spring Cloud Gateway) that enforces JWT validation and per-user and per-IP rate limiting, and that every client call goes through.
- Decision: Implement `services/api-gateway` with Spring Cloud Gateway (the release train matching Spring Boot 4.1, per ADR 0006), using the Spring Security OAuth2 resource server against Keycloak and the Redis rate limiter.
- Consequences:
  - Positive: same language, build, observability and security libraries as the other Java services; Redis is already in the stack; no extra licence or control plane.
  - Negative: no admin UI or plugin marketplace; routing changes ship as code changes.
  - Neutral: can sit behind a Kubernetes ingress controller for TLS termination.
- Alternatives considered:
  - Kong — rejected for now: capable, but adds a separate runtime (Lua/OpenResty), its own database or declarative config, and some features need Kong Enterprise.
