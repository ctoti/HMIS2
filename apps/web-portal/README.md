# web-portal

The PHA staff portal: React 18, TypeScript, Vite, TanStack Query, Zustand and Tailwind, with sign-in through the Keycloak `pha-hmg` realm. Library choices beyond `.cursorrules` are recorded in ADR 0007.

## What the shell does

- Signs users in with OIDC (authorisation code + PKCE). Tokens live in `sessionStorage`.
- Reads realm roles and `/facilities/<code>` groups from the access token to show the user's access and the modules they can open. This is for navigation only; services enforce access.
- Shows English or Tok Pisin; the choice is remembered per browser.
- Warns when the device is offline. Queries use TanStack Query's `offlineFirst` mode.
- Uses system fonts only, so nothing is fetched from the internet to render the UI.

Module pages (HR, facilities, projects, governance) are placeholders until their services exist.

## Run locally

```sh
make up                          # from the repo root: Keycloak on :8180
npm run dev -w @pha/web-portal   # http://localhost:5173
```

Create a test user in the Keycloak admin console (realm `pha-hmg`), give them a role, and optionally add them to a `/facilities/<code>` group. Override defaults by copying `.env.example` to `.env.local`.

## Checks

```sh
npm run typecheck -w @pha/web-portal
npm test -w @pha/web-portal      # Vitest + Testing Library, 80% coverage gate
npm run build -w @pha/web-portal
```

## Translations

Strings live in `src/i18n/locales/<lang>.json`; a test fails if a language is missing a key. The Tok Pisin file is a draft awaiting native-speaker review (Q9 in `docs/questions.md`).
