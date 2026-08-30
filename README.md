# MedScope AI

Medical report analysis platform. Monorepo containing three services:

```
medscope-ai/
├── backend/       Spring Boot 3 (Java 21) — REST API, auth, business logic
├── frontend/      Next.js + TypeScript — web UI
├── ai-service/    Python + FastAPI — AI-powered report interpretation (OpenRouter)
├── uploads/       Local file storage for uploaded reports (gitignored)
└── docker-compose.yml   PostgreSQL only, for local dev
```

## Status

| Step | Description | Status |
|------|-------------|--------|
| 1 | Foundation — repo structure, health endpoints, error handling, DB wiring | ✅ done |
| 2 | User accounts & security — registration, login, JWT auth, `/users/me` | ✅ done |
| 3 | Report management — upload, list, view, download, delete | ✅ done |
| 4 | PDF extraction — text extraction, structured `report_results` | ✅ done |
| 5 | AI interpretation — OpenRouter-backed analysis, findings, versioning | ✅ done |

Steps 1–5 have all been verified end-to-end: full automated suite passing (`mvn clean test`) and a complete manual walkthrough (register → login → upload → process → analyze → view/delete, including cross-status test data covering NORMAL/HIGH/LOW/UNKNOWN results and one-sided threshold ranges).

Deliberately **not** used yet: Kafka, Redis, AWS/S3, Kubernetes, microservices, vector DB.

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Node.js 20+
- Python 3.11+
- Docker + Docker Compose
- An [OpenRouter](https://openrouter.ai) API key (for Step 5 AI analysis)

## Setup

### 1. Environment variables

```bash
cp .env.example .env
cp ai-service/.env.example ai-service/.env
```

Fill in real values — DB credentials, `JWT_SECRET`, and `OPENROUTER_API_KEY`. **`.env` files are never committed** — but note that neither Spring Boot nor this FastAPI service auto-loads a `.env` file at runtime; you also need to export these as real shell environment variables in the terminal you run each service from (see steps 3 and 5 below).

### 2. Start PostgreSQL

```bash
docker compose up -d
docker ps   # confirm medscope-postgres is Up
```

### 3. Backend (Spring Boot)

Set these in the terminal you'll run the backend from — they don't persist across terminal sessions:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/medscope"
$env:DB_USERNAME="medscope"
$env:DB_PASSWORD="change_me"
$env:JWT_SECRET="a-long-random-string-at-least-32-characters-long"
$env:JWT_EXPIRATION_MS="86400000"
$env:UPLOAD_DIR="./uploads"
$env:AI_SERVICE_URL="http://localhost:8000"

cd backend
mvn spring-boot:run
```

Flyway runs all migrations automatically on startup (`users`, `reports`, `report_results`, `analyses`, `analysis_findings`).

Verify:

```bash
curl http://localhost:8080/api/health
# {"status":"UP"}
```

### 4. Frontend (Next.js)

```bash
cd frontend
npm install
npm run dev
```

Visit http://localhost:3000

### 5. AI service (FastAPI)

```powershell
cd ai-service
python -m venv venv
venv\Scripts\activate          # macOS/Linux: source venv/bin/activate
pip install -r requirements.txt

$env:OPENROUTER_API_KEY="your-actual-key-here"
uvicorn app.main:app --reload --port 8000
```

Verify:

```bash
curl http://localhost:8000/health
# {"status":"UP"}
```

## API overview

| Method | Path | Auth required | Description |
|--------|------|----------------|--------------|
| GET    | `/api/health` | No | Backend liveness check |
| POST   | `/api/auth/register` | No | Create a new user account |
| POST   | `/api/auth/login` | No | Exchange credentials for a JWT |
| GET    | `/api/users/me` | Yes | Get the authenticated user's profile |
| POST   | `/api/reports` | Yes | Upload a PDF report (multipart) |
| GET    | `/api/reports` | Yes | List the caller's own reports |
| GET    | `/api/reports/{reportId}` | Yes | Get one report's metadata |
| GET    | `/api/reports/{reportId}/file` | Yes | Download/view the original PDF |
| DELETE | `/api/reports/{reportId}` | Yes | Delete a report (file + metadata + any results/analysis) |
| POST   | `/api/reports/{reportId}/process` | Yes | Extract structured test results from the PDF |
| GET    | `/api/reports/{reportId}/results` | Yes | Get the extracted results for a report |
| POST   | `/api/reports/{reportId}/analysis` | Yes | Create or replace the AI interpretation for a processed report |
| GET    | `/api/reports/{reportId}/analysis` | Yes | Get the caller's interpretation for a report |
| GET    | `/api/analyses/{analysisId}` | Yes | Get one of the caller's saved interpretations by id |

Every `Yes`-auth endpoint requires a `Authorization: Bearer <token>` header and enforces ownership from the JWT — never from a client-supplied id.

## How AI interpretation works (Step 5)

1. The caller requests analysis for one of their own reports.
2. The backend requires that report to be `PROCESSED` and to contain extracted `ReportResult` rows — the raw PDF is never sent to the AI service, only the already-extracted structured facts.
3. FastAPI builds a prompt from those facts (including each result's extraction confidence and reference range, or an explicit "not provided" when a range is missing) and calls the configured OpenRouter model.
4. The model's JSON response is validated by Pydantic, then re-validated on the Spring Boot side — every finding must point at a real index in the input results, so the AI can never reference a result that wasn't actually extracted.
5. The backend saves one `Analysis` per report plus its `AnalysisFinding` rows. Re-running analysis replaces the existing interpretation rather than creating duplicates (one analysis per report, enforced at the database level).

`ReportResult` (Step 4) remains the factual source of truth. An `Analysis` (Step 5) is only an interpretation of those saved facts — the AI can never modify an extracted value, only comment on it.

Every saved analysis also records `model_name`, `model_version`, and `prompt_version`, read from environment variables on the ai-service side:

```text
OPENROUTER_MODEL=nvidia/nemotron-3-ultra-550b-a55b:free
OPENROUTER_MODEL_VERSION=nvidia-nemotron-3-ultra-550b-a55b
ANALYSIS_PROMPT_VERSION=v1.0
```

Update the version values whenever you change the model or the prompt text, so every stored analysis stays traceable to the configuration that produced it.

## Engineering rules

1. **Don't code future features early.** Each step only builds what it needs.
2. **Database first.** Migration → entity → repository → service → DTO → controller → frontend.
3. **Controllers stay thin.** Validate → call service → return response. No business logic in controllers.
4. **Never expose JPA entities directly.** Always map to/from DTOs.
5. **Every schema change is a Flyway migration**, with `ON DELETE CASCADE` set correctly the first time — never hand-edit the running Postgres schema.
6. **Ownership is mandatory.** Any per-user resource is fetched via `authenticatedUserId` from the security context — never a client-supplied `userId`.
7. **Facts and interpretation stay in separate packages.** `com.medscope.analysis` (Step 4, extraction) and `com.medscope.interpretation` (Step 5, AI) are deliberately independent — the AI can read extracted facts but never rewrite them.

## Tech stack

| Layer            | Choice                         |
|-------------------|--------------------------------|
| Backend           | Java 21 + Spring Boot 3.x      |
| Database          | PostgreSQL                     |
| ORM               | Spring Data JPA                |
| Migrations        | Flyway                         |
| Security          | Spring Security + JWT          |
| Password hashing  | BCrypt                         |
| Frontend          | Next.js + TypeScript           |
| PDF text extraction | Apache PDFBox                |
| AI service        | Python + FastAPI + Pydantic    |
| AI provider       | OpenRouter                     |
| File storage      | Local filesystem (`uploads/`)  |
| Local DB runtime  | Docker                         |
| API format        | REST + JSON                    |

