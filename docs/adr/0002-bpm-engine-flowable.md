# 0002 — Use Flowable 7 as the BPM Engine

- Status: Proposed
- Date: 2026-10-10
- Deciders: Platform engineering lead, PHA ICT manager (pending review)
- Context: `.cursorrules` requires choosing between Camunda 8 (Zeebe) and Flowable 7 for BPMN 2.0 workflows (leave, work orders, procurement, KPI reporting) with SLAs, escalation, and audit. The PHA operates on constrained budgets and infrastructure, must run edge nodes at L5 hospitals with intermittent connectivity, and already standardises on Java 17 + Spring Boot (4.x per ADR 0006) and PostgreSQL 15.
- Decision: Adopt Flowable 7 (open source, Apache 2.0), embedded in `bpm-service` as a Spring Boot application, persisting process state in PostgreSQL. Workflow lifecycle events (`process.started`, `task.created`, `task.completed`, `sla.breached`) are published to the event bus via Flowable event listeners. BPMN definitions live in `libs/bpmn`.
- Consequences:
  - Positive: no production licence cost; reuses the existing PostgreSQL, backup, and encryption-at-rest setup; native Spring Boot integration; small footprint suitable for edge deployment; mature user-task and timer support for SLAs and escalation.
  - Negative: less horizontal throughput than Zeebe at very high volumes; fewer bundled operational UIs (task list / monitoring must be built or configured).
  - Neutral: BPMN 2.0 XML remains portable, so a later migration stays possible.
- Alternatives considered:
  - Camunda 8 (Zeebe) — rejected for now: self-managed production use requires a commercial licence, and it adds Zeebe brokers plus Elasticsearch/OpenSearch to operate, which is heavy for provincial and edge infrastructure. Its scale advantages exceed projected PHA workflow volumes.
