# Foundation architecture

## Scope and request path

The first increment establishes one real request through the complete stack:

Browser → Vite development proxy → Spring Boot → JSON response → connection state.

The browser calls `/api/status` on its own origin. Vite forwards `/api` to `http://127.0.0.1:8080`, so local development and private Codespaces port forwarding use the same frontend code. The Vite host allowlist is derived from this Codespace's name and forwarding domain; it does not trust every host. No broad CORS policy is needed. Production routing is not configured.

## Responsibilities

| Location | Responsibility |
| --- | --- |
| `backend/.../JobSintonApplication.java` | Start Spring Boot |
| `backend/.../system/SystemStatusController.java` | Own the public service-status contract |
| `frontend/src/api/status.ts` | HTTP request and runtime validation of external JSON |
| `frontend/src/App.tsx` | Accessible introduction and connection state |
| `frontend/src/styles.css` | Responsive visual foundation without external assets |
| `frontend/tests/` | Browser journeys against the real API, plus controlled failures |
| `.github/workflows/ci.yml` | Reproduce builds and integration checks in GitHub |
| `.devcontainer/` | Optional shared cloud development environment |

Future profile, opportunity, connector, and matching modules will live in this repository. They are not generated as empty abstractions in the foundation.

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

## Documentation policy

Document API contracts, design reasons, boundary validation, and non-obvious behavior next to the code. Avoid comments that only restate syntax. Update this document when responsibilities change, `CHANGELOG.md` for observable increments and verification, and the project plan for decisions and next steps.

## Current limitations

No persistence, user accounts, matching, provider access, production deployment, or published release. The visual layout is a foundation subject to design review. Desktop/mobile browser automation covers the first journey but does not replace a complete accessibility audit or human visual review.
