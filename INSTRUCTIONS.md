# JobSinton — Quickstart & Operating Instructions

JobSinton runs completely self-contained with **zero API keys**, **zero `.env` setup**, and **EUR 0 cost**. You can boot the entire stack (Java 21 Spring Boot backend + React/Vite frontend) on any laptop (Windows, macOS, or Linux) with a single Docker command.

---

## 1. One-Command Docker Launch (Recommended)

### Requirements
- **[Git](https://git-scm.com/)**
- **[Docker](https://docs.docker.com/get-docker/)** with Docker Compose (`docker compose`):
  - **Windows / macOS:** Install [Docker Desktop](https://www.docker.com/products/docker-desktop/).
  - **Linux:** Install `docker-ce` and `docker-compose-plugin`.
- *(No Java, Maven, or Node.js installation is required on your machine when using Docker—everything is built inside multi-stage containers.)*

### Step 1 — Clone & Start JobSinton
Open a terminal and run:

```bash
git clone https://github.com/Garatech/jobsinton.git
cd jobsinton
docker compose up --build -d
```

- The first build takes ~1–2 minutes while Docker compiles the Java 21 backend JAR and the Node 24 / React production bundle.
- Subsequent starts (`docker compose up -d`) launch in a few seconds.

### Step 2 — Open JobSinton in Your Browser
Once the containers are healthy, open:

- **Application UI:** [http://localhost:5173](http://localhost:5173)
- **Backend Health Check (optional):** [http://localhost:5173/api/status](http://localhost:5173/api/status) (`{"name":"JobSinton","status":"UP"}`)

> **Custom Port (Optional):** If port `5173` is already in use on your laptop, start Compose with a custom port:
> ```bash
> JOBSINTON_PORT=3000 docker compose up -d
> ```
> Then open `http://localhost:3000`.

---

## 2. Everyday Docker Commands

Run these commands from the repository root (`jobsinton/`):

| Action | Command |
| --- | --- |
| **Start in the background** | `docker compose up -d` |
| **Pull latest updates from GitHub & rebuild** | `git pull && docker compose up --build -d` |
| **Check container status & health** | `docker compose ps` |
| **Follow live logs** | `docker compose logs -f` |
| **Stop containers (preserves all data)** | `docker compose down` |
| **Full reset (removes containers + Docker data volume)** | `docker compose down -v` |

---

## 3. Where Your Data Is Stored (Persistence)

JobSinton keeps your job search progress persistent across restarts without requiring an external database:

1. **Backend Docker Volume (`jobsinton-data` mounted at `/app/data`):**
   - `data/ats-feed-cache.json`: Stores a local snapshot of the full aggregated ATS feed so cold starts and container restarts render all cached opportunities immediately while refreshing live boards in the background.
   - `data/custom-companies.json`: Stores every company board you add via **`➕ Discover / Add Companies`** (Career Page URL Auto-Sniffer or 9-ATS Batch Company Name Probe) alongside the 334 pre-seeded Spain & Europe company boards.
   - `data/ats-seen-jobs.json`: Stores the earliest timestamp each job ID was seen (`firstSeenAt`) so recruiter edits to older listings never fake a `< 24h` (`🔥`) or `< 48h` (`⚡`) freshness badge.
   - This volume survives `docker compose down` and `docker compose up --build -d`.

2. **Browser `localStorage` (scoped to `http://localhost:5173`):**
   - `jobsinton.professional-profile`: Your saved **Professional Profile** (target role across 14 disciplines, technologies, experience, residence, and work mode).
   - `jobsinton.ats-triage-v1`: Your **1-Click Job Triage** state (`⭐ Save`, `✅ Applied`, `✕ Dismiss`), synced with the main navigation bar **`Saved`** counter and `#saved` page.
   - `jobsinton.ats-custom-stack`: Your custom technology keyword tags under **`Custom Profile`**.

---

## 4. Running Without Docker (Local Native Mode)

If you prefer to run the backend and frontend directly on your host OS without Docker:

### Requirements
- **Java 21** (`java --version` → 21)
- **Maven 3.9+** (`mvn --version`)
- **Node.js 24 LTS** & **npm** (`node --version` → v24.x)

### Start Backend & Frontend
1. **Terminal 1 (Backend on `http://127.0.0.1:8080`):**
   ```bash
   mvn -f backend/pom.xml spring-boot:run
   ```
2. **Terminal 2 (Frontend on `http://localhost:5173`):**
   ```bash
   npm --prefix frontend ci
   npm --prefix frontend run dev
   ```
3. Open **[http://localhost:5173](http://localhost:5173)** in your browser.
