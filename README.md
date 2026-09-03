The `License` section has been completely removed. Here is the updated `README.md`:

```markdown
# MedScope AI
> **Intelligent Medical Report Analysis & Longitudinal Health Intelligence**

MedScope AI is an enterprise-grade medical report analysis platform that converts raw laboratory documents into structured, traceable, and longitudinal health insights. By combining deterministic document processing with AI-driven contextual interpretation, MedScope AI tracks persistent abnormalities, calculates numerical trends, and delivers explainable health intelligence over time.

> ⚠️ **Medical Disclaimer:** MedScope AI is an informational software system and is not a diagnostic tool or substitute for professional medical advice. AI-generated interpretations may be incomplete or incorrect. Laboratory results must always be reviewed with a qualified healthcare professional.

---

## Table of Contents
* [Project Overview](#project-overview)
* [Problem Statement](#problem-statement)
* [System Architecture](#system-architecture)
* [End-to-End Processing Pipeline](#end-to-end-processing-pipeline)
* [Core Architectural Modules](#core-architectural-modules)
* [OCR Pipeline](#ocr-pipeline)
* [AI Interpretation & Longitudinal Trends](#ai-interpretation--longitudinal-trends)
* [Explainability & Traceability](#explainability--traceability)
* [Security & Compliance Architecture](#security--compliance-architecture)
* [Technology Stack](#technology-stack)
* [Project Structure](#project-structure)
* [Database Architecture](#database-architecture)
* [API Reference](#api-reference)
* [Environment Configuration](#environment-configuration)
* [Local Development Setup](#local-development-setup)
* [Project Status](#project-status)
* [Known Limitations & Future Roadmap](#known-limitations--future-roadmap)

---

## Project Overview

Laboratory reports contain critical diagnostic metrics, but they are typically delivered as static PDFs with dense tabular data, complex reference ranges, and isolated timeframes. 

MedScope AI solves this by converting unstructured laboratory documents into a structured, longitudinal health history. Instead of evaluating each report in isolation, the platform analyzes time-series laboratory metrics to highlight meaningful changes, recurring patterns, and persistent risks.


```

Laboratory Report (PDF / Image)
│
▼
PDF Validation & Layout Analysis
│
├───────────────────────────────┐
▼                               ▼
Native Text Extraction            OCR Pipeline
(Apache PDFBox)               (Image Preprocessing)
│                               │
└───────────────┬───────────────┘
▼
Structured Laboratory Results
│
┌───────────────┴───────────────┐
▼                               ▼
AI Interpretation               Historical Store
│
▼
Long-Term Trends
│
▼
Cross-Report Intelligence
│
▼
Source-Linked Insights

```

---

## Problem Statement

Most digital laboratory software relies on passive document storage or pure LLM-based text parsing. LLM-only approaches introduce hallucination risks when reading critical numerical values, units, and decimal places.

MedScope AI addresses this challenge by strictly separating deterministic software responsibilities from generative AI capabilities:

| Functional Responsibility | Deterministic Engine | Generative AI Layer |
| :--- | :---: | :---: |
| PDF Signature Validation & Layout Detection | **✓** | |
| Text Extraction & OCR Parsing | **✓** | |
| Laboratory Result Extraction & Unit Matching | **✓** | |
| Chronological Grouping & Trend Math | **✓** | |
| Insight Prioritization & Source Mapping | **✓** | |
| Natural Language Explanation | | **✓** |
| Contextual Health Summarization | | **✓** |
| Cross-Report Narrative Generation | | **✓** |

---

## System Architecture


```

```
                   ┌──────────────────────────┐
                   │     Next.js Frontend     │
                   │    (TypeScript / App)    │
                   │  localhost:3000          │
                   └────────────┬─────────────┘
                                │ REST / JWT
                                ▼
                   ┌──────────────────────────┐
                   │   Spring Boot Backend    │
                   │    (Java 21 / Spring 3.3)│
                   │  localhost:8080          │
                   └───────┬───────────┬──────┘
                           │           │
              JPA / JDBC   │           │ REST
                           │           │
            ┌──────────────▼──┐     ┌──▼─────────────────┐
            │   PostgreSQL    │     │  FastAPI AI Engine │
            │   (Schema v8)   │     │  (Python 3.11)     │
            │  localhost:5432 │     │  localhost:8000    │
            └─────────────────┘     └─────────┬──────────┘
                                              │ REST
                                              ▼
                                    ┌───────────────────┐
                                    │  OpenRouter Gateway│
                                    │  (LLM Execution)  │
                                    └───────────────────┘

```

