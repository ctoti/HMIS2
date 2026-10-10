# Open Questions

Questions needing human review. Agents proceed with the stated default and record the decision in an ADR.

| # | Question | Default taken | ADR | Status |
|---|----------|---------------|-----|--------|
| Q1 | Camunda 8 or Flowable 7 for the BPM engine? | Flowable 7 (licence cost, PostgreSQL reuse, edge footprint) | [0002](adr/0002-bpm-engine-flowable.md) | Open |
| Q2 | Kafka or RabbitMQ for the event bus? | Kafka 3.x in KRaft mode (replay for rural sync; matches BPMN event names) | [0003](adr/0003-event-bus-kafka.md) | Open |
| Q3 | Kong or Spring Cloud Gateway for the API gateway? | Spring Cloud Gateway (same stack as the services, Redis rate limiter) | [0004](adr/0004-api-gateway-spring-cloud-gateway.md) | Open |
| Q4 | GitHub Actions or GitLab CI? | GitHub Actions (repository is hosted on GitHub) | — | Open |
| Q5 | Hosting target for dev/prod (government data centre, cloud region, hybrid)? | Undecided — drives Terraform provider and data-residency design | — | Open |
| Q6 | What integration interfaces do IFMS, Alesco, and Clinical HIMS expose (API, file drop, DB views)? | Undecided — blocks integration-service design | — | Open |
| Q7 | Maven or Gradle; which JS monorepo tooling? | Maven multi-module + npm workspaces | [0005](adr/0005-build-tooling.md) | Open |
