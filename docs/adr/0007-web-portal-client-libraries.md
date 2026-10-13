# 0007 — Web Portal Client Libraries

- Status: Proposed
- Date: 2026-10-13
- Deciders: Platform engineering lead (pending review)
- Context: `.cursorrules` fixes React 18, TypeScript, Vite, TanStack Query, Zustand, React Hook Form + Zod, TanStack Table, Recharts and Tailwind for the frontend, but names no library for routing, internationalisation or OIDC sign-in. The web portal shell needs all three.
- Decision:
  - Routing: React Router 7 (`react-router-dom`), declarative mode.
  - i18n: i18next with react-i18next; JSON resource files per language, bundled with the app so translations work offline. English (`en`) and Tok Pisin (`tpi`) to start.
  - Sign-in: `oidc-client-ts` with `react-oidc-context` against the Keycloak `web-portal` client (authorisation code + PKCE). Tokens are kept in `sessionStorage`, not `localStorage`.
  - Tests: Vitest with jsdom 29 and Testing Library. jsdom 30 needs Node 22 and the CI baseline is Node 20.
- Consequences:
  - Positive: all are widely used, actively maintained, open source and TypeScript-first; OIDC handling (PKCE, token refresh, sign-out) is not hand-written.
  - Negative: four more runtime dependencies to keep patched; react-oidc-context is maintained by a small team.
  - Neutral: role and facility claims are read in the browser for navigation only; services re-validate every token.
- Alternatives considered:
  - TanStack Router — rejected for now: less familiar to most contributors; React Router covers the shell's needs.
  - keycloak-js — rejected: ties the portal to Keycloak-specific APIs; oidc-client-ts works with any OIDC provider.
  - FormatJS / react-intl — rejected: heavier ICU tooling than the portal needs today.
