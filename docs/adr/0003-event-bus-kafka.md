# 0003 — Use Apache Kafka as the Event Bus

- Status: Proposed
- Date: 2026-10-10
- Deciders: Platform engineering lead, PHA ICT manager (pending review)
- Context: `.cursorrules` requires asynchronous, event-based communication between services and names Kafka or RabbitMQ. The BPMN standard in `.cursorrules` already specifies Kafka events (`process.started`, `task.created`, `task.completed`, `sla.breached`). Rural sync needs durable, replayable streams so edge nodes can catch up after outages.
- Decision: Use Apache Kafka 3.x in KRaft mode (no ZooKeeper) as the platform event bus. Topics are named `<domain>.<entity>.<event>` (e.g. `hr.leave-request.submitted`). Local development runs a single-node broker; production uses at least three brokers with replication factor 3.
- Consequences:
  - Positive: durable, ordered, replayable log suits sync catch-up and audit; first-class Apache Camel and Spring support; matches the event names already in the BPMN standard.
  - Negative: heavier to operate than RabbitMQ; needs monitoring and capacity planning.
  - Neutral: KRaft mode removes the ZooKeeper dependency, which reduces moving parts.
- Alternatives considered:
  - RabbitMQ — rejected: simpler to run, but message replay after consumption is limited (streams help, but are less mature in the Camel and Spring ecosystem), which weakens offline catch-up.