```

---

## End-to-End Processing Pipeline

1. **Document Ingestion:** PDF signature verification ensures file integrity.
2. **Text-Layer Classification:** The document is classified as `TEXT`, `SCANNED`, or `MIXED`.
3. **Extraction Route:** Standard PDFs pass through Apache PDFBox; scanned files route directly to the OCR pipeline.
4. **Structured Parsing:** Laboratory patterns are parsed into standardized `ReportResult` entities using a controlled terminology vocabulary.
5. **Deterministic Trend Analysis:** Historical data points are grouped chronologically to evaluate trend metrics (`INCREASING`, `DECREASING`, `STABLE`, `FLUCTUATING`).
6. **Prioritization Engine:** The system evaluates persistent abnormalities and significant variance against historical baselines.
7. **AI Contextualization:** Summaries and natural-language explanations are generated via the AI engine using structured data context.
8. **Traceability Verification:** Generated insights are verified and linked to underlying database records.

---

## Core Architectural Modules

* **`auth` & `user`:** Manages user authentication, BCrypt password hashing, JWT generation, and identity resolution.
* **`report`:** Handles PDF uploads, file storage validation, metadata management, and document lifecycle events.
* **`analysis`:** Deterministic extraction engine using PDFBox and regularized expression mapping for laboratory terms.
* **`ocr`:** End-to-end optical character recognition engine with preprocessing, layout analysis, confidence scoring, and validation.
* **`timeline`:** Performs time-series aggregation, canonical test grouping, and numerical direction calculations.
* **`intelligence`:** Runs cross-report signal detection, prioritization scoring, and insight source mapping.
* **`interpretation`:** Interfaces with the FastAPI service for AI-assisted human-readable explanations.

---

## OCR Pipeline

The OCR module processes legacy, scanned, or image-based medical reports while preserving data integrity:


```

```
      Input Document
            │
            ▼
  Page Text Inspection
            │
 ┌──────────┴──────────┐
 ▼                     ▼

```

Sufficient Text       Scanned / Low Text
│                     │
▼                     ▼
PDFBox Engine      Page Image Rendering (300+ DPI)
│
▼
Contrast & Binarization
│
▼
OCR Text Extraction
│
▼
Decimal & Value Guardrails
│
▼
Structured Result Parser

```

### OCR Validation Principles
To prevent critical reading errors (such as misreading `13.8 g/dL` as `138 g/dL`), the OCR pipeline incorporates numerical decimal guardrails, unit boundary checks, reference range validation, and bounding-box level traceability.

---

## AI Interpretation & Longitudinal Trends

### Supported Trend Classifications
* **`INCREASING`:** Sequential rise across multiple historical reports.
* **`DECREASING`:** Sequential drop across multiple historical reports.
* **`STABLE`:** Value remains within a tight percentage margin of historical baselines.
* **`FLUCTUATING`:** Directional variance detected without a clear linear slope.
* **`INSUFFICIENT_DATA`:** Requires additional historical submissions to calculate trends.
* **`UNSUPPORTED`:** Incompatible units detected across historical points (e.g., `mg/dL` vs `mmol/L`).

### Validation Guardrails
The AI interpretation layer cannot override stored numeric values. All AI outputs are mapped against original `ReportResult` rows in PostgreSQL using Pydantic schema validation.

---

## Explainability & Traceability

Every generated insight maintains direct data provenance back to the source PDF:

$$\text{Generated Insight} \longrightarrow \text{ReportResult ID} \longrightarrow \text{Report Metadata} \longrightarrow \text{Original PDF File}$$

This verifiable chain ensures that every recommendation or trend summary can be audited against explicit, extracted laboratory measurements.

---

## Security & Compliance Architecture

* **Authentication:** Stateless JWT execution; sensitive patient identity data is kept out of token payloads.
* **Resource Ownership:** Authorization queries enforce tenant-level resource checks:
  ```sql
  SELECT * FROM reports WHERE id = :reportId AND user_id = :authenticatedUserId;

```

* **Storage Isolation:** Internal storage uses random UUID identifiers (`uploads/{userId}/{uuid}.pdf`), masking original file paths.
* **MIME Validation:** Signature checking confirms valid PDF headers rather than relying on file extensions.

---

## Technology Stack

| Domain | Technology | Description |
| --- | --- | --- |
| **Frontend** | Next.js 14, TypeScript | Responsive Dashboard UI & Health Trends |
| **Backend Framework** | Spring Boot 3.3, Java 21 | Core Business Logic, JPA, Security |
| **AI Ingestion Service** | FastAPI, Python 3.11, Pydantic | AI Service Orchestration & Prompt Guardrails |
| **Database** | PostgreSQL 16, Flyway | Persistent Schema Management & Migrations |
| **Document Processing** | Apache PDFBox 3, Tesseract OCR | Native PDF Extraction & OCR Processing |
| **AI Infrastructure** | OpenRouter Gateway | Enterprise LLM Access & Execution |

---

## Project Structure

```
medscope-ai/
├── backend/                  # Spring Boot Java Application
│   ├── src/main/java/com/medscope/
│   │   ├── auth/             # Authentication & JWT
│   │   ├── analysis/         # Deterministic Extraction
│   │   ├── ocr/              # Complete OCR Engine
│   │   ├── timeline/         # Longitudinal Trend Engine
│   │   ├── intelligence/     # Cross-Report Reasoning
│   │   └── security/         # Security & Access Control
│   └── src/main/resources/
│       └── db/migration/     # Flyway Migration Scripts (V1-V8)
├── frontend/                 # Next.js 14 Web Interface
├── ai-service/               # FastAPI Python AI Orchestrator
├── docker-compose.yml        # Multi-Container Development Stack
└── .env.example              # Environment Configuration Template

