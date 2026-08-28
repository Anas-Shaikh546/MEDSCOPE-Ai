# MedScope AI

Medical report analysis platform. Monorepo containing three services:

```
medscope-ai/
├── backend/       Spring Boot 3 (Java 21) — REST API, auth, business logic
├── frontend/      Next.js + TypeScript — web UI
├── ai-service/    Python + FastAPI — AI/report analysis (not yet implemented)
├── uploads/        Local file storage for uploaded reports (gitignored)
└── docker-compose.yml   PostgreSQL only, for local dev
```

## Status

**Step 1 — Foundation:** done (repo structure, health endpoints, error handling, DB wiring)
**Step 2 — User accounts & security:** done (registration, login, JWT auth, protected `/users/me`)
**Step 3+ — Report upload & AI analysis:** not started (deliberately — see engineering rules below)

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+ (or use `./mvnw` if you add the wrapper)
- Node.js 20+
- Python 3.11+
- Docker + Docker Compose

## Setup

### 1. Environment variables

```bash
cp .env.example .env
```

Fill in real values for `JWT_SECRET`, DB credentials, etc. `.env` is never committed.

### 2. Start PostgreSQL

```bash
docker compose up -d
```

### 3. Backend (Spring Boot)

```bash
cd backend
# export the vars from .env into your shell, or configure them in your IDE run config
mvn spring-boot:run
```

Flyway will run migrations automatically on startup (creates the `users` table).

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

```bash
cd ai-service
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Verify:

```bash
curl http://localhost:8000/health
# {"status":"UP"}
```

## API overview (Step 1–2)

| Method | Path                | Auth required | Description                  |
|--------|---------------------|----------------|-------------------------------|
| GET    | `/api/health`        | No             | Backend liveness check        |
| POST   | `/api/auth/register`| No             | Create a new user account     |
| POST   | `/api/auth/login`   | No             | Exchange credentials for a JWT|
| GET    | `/api/users/me`     | Yes (Bearer)   | Get the authenticated user's profile |

## Engineering rules (apply to every future step)

1. **Don't code future features early.** Step N only builds what Step N needs.
2. **Database first.** Migration → entity → repository → service → DTO → controller → frontend.
3. **Controllers stay thin.** Validate → call service → return response. No business logic in controllers.
4. **Never expose JPA entities directly.** Always map to/from DTOs.
5. **Every schema change is a Flyway migration.** Never hand-edit the running Postgres schema.
6. **Ownership is mandatory.** Any per-user resource is fetched via `authenticatedUserId` from the security context — never a client-supplied `userId`.
7. **No medical/AI logic until the user & data layer is solid.** That starts at Step 3.

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
| AI service        | Python + FastAPI               |
| File storage      | Local filesystem (`uploads/`)  |
| Local DB runtime  | Docker                         |
| API format        | REST + JSON                    |

Deliberately **not** used yet: Kafka, Redis, AWS/S3, Kubernetes, microservices, vector DB.
