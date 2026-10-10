# @pha/common-ts

Shared TypeScript utilities for PHA web apps and the rural client. Consumed through the bundler (Vite); it is not published as a standalone Node package.

- `createLogger` — structured logger that redacts PII from messages and context. Use it instead of `console.*`.
- `redactValue` / `redactText` / `isSensitiveKey` — the same PII rules as `common-java`.
- `toProblemDetails` / `isProblemDetails` — normalise API error bodies to RFC 7807.
- `createCorrelationId` / `CORRELATION_ID_HEADER` — correlation IDs accepted by backend services.

```sh
npm test -w @pha/common-ts       # Vitest with 80% coverage thresholds
npm run typecheck -w @pha/common-ts
```
