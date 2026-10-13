# Tech Debt Log

| # | Item | Area | Impact | Raised | Status |
|---|------|------|--------|--------|--------|
| TD1 | Local stack and Keycloak realm import have not been run end to end yet; verify with `make up` | infra | Realm import errors would block local login | 2026-10-10 | Open |
| TD2 | GitHub Actions are pinned to major tags, not commit SHAs | ci | Exposed to a compromised action tag | 2026-10-10 | Open |
| TD3 | PII redaction masks unquoted `key=value` text only up to the first space | common | Multi-word values can partly leak into logs | 2026-10-10 | Open |
| TD4 | Root `pom.xml` overrides Boot's Tomcat (11.0.26), Jackson 3 (3.1.7) and Jackson 2 (2.21.7) versions for security fixes | build | Overrides can drift from Boot's tested set; remove when Boot 4.1.x catches up | 2026-10-10 | Open |
| TD5 | Gateway does not trust `X-Forwarded-For`, so behind an ingress the per-IP limit sees the proxy's address (signed-in users are limited per user, unaffected) | gateway | Anonymous traffic shares one bucket behind a proxy; set `server.forward-headers-strategy` once the ingress is known (Q5) | 2026-10-13 | Open |
| TD6 | Gateway `429` responses have no RFC 7807 body (Spring Cloud Gateway's rate limiter writes an empty one) | gateway | Clients get the status and `X-RateLimit-*` headers but no problem document | 2026-10-13 | Open |
