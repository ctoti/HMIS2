# 0006 — Use Spring Boot 4.x Instead of 3.x

- Status: Accepted
- Date: 2026-10-10
- Deciders: Project owner
- Context: `.cursorrules` specified Spring Boot 3.x. The first CI dependency scan (OSV-Scanner) of Spring Boot 3.5.6 reported dozens of known vulnerabilities in Tomcat, Spring MVC, Jackson and Spring Boot itself, several rated critical (CVSS 9.1–9.8). Open-source support for Spring Boot 3.5 and Spring Framework 6.2 ended in mid-2026, and two critical Spring MVC advisories list a fix only in Spring Framework 7.0.9. A health platform handling staff and patient-adjacent data cannot start on an unsupported, vulnerable baseline.
- Decision: Use Spring Boot 4.1.x (initially 4.1.1, which ships Spring Framework 7.0.9) with Java 17 as the minimum language level. Use Jackson 3 (`tools.jackson`), the Boot 4 default, and logstash-logback-encoder 9.x, which is built on Jackson 3. Use the Boot 4 starter names (e.g. `spring-boot-starter-webmvc`). Keep to the latest Boot 4.1.x patch release.
- Consequences:
  - Positive: supported, patched baseline; dependency scan can gate CI; one Jackson major version across the platform.
  - Negative: fewer tutorials and third-party libraries certified for Boot 4 and Jackson 3 than for 3.x; each new dependency must be checked for Boot 4 compatibility.
  - Neutral: Spring Cloud Gateway (ADR 0004) must use the release train for Boot 4.1. Flowable (ADR 0002) must use a release that supports Spring Boot 4; confirm before scaffolding `bpm-service` (Q8).
- Alternatives considered:
  - Stay on 3.5 and pin patched transitive versions — rejected: some critical fixes are not available for the 6.2 line in open source, so findings would have to be suppressed.
  - Buy commercial Spring support for 3.5 — rejected: recurring licence cost and a private artifact repository for no functional gain over 4.x.
