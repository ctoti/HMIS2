# Tech Debt Log

| # | Item | Area | Impact | Raised | Status |
|---|------|------|--------|--------|--------|
| TD1 | Local stack and Keycloak realm import have not been run end to end yet; verify with `make up` | infra | Realm import errors would block local login | 2026-10-10 | Open |
| TD2 | GitHub Actions are pinned to major tags, not commit SHAs | ci | Exposed to a compromised action tag | 2026-10-10 | Open |
| TD3 | PII redaction masks unquoted `key=value` text only up to the first space | common | Multi-word values can partly leak into logs | 2026-10-10 | Open |
