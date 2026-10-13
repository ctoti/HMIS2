# 0005 — Maven Multi-Module and npm Workspaces for Builds

- Status: Proposed
- Date: 2026-10-10
- Deciders: Platform engineering lead
- Context: The monorepo holds Java services and libraries plus TypeScript apps and libraries. `.cursorrules` does not name build tools.
- Decision: Java builds use a root Maven aggregator (`pom.xml`) whose parent is `spring-boot-starter-parent`; each Java service and library is a module. TypeScript builds use npm workspaces from the root `package.json`. JaCoCo enforces at least 80% line coverage per Java module. A root `Makefile` exposes `make test`, `make up`, etc. across both.
- Consequences:
  - Positive: boring, widely known tools; dependency versions come from the Spring Boot BOM; no extra monorepo tool to learn.
  - Negative: no incremental build caching across the whole repo; CI builds every module on each change.
  - Neutral: can move to a dedicated monorepo tool later if build times grow.
- Alternatives considered:
  - Gradle — rejected: faster incremental builds, but build scripts are code and harder to keep uniform across agents.
  - pnpm / Nx / Turborepo — rejected: extra tooling not justified at this size.
