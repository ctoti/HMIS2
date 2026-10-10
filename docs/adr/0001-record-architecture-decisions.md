# 0001 — Record Architecture Decisions

- Status: Accepted
- Date: 2026-10-10
- Deciders: Platform engineering lead
- Context: The HMG Platform spans many services, teams, and AI coding agents over a 48-week delivery. Decisions about technology and structure must be traceable, reviewable, and discoverable so that agents and humans do not re-litigate or silently override them.
- Decision: Record every architecturally significant decision as an ADR in `docs/adr/NNNN-<kebab-title>.md`, using the template in `0000-template.md`. ADRs are numbered sequentially and never deleted; a changed decision is recorded as a new ADR that supersedes the old one. Any substitution of the stack defined in `.cursorrules` requires an ADR.
- Consequences:
  - Positive: decisions are auditable; agents can check prior decisions before acting; onboarding is faster.
  - Negative: small overhead per decision.
  - Neutral: ADRs start as `Proposed` and move to `Accepted` after human review.
- Alternatives considered:
  - Wiki pages — rejected; drift from the code and are not reviewed in PRs.
  - Decisions in PR descriptions only — rejected; hard to discover later.
