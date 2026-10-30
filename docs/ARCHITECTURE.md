# Application architecture

## Scope and request path

The first increment establishes one real request through the complete stack:

Browser → Vite development proxy → Spring Boot → JSON response → connection state.

The browser calls `/api/status` on its own origin. Vite forwards `/api` to `http://127.0.0.1:8080`, so local development and private Codespaces port forwarding use the same frontend code. The Vite host allowlist is derived from this Codespace's name and forwarding domain; it does not trust every host. No broad CORS policy is needed. Production routing is not configured.

## Responsibilities

| Location | Responsibility |
| --- | --- |
| `backend/.../JobSintonApplication.java` | Start Spring Boot |
| `backend/.../system/SystemStatusController.java` | Own the public service-status contract |
| `backend/.../matching/` | Pure Java matching domain, immutable facts, validation and versioned explanations |
| `frontend/src/api/status.ts` | HTTP request and runtime validation of external JSON |
| `frontend/src/App.tsx` | Shared Current shell, route composition, profile and shortlist state |
| `frontend/src/navigation.ts` | Hash navigation and direct-link handling |
| `frontend/src/pages/` | Home, search, companies, details and service status |
| `frontend/src/catalog/` | Typed fictional catalog and validated shortlist storage |
| `frontend/src/profile/model.ts` | Profile contract, normalization, validation, and saved-data parsing |
| `frontend/src/profile/storage.ts` | Guarded reads, writes, and removal of the browser-local profile |
| `frontend/src/profile/ProfileEditor.tsx` | Editable draft, validation feedback, and last-saved summary |
| `frontend/src/styles.css` | Responsive visual foundation without external assets |
| `frontend/tests/` | Browser journeys against the real API, plus controlled failures |
| `.github/workflows/ci.yml` | Reproduce builds and integration checks in GitHub |
| `.devcontainer/` | Optional shared cloud development environment |

The profile module currently lives in the frontend because its data stays in the browser. The catalog module supplies fictional data and browser-local saved IDs. The matching domain module is independent of the HTTP and frontend boundaries. Connectors and numerical ranking remain future work.

## API contract

`GET /api/status` returns HTTP 200 with JSON:

```json
{"name":"JobSinton","status":"UP"}
```

The endpoint confirms that this process can handle an HTTP request. It does not verify databases, providers, authentication, or overall application readiness. It exposes no host information, framework versions, or secrets. Unknown routes return 404.

The client validates both required fields before declaring success. A non-success response, malformed JSON, unexpected contract, network failure, or five-second timeout produces the same recoverable unavailable state. Error details remain outside the product interface.

## Async behavior

Each request owns an `AbortController`. Cleanup cancels work when the component unmounts or a new attempt replaces it. An active-request guard prevents stale responses from changing the current state. The retry button is disabled while checking; there is no automatic polling or retry traffic.

## Why this stack and scope

Java 21 provides a stable JDK baseline. Spring Boot supplies HTTP routing and JSON serialization. React and strict TypeScript own the interface. Maven manages the backend; an npm lockfile makes frontend installs reproducible.

Vite handles development and the static production build. The initial application does not need an additional React bundler plugin. Playwright tests the actual browser-to-Java connection and failure recovery on two viewport configurations.

A database and provider credentials would introduce setup and data concerns without helping prove this first connection. Those decisions remain in the project plan. GitHub Codespaces is an optional development environment, not application hosting.

## Local profile

The profile editor keeps its draft separate from the last successful save. Saving validates and normalizes the draft, then writes a versioned record to browser storage. Only a successful write updates the saved summary. Invalid input focuses the first affected control; failed storage operations retain the draft and show an error.

Saved records are untrusted input: loading checks their version, shape, size, and values before rendering. There are no storage writes on mount or while typing. Invalid records are preserved until an explicit successful save or clear. The same pure validation functions protect submission and loading.

The profile never passes through the status API and remains usable during an API outage. Storage is scoped to the browser and origin, with no account or device synchronization. Multiple tabs use last-save-wins behavior. These frontend checks are not a substitute for server validation when a profile API is introduced.

See [the profile contract and verification guide](PROFILE.md).

## Documentation policy

API contracts, design reasons, boundary validation, and non-obvious behavior are documented next to the code. Comments explain intent rather than restate syntax. This document tracks changes in responsibilities, `CHANGELOG.md` records observable increments and verification, and the project plan covers decisions and next steps.

## Current limitations

Persistence is limited to browser-local profile and shortlist storage. There are no user accounts, server-side profile storage, matching API/interface integration, provider access, production deployment, or published release. Current is the owner-selected visual baseline; see [DESIGN.md](DESIGN.md) for the selected system and acceptance checklist. Desktop/mobile browser automation covers the first journey but does not replace a complete accessibility audit or human visual review.

## Current application composition

The approved design is implemented in shared React components and CSS within the existing Vite build. The preversion branch is retained as a visual reference, not merged as a second application. Hash routes avoid adding deployment rewrite requirements and support direct links, reloads and history. Unknown routes render a recovery view.

The profile editor stays mounted while hidden on other routes, preserving an unsaved draft during navigation. Successful saves notify the shell; the home direction and detail facts use only that saved profile. The profile storage key and v1 envelope remain unchanged. No profile is seeded or reset during startup.

The shortlist uses its own versioned localStorage record. Reads are bounded and validated; unknown catalog IDs are ignored without rewriting storage. A failed write or clear leaves the displayed saved state unchanged and reports the error. Storage events refresh open tabs. There is no cross-device synchronization or user account.

The catalog is bundled fictional content, not an API response. Opportunity facts compare explicit values without scoring or eligibility inference. Application actions explain that no submission occurs. Backend contracts, provider credentials and database setup remain unchanged.

## Matching domain

The `matching` package implements the accepted [rule contract](MATCHING.md) using Java records and enums. `MatchingEngine.evaluate` returns either a complete versioned evaluation or structured validation errors. It has no Spring annotations, external I/O, clock, database or provider dependency.

Input snapshots preserve unknown facts and explicit limits. Assessments retain compared evidence, stable reasons and scope limitations. Only constraint outcomes influence the constraint summary; advisory differences never remove offers. Technology terms and country evidence are ordered deterministically, and result collections are immutable.

No matching controller or frontend caller is introduced in this increment. The existing status endpoint and browser-local profile remain unchanged. The [engine guide](MATCHING_ENGINE.md) records Java types, the future fictional-catalog mapping and verification commands. The API adapter and interface integration follow after domain review; ranking follows separately.