```

---

## Database Architecture

### Migration Schema Overview

* **`V1__Users.sql`:** Base user identity tables.
* **`V2__Reports.sql`:** Report document metadata and storage references.
* **`V3__Report_Results.sql`:** Extracted laboratory metrics.
* **`V4__Cascade_Deletion.sql`:** Referential integrity rules.
* **`V5__Analyses_Findings.sql`:** Single-report interpretation findings.
* **`V6__Test_Definitions.sql`:** Test vocabulary and reference metadata.
* **`V7__Insights_Sources.sql`:** Cross-report intelligence mapping.
* **`V8__OCR_Metadata.sql`:** OCR execution logs, confidence metrics, and coordinate mappings.

---

## API Reference

### System & Authentication

| Method | Endpoint | Auth Required | Description |
| --- | --- | --- | --- |
| `GET` | `/api/health` | No | System health check |
| `POST` | `/api/auth/register` | No | Register new user |
| `POST` | `/api/auth/login` | No | Authenticate user & return JWT |
| `GET` | `/api/users/me` | **Yes** | Retrieve active user profile |

### Reports & Extraction

| Method | Endpoint | Auth Required | Description |
| --- | --- | --- | --- |
| `POST` | `/api/reports` | **Yes** | Upload laboratory report PDF |
| `GET` | `/api/reports` | **Yes** | List authenticated user reports |
| `GET` | `/api/reports/{id}` | **Yes** | Get report metadata |
| `POST` | `/api/reports/{id}/process` | **Yes** | Trigger document extraction/OCR |
| `GET` | `/api/reports/{id}/results` | **Yes** | Retrieve parsed results |

### Trends & Intelligence

| Method | Endpoint | Auth Required | Description |
| --- | --- | --- | --- |
| `POST` | `/api/reports/{id}/analysis` | **Yes** | Generate report interpretation |
| `GET` | `/api/results/trends` | **Yes** | Retrieve all longitudinal health trends |
| `GET` | `/api/results/trends/{name}` | **Yes** | Fetch specific test trend |
| `POST` | `/api/reports/{id}/insights/generate` | **Yes** | Build cross-report intelligence |

---

## Environment Configuration

### Backend Setup (`.env`)

```bash
DB_URL=jdbc:postgresql://localhost:5432/medscope
DB_USERNAME=medscope
DB_PASSWORD=change_me
JWT_SECRET=your_32_character_minimum_secure_jwt_secret_key
JWT_EXPIRATION_MS=86400000
UPLOAD_DIR=./uploads
AI_SERVICE_URL=http://localhost:8000

```

### AI Service Setup (`ai-service/.env`)

```bash
OPENROUTER_API_KEY=sk-or-v1-your-openrouter-key
OPENROUTER_MODEL=anthropic/claude-3.5-sonnet
AI_SERVICE_PORT=8000

```

---

## Local Development Setup

### 1. Repository Setup

```bash
git clone [https://github.com/your-org/medscope-ai.git](https://github.com/your-org/medscope-ai.git)
cd medscope-ai

cp .env.example .env
cp ai-service/.env.example ai-service/.env

```

### 2. Infrastructure Deployment

```bash
docker compose up -d

```

### 3. Backend Execution

```bash
cd backend
./mvnw spring-boot:run

```

### 4. AI Engine Execution

```bash
cd ai-service
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000

```

### 5. Frontend Execution

```bash
cd frontend
npm install
npm run dev

```

---

## Project Status

| Module | Architectural Status | Implementation Level |
| --- | --- | --- |
| **Authentication & Users** | Completed | JWT + BCrypt + User-Scoped Access |
| **Report Processing Engine** | Completed | Apache PDFBox + Metadata Extraction |
| **OCR Processing Pipeline** | **Completed** | Full OCR Engine with Quality Guardrails |
| **Single-Report AI Interpretation** | Completed | FastAPI + Pydantic + OpenRouter |
| **Longitudinal Trend Calculation** | Completed | Deterministic Chronological Math |
| **Cross-Report Intelligence Engine** | Completed | Context Prioritization & Source Tracking |
| **Frontend Web Interface** | Completed | Next.js 14 Dashboard & Trend Visualizations |

---

## Known Limitations & Future Roadmap

* **Refresh Tokens:** Short-lived access tokens are active; full refresh-token flow is scheduled for a future update.
* **Unit Conversion:** Mixed unit inputs (e.g., `mg/dL` vs `mmol/L`) yield `UNSUPPORTED` trend statuses until automatic unit conversion rules are introduced.
* **Cloud Object Storage:** Current uploads are maintained locally in a secure system directory; S3/GCS drivers will be integrated in future releases.
* **Vocabulary Expansion:** Ongoing expansion of canonical medical terminology to increase standard parsing rates across non-standard laboratory layouts.

```

```