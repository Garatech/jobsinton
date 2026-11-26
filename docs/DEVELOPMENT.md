# Development Guide

GitHub is the source of truth for shared code and history. All automated tests run 100% offline with deterministic fixtures and require **zero API keys, `.env` files, or external databases**.

---

## What Runs Out of the Box

- **Backend (`backend/` — Java 21 + Spring Boot 4):** Exposes `GET /api/status`, `/api/ats/*` (aggregating **334 pre-seeded Spain & Europe company boards across 9 public ATS platforms**, plus live URL/slug auto-discovery, first-seen freshness tracking, and Stale-While-Revalidate disk caching), and `/api/matching/*` (deterministic profile evaluation and compatibility batch ranking).
- **Frontend (`frontend/` — React 19 + strict TypeScript + Vite):** Opens onto **Live ATS Radar** (`#home`) and **Live ATS Boards** (`#opportunities`) with **8 developer stack presets**, **3-tier Spain/Europe geo filtering**, **Junior/Entry & `< 48h` freshness filters**, **1-click triage** (`⭐ Save`, `✅ Applied`, `✕ Dismiss`), **14-role local profile editor** (`#profile`), searchable sample catalog & explainable matching, and instant **Español / English** switching.

---

## Option A — Run with Docker Compose (Fastest)

From the repository root:

```bash
docker compose up --build -d
```

Open **[http://localhost:5173](http://localhost:5173)**. Persistent runtime state (`ats-feed-cache.json`, `custom-companies.json`, `ats-seen-jobs.json`) is stored in the named `jobsinton-data` Docker volume. Stop with `docker compose down`. See [../INSTRUCTIONS.md](../INSTRUCTIONS.md) for full container commands.

---

## Option B — Local Native Development (Hot Reload)

### Prerequisites

Install Git, a **Java 21 JDK**, **Maven 3.9+**, and **Node.js 24** (with `npm`):

```bash
git --version
java --version
javac --version
mvn --version
node --version
npm --version
```

Install locked frontend dependencies once:

```bash
npm --prefix frontend ci
```

### Start Both Services

Terminal 1 (Spring Boot API on `http://localhost:8080`):

```bash
mvn -f backend/pom.xml spring-boot:run
```

Terminal 2 (Vite dev server on `http://localhost:5173`, proxying `/api/*` to `127.0.0.1:8080`):

```bash
npm --prefix frontend run dev
```

Open **[http://localhost:5173](http://localhost:5173)**. You can verify backend connectivity directly at `http://localhost:8080/api/status` (`{"name":"JobSinton","status":"UP"}`) or via **Service status** in the application footer.

*(Optional)* A `.devcontainer/` configuration is also included for GitHub Codespaces with Java 21, Maven, and Node 24 preconfigured.

---

## Run the Full Verification Suite

Stop any running development servers (`Ctrl+C`) on ports `8080` and `5173` before running Playwright so the test harness boots isolated offline instances.

From the repository root, run each step in order:

```bash
# 1. Backend unit, ATS adapter & MockMvc integration tests (136 tests)
mvn -B -ntp -f backend/pom.xml verify

# 2. Frontend dependencies, strict TypeScript check & production build
npm --prefix frontend ci
npm --prefix frontend run build

# 3. Install Playwright Chromium (first run only; add --with-deps on Linux)
npx --prefix frontend playwright install chromium

# 4. Playwright desktop & mobile E2E + ATS scoring suite (148 active tests)
npm --prefix frontend run test:e2e
```

---

## Troubleshooting

- **Port `8080` or `5173` already in use:** Stop existing `spring-boot:run`, `vite`, or Docker (`docker compose down`) instances before starting native processes or running `npm --prefix frontend run test:e2e`.
- **Windows `EPERM` on `esbuild.exe` during `npm ci`:** Stop any running Vite dev server or VS Code terminal holding `frontend/node_modules` before running `npm --prefix frontend ci`.
- **Separate browser `localStorage` per origin:** `http://localhost:5173` and `http://127.0.0.1:5173` maintain separate browser storage scopes; use `http://localhost:5173` consistently to keep your saved profile and job triage state together.
