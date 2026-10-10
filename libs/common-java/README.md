# common-java

Shared building blocks for PHA Java services. Add it as a dependency (`com.pha:common-java`, version managed by the root POM); servlet services pick up the web pieces through Spring Boot auto-configuration.

| Package | What it gives you |
|---------|-------------------|
| `com.pha.common.error` | `ProblemDetailsAdvice` maps exceptions to RFC 7807 responses with a `correlationId` property. Unhandled exceptions return a generic 500 without the exception message. `ResourceNotFoundException` maps to 404. |
| `com.pha.common.web` | `CorrelationIdFilter` reads `X-Correlation-Id` (8–64 chars of `[A-Za-z0-9-]`) or generates a UUID, puts it in the MDC as `correlationId`, and echoes it on the response. |
| `com.pha.common.logging` | `PiiRedactor` masks names, DOB, MRN, national ID, emails and PNG phone numbers in text. `PiiValueMasker` applies it to JSON logs. |
| `com.pha.common.audit` | `AuditHashChain` seals `AuditRecord`s into a SHA-256 hash chain and finds the first broken link. Persistence is left to each service. |

## JSON logging with PII masking

In a service's `src/main/resources/logback-spring.xml`:

```xml
<configuration>
    <include resource="com/pha/common/logging/json-console-appender.xml"/>
    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE"/>
    </root>
</configuration>
```

Redaction is a safety net, not a licence to log personal data. Unquoted values are masked only up to the first space (`name=Mary Kila` leaves `Kila`), so log identifiers, never people.

## Build

```sh
mvn -B verify   # from the repository root; enforces 80% line coverage
```
