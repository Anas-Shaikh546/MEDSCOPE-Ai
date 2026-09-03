# MedScope AI

### Intelligent Medical Report Analysis & Longitudinal Health Intelligence

MedScope AI is an AI-assisted medical report analysis platform that converts laboratory reports into structured health data and transforms that data into understandable, traceable, and longitudinal insights.

The platform combines **deterministic document processing**, **OCR**, **structured laboratory-result extraction**, **AI-assisted interpretation**, **historical trend analysis**, and **cross-report intelligence** into a single system.

Instead of treating every laboratory report as an isolated document, MedScope AI builds a structured history of a user's laboratory results and uses that history to identify meaningful changes, persistent abnormalities, and recurring patterns.

> **Medical Disclaimer:** MedScope AI is an informational software system and is **not a diagnostic tool or substitute for professional medical advice**. AI-generated interpretations may be incomplete or incorrect. Laboratory results should always be reviewed with a qualified healthcare professional.

---

## Table of Contents

* [Project Overview](#project-overview)
* [Problem Statement](#problem-statement)
* [Objectives](#objectives)
* [Key Features](#key-features)
* [System Architecture](#system-architecture)
* [End-to-End Processing Pipeline](#end-to-end-processing-pipeline)
* [Core Modules](#core-modules)
* [OCR Pipeline](#ocr-pipeline)
* [AI Interpretation](#ai-interpretation)
* [Longitudinal Trend Analysis](#longitudinal-trend-analysis)
* [Cross-Report Intelligence](#cross-report-intelligence)
* [Explainability & Traceability](#explainability--traceability)
* [Security Architecture](#security-architecture)
* [Technology Stack](#technology-stack)
* [Project Structure](#project-structure)
* [Database Architecture](#database-architecture)
* [API Reference](#api-reference)
* [Environment Configuration](#environment-configuration)
* [Local Development Setup](#local-development-setup)
* [Testing](#testing)
* [Development Stages](#development-stages)
* [Known Limitations](#known-limitations)
* [Future Improvements](#future-improvements)
* [Design Philosophy](#design-philosophy)
* [Project Status](#project-status)
* [Medical Safety Disclaimer](#medical-safety-disclaimer)

---

# Project Overview

Laboratory reports contain clinically important information, but the information is commonly presented as dense tables containing test names, numerical values, units, reference ranges, and abnormal flags.

This creates several practical problems:

* Users may not understand individual laboratory measurements.
* Comparing reports from different dates is difficult.
* Persistent abnormalities can be missed when reports are viewed independently.
* Changes in laboratory values are difficult to identify manually.
* AI-generated explanations can become unreliable if the underlying data is not structured and validated.
* Scanned laboratory reports cannot be processed using ordinary PDF text extraction.

MedScope AI addresses these problems by creating a structured processing pipeline:

```text
Laboratory Report
       │
       ▼
 PDF Validation
       │
       ▼
Text Extraction / OCR
       │
       ▼
Structured Laboratory Results
       │
       ├───────────────┐
       ▼               ▼
AI Interpretation   Historical Data
                       │
                       ▼
                 Trend Analysis
                       │
                       ▼
             Cross-Report Intelligence
                       │
                       ▼
             Explainable Insights
                       │
                       ▼
                Source References
```

---

# Problem Statement

Most digital laboratory-report systems focus primarily on storing or displaying reports.

MedScope AI focuses on the next layer:

> **Turning laboratory documents into structured, explainable, longitudinal health information.**

The system therefore does not rely on an LLM alone.

Instead, it separates the responsibilities of deterministic software and artificial intelligence.

### Deterministic layer

Responsible for:

* PDF validation
* Text extraction
* OCR
* Laboratory-result parsing
* Data validation
* Unit consistency
* Historical grouping
* Numerical trend calculation
* Insight prioritization
* Source mapping

### AI layer

Responsible for:

* Natural-language explanation
* Contextual interpretation
* Summarization
* Cross-report explanation
* Human-readable health insights

This architecture reduces the possibility of an AI model becoming the authoritative source for laboratory measurements.

---

# Objectives

MedScope AI is designed around five primary objectives:

### 1. Structured Extraction

Convert unstructured laboratory reports into structured laboratory observations.

### 2. Understandable Interpretation

Provide understandable explanations of extracted laboratory results.

### 3. Longitudinal Analysis

Allow laboratory values to be compared across multiple reports and dates.

### 4. Cross-Report Intelligence

Identify persistent abnormalities, significant changes, and meaningful historical patterns.

### 5. Traceability

Ensure generated insights can be traced back to the laboratory results that contributed to them.

---

# Key Features

## Authentication & User Management

* User registration
* Secure login
* JWT-based authentication
* BCrypt password hashing
* Authenticated user resolution
* User-specific resource ownership

## Medical Report Management

* PDF upload
* PDF validation
* Report metadata
* Test-date management
* Report listing
* Original report download
* Report deletion

## Laboratory Result Extraction

* PDF text extraction using Apache PDFBox
* Controlled laboratory-test vocabulary
* Structured result generation
* Numerical value extraction
* Unit extraction
* Reference-range extraction
* Abnormality information

## OCR

* Detection of scanned/image-based PDFs
* Page classification
* High-resolution page rendering
* OCR-ready preprocessing pipeline
* Pluggable OCR engine architecture
* Integration with the existing result parser

## AI Interpretation

* AI-assisted laboratory-result explanations
* Structured AI responses
* Pydantic validation
* Java-side validation
* Result-reference validation
* Persistent analysis storage

## Longitudinal Health Trends

* Historical laboratory observations
* Canonical test grouping
* Date-based ordering
* Increasing/decreasing/stable analysis
* Fluctuation detection
* Insufficient-data handling
* Unit-consistency validation

## Cross-Report Intelligence

* Current-vs-historical comparison
* Persistent abnormality detection
* Significant-change detection
* Trend context
* Deterministic prioritization
* AI-assisted explanation
* Source-linked insights
* Multiple insight generations

---

# System Architecture

```text
                         ┌──────────────────────────┐
                         │      Next.js Frontend     │
                         │                          │
                         │ TypeScript                │
                         │ Dashboard                │
                         │ Reports                  │
                         │ Health Trends            │
                         │ Insights                 │
                         │ Authentication           │
                         │                          │
                         │ localhost:3000           │
                         └────────────┬─────────────┘
                                      │
                                  REST / JWT
                                      │
                         ┌────────────▼─────────────┐
                         │     Spring Boot Backend   │
                         │                          │
                         │ Java 21                  │
                         │ Spring Boot 3.3          │
                         │                          │
                         │ Authentication           │
                         │ Reports                  │
                         │ Extraction               │
                         │ OCR                      │
                         │ Interpretation           │
                         │ Timeline                 │
                         │ Intelligence             │
                         │ Security                 │
                         │                          │
                         │ localhost:8080           │
                         └───────┬───────────┬──────┘
                                 │           │
                    JPA / JDBC   │           │ REST
                                 │           │
                  ┌──────────────▼──┐     ┌──▼─────────────────┐
                  │   PostgreSQL    │     │  FastAPI AI Service│
                  │                 │     │                    │
                  │ users           │     │ Python 3.11        │
                  │ reports         │     │ Pydantic           │
                  │ report_results  │     │ AI orchestration   │
                  │ analyses        │     │                    │
                  │ trends          │     │ localhost:8000     │
                  │ insights        │     └─────────┬──────────┘
                  │ insight_sources│               │
                  └─────────────────┘               │
                                                    │
                                          ┌─────────▼─────────┐
                                          │     OpenRouter    │
                                          │   Configurable AI │
                                          │       Model       │
                                          └───────────────────┘
```

---

# End-to-End Processing Pipeline

A complete report-processing workflow follows these stages:

```text
1. User uploads PDF
          │
          ▼
2. PDF signature validation
          │
          ▼
3. Text-layer detection
          │
       ┌──┴──┐
       │     │
   TEXT     SCANNED
       │     │
       │     ▼
       │    OCR
       │     │
       └──┬──┘
          ▼
4. Extracted laboratory text
          │
          ▼
5. Controlled result parser
          │
          ▼
6. Structured ReportResult records
          │
          ├───────────────┐
          ▼               ▼
7. AI Interpretation   Historical Results
                              │
                              ▼
                       8. Trend Engine
                              │
                              ▼
                       9. Intelligence
                              │
                              ▼
                     10. Prioritization
                              │
                              ▼
                       11. AI Explanation
                              │
                              ▼
                     12. Validated Insight
                              │
                              ▼
                     13. Source References
```

Each stage has a specific responsibility and can be independently tested.

---

# Core Modules

## `auth`

Responsible for:

* User registration
* Login
* Password hashing
* JWT generation

---

## `user`

Provides authenticated-user operations such as:

```text
GET /api/users/me
```

The authenticated identity is derived from the JWT.

---

## `report`

Responsible for:

* PDF upload
* Report metadata
* Test date
* Report retrieval
* Original file download
* Report deletion

---

## `analysis`

Responsible for deterministic document processing.

This includes:

* PDFBox extraction
* Laboratory pattern matching
* Canonical test-name mapping
* Structured `ReportResult` creation

This module represents the core extraction stage.

---

## `interpretation`

Responsible for single-report AI interpretation.

Structured laboratory results are sent to the AI service.

The AI response is validated before persistence.

---

## `timeline`

Responsible for historical laboratory analysis.

It provides:

* Historical result retrieval
* Canonical grouping
* Date ordering
* Trend calculation
* Unit validation

---

## `intelligence`

Responsible for cross-report health intelligence.

Major components include:

```text
ContextBuilderService
PrioritizationEngine
InsightGeneration
Insight
InsightSource
```

---

## `ocr`

Responsible for scanned-document processing.

The OCR module is designed as an abstraction so that different OCR engines can be introduced without changing the rest of the application.

---

## `security`

Responsible for:

* JWT authentication
* Security filters
* Authenticated-user resolution
* Protected endpoint access

---

## `common`

Contains shared infrastructure such as:

* DTOs
* Exceptions
* Global exception handling
* Common utilities

---

# OCR Pipeline

Modern laboratory reports may be generated digitally, while older reports or photographed documents may contain only images.

A normal PDF text extractor cannot reliably process such documents.

MedScope AI therefore introduces an OCR processing path.

```text
                 Input PDF
                     │
                     ▼
              PDF Inspection
                     │
                     ▼
          ┌─────────────────────┐
          │ Text-layer analysis │
          └──────────┬──────────┘
                     │
            ┌────────┴────────┐
            │                 │
       Sufficient          Insufficient
          text                text
            │                 │
            ▼                 ▼
     PDFBox extraction     Page rendering
                              │
                              ▼
                       Image preprocessing
                              │
                              ▼
                         OCR engine
                              │
                              ▼
                         OCR text
                              │
                 ┌────────────┘
                 ▼
          Existing parser
                 │
                 ▼
          ReportResult
```

### Page classification

Pages can be classified as:

```text
TEXT
SCANNED
MIXED
```

This allows the system to avoid unnecessary OCR processing when a valid text layer already exists.

### OCR accuracy strategy

For production-quality OCR, the pipeline should validate:

* Test names
* Numeric values
* Decimal positions
* Units
* Reference ranges
* Abnormal flags
* Page numbers
* OCR confidence

For example, an OCR result such as:

```text
Hemoglobin 13.8 g/dL
```

must not accidentally become:

```text
Hemoglobin 138 g/dL
```

because a decimal-point error can materially change the meaning of the extracted result.

The architecture therefore keeps OCR separate from the structured medical-data layer and allows validation before persistence.

---

# AI Interpretation

The AI service is implemented using:

```text
Python
FastAPI
Pydantic
OpenRouter
```

The Java backend communicates with the AI service through REST.

```text
Spring Boot
     │
     │ structured results
     ▼
FastAPI
     │
     ▼
Prompt construction
     │
     ▼
OpenRouter
     │
     ▼
AI response
     │
     ▼
Pydantic validation
     │
     ▼
Spring Boot validation
     │
     ▼
Analysis
```

### Validation principle

The AI model should not be trusted as the source of the original laboratory values.

If the database contains:

```text
Hemoglobin = 13.8 g/dL
```

the AI is expected to explain that structured value.

It should not be allowed to replace the stored value with an independently generated number.

AI findings are therefore validated against actual `ReportResult` records.

---

# Longitudinal Trend Analysis

A single laboratory value provides limited historical context.

MedScope AI therefore groups laboratory observations across reports.

Example:

```text
Report 1     Hemoglobin = 13.2
Report 2     Hemoglobin = 13.6
Report 3     Hemoglobin = 13.9
Report 4     Hemoglobin = 14.1
```

The trend engine can identify the direction of change without asking the AI model to perform the numerical calculation.

Supported classifications:

```text
INCREASING
DECREASING
STABLE
FLUCTUATING
INSUFFICIENT_DATA
UNSUPPORTED
```

### Trend calculation rules

The trend engine:

1. Groups results by canonical test name.
2. Orders observations chronologically.
3. Uses `testDate` when available.
4. Falls back to upload date when required.
5. Checks unit consistency.
6. Calculates deterministic trend information.
7. Rejects unsupported mixed-unit comparisons.

This makes trend calculations reproducible and independent of the selected AI model.

---

# Cross-Report Intelligence

The intelligence engine extends beyond simple trend visualization.

It combines:

```text
Current Report
      +
Historical Results
      +
Trend Information
      +
Abnormality Information
      +
Deterministic Prioritization
```

The resulting context is supplied to the AI service for explanation.

### Deterministic prioritization

The system first evaluates signals such as:

* Persistent abnormalities
* Significant changes
* Historical trends
* Repeated abnormal observations

The AI is then used to explain the prioritized context in human-readable language.

```text
Historical Data
       │
       ▼
ContextBuilderService
       │
       ▼
IntelligenceContext
       │
       ▼
PrioritizationEngine
       │
       ├── Persistent abnormality
       ├── Significant change
       └── Trend context
       │
       ▼
AI explanation
       │
       ▼
InsightGeneration
       │
       ▼
Insight
       │
       ▼
InsightSource
```

---

# Explainability & Traceability

A core design requirement is:

> **Every generated insight should be explainable through the underlying laboratory data.**

Insights are therefore associated with source records.

Example:

```text
Insight
 │
 ├── ReportResult #103
 │      └── Hemoglobin = 13.2
 │
 ├── ReportResult #127
 │      └── Hemoglobin = 13.8
 │
 └── ReportResult #151
        └── Hemoglobin = 14.1
```

This creates a traceability chain:

```text
Insight
   ↓
Source Result
   ↓
Original Report
   ↓
Original PDF
```

The objective is to make the system capable of answering:

> **"Which actual laboratory observations contributed to this insight?"**

This is an important distinction from systems that provide AI-generated text without a verifiable source chain.

---

# Security Architecture

Security is implemented at both the authentication and data-ownership levels.

## Password Security

Passwords are hashed using BCrypt.

Plaintext passwords are never stored.

---

## JWT Authentication

JWT tokens contain identity information such as:

```text
userId
email
```

Medical report data is not stored inside the JWT.

---

## Resource Ownership

Authenticated endpoints derive ownership from the authenticated identity.

Conceptually:

```sql
SELECT *
FROM reports
WHERE user_id = authenticatedUserId;
```

The application does not trust a client-supplied user ID for ownership decisions.

---

## File Storage

Uploaded files use UUID-based filenames.

Example:

```text
uploads/
└── {userId}/
    ├── 9f3d...pdf
    ├── 82aa...pdf
    └── 4c12...pdf
```

The original filename is retained only as metadata.

Filesystem paths are not exposed through API responses.

---

## PDF Validation

The upload layer verifies the PDF signature rather than relying only on:

```text
filename.pdf
```

This prevents a file from being treated as a valid PDF solely because its extension says `.pdf`.

---

# Technology Stack

| Category           | Technology                  |
| ------------------ | --------------------------- |
| Frontend           | Next.js 14                  |
| Frontend Language  | TypeScript                  |
| Backend            | Spring Boot 3.3             |
| Backend Language   | Java 21                     |
| Database           | PostgreSQL 16               |
| ORM                | Spring Data JPA / Hibernate |
| Database Migration | Flyway                      |
| Authentication     | Spring Security + JWT       |
| Password Hashing   | BCrypt                      |
| PDF Processing     | Apache PDFBox 3             |
| OCR                | Pluggable OCR architecture  |
| AI Service         | Python 3.11 + FastAPI       |
| Data Validation    | Pydantic                    |
| AI Gateway         | OpenRouter                  |
| Containerisation   | Docker                      |
| API Communication  | REST                        |

---

# Project Structure

```text
medscope-ai/
│
├── backend/
│   │
│   ├── src/main/java/com/medscope/
│   │   │
│   │   ├── auth/
│   │   ├── user/
│   │   ├── report/
│   │   ├── analysis/
│   │   ├── interpretation/
│   │   ├── timeline/
│   │   ├── intelligence/
│   │   ├── ocr/
│   │   ├── security/
│   │   └── common/
│   │
│   └── src/main/resources/
│       └── db/migration/
│
├── frontend/
│   │
│   ├── app/
│   │   ├── dashboard/
│   │   ├── health/
│   │   ├── login/
│   │   └── register/
│   │
│   ├── components/
│   ├── services/
│   └── types/
│
├── ai-service/
│   │
│   ├── app/
│   │   ├── routes/
│   │   ├── services/
│   │   └── models/
│   │
│   └── tests/
│
├── uploads/
├── docker-compose.yml
├── .env.example
└── README.md
```

---

# Database Architecture

MedScope AI uses PostgreSQL with Flyway migrations.

## Core entities

```text
users
   │
   └── reports
         │
         ├── report_results
         │
         └── analyses
                │
                └── analysis_findings


users
   │
   └── insight_generations
          │
          └── insights
                 │
                 └── insight_sources
                        │
                        └── report_results
```

## Flyway Migrations

| Version | Description                                        |
| ------- | -------------------------------------------------- |
| V1      | Users table                                        |
| V2      | Reports table                                      |
| V3      | Report results                                     |
| V4      | Cascade deletion for report results                |
| V5      | Analyses and analysis findings                     |
| V6      | Test definitions and report test dates             |
| V7      | Insight generations, insights, and insight sources |
| V8      | OCR metadata columns                               |

---

# API Reference

## Health

| Method | Endpoint      | Authentication | Description          |
| ------ | ------------- | -------------- | -------------------- |
| GET    | `/api/health` | No             | Backend health check |

## Authentication

| Method | Endpoint             | Authentication | Description           |
| ------ | -------------------- | -------------- | --------------------- |
| POST   | `/api/auth/register` | No             | Register user         |
| POST   | `/api/auth/login`    | No             | Login and receive JWT |

## User

| Method | Endpoint        | Authentication | Description                |
| ------ | --------------- | -------------- | -------------------------- |
| GET    | `/api/users/me` | Yes            | Current authenticated user |

## Reports

| Method | Endpoint                 | Authentication | Description                    |
| ------ | ------------------------ | -------------- | ------------------------------ |
| POST   | `/api/reports`           | Yes            | Upload PDF                     |
| GET    | `/api/reports`           | Yes            | List user's reports            |
| GET    | `/api/reports/{id}`      | Yes            | Get report metadata            |
| GET    | `/api/reports/{id}/file` | Yes            | Download original PDF          |
| PATCH  | `/api/reports/{id}`      | Yes            | Update report metadata         |
| DELETE | `/api/reports/{id}`      | Yes            | Delete report and related data |

## Extraction

| Method | Endpoint                    | Authentication | Description                |
| ------ | --------------------------- | -------------- | -------------------------- |
| POST   | `/api/reports/{id}/process` | Yes            | Process report             |
| GET    | `/api/reports/{id}/results` | Yes            | Retrieve extracted results |

## AI Interpretation

| Method | Endpoint                     | Authentication | Description             |
| ------ | ---------------------------- | -------------- | ----------------------- |
| POST   | `/api/reports/{id}/analysis` | Yes            | Generate interpretation |
| GET    | `/api/reports/{id}/analysis` | Yes            | Retrieve interpretation |
| GET    | `/api/analyses/{id}`         | Yes            | Retrieve analysis       |

## Trends

| Method | Endpoint                     | Authentication | Description                        |
| ------ | ---------------------------- | -------------- | ---------------------------------- |
| GET    | `/api/results/trends`        | Yes            | Retrieve all health trends         |
| GET    | `/api/results/trends/{name}` | Yes            | Retrieve trend for a specific test |

## Intelligence

| Method | Endpoint                              | Authentication | Description                        |
| ------ | ------------------------------------- | -------------- | ---------------------------------- |
| POST   | `/api/reports/{id}/insights/generate` | Yes            | Generate cross-report intelligence |
| GET    | `/api/insights`                       | Yes            | Retrieve generated insights        |

All authenticated resources are scoped to the currently authenticated user.

---

# Environment Configuration

## Backend

The backend requires the following environment variables:

| Variable            | Example                                     | Required |
| ------------------- | ------------------------------------------- | -------: |
| `DB_URL`            | `jdbc:postgresql://localhost:5432/medscope` |      Yes |
| `DB_USERNAME`       | `medscope`                                  |      Yes |
| `DB_PASSWORD`       | `change_me`                                 |      Yes |
| `JWT_SECRET`        | Long random secret                          |      Yes |
| `JWT_EXPIRATION_MS` | `86400000`                                  |      Yes |
| `UPLOAD_DIR`        | `./uploads`                                 |       No |
| `AI_SERVICE_URL`    | `http://localhost:8000`                     |       No |

## AI Service

| Variable                   | Example                     | Required |
| -------------------------- | --------------------------- | -------: |
| `OPENROUTER_API_KEY`       | `sk-or-v1-...`              |      Yes |
| `OPENROUTER_MODEL`         | Configured OpenRouter model |       No |
| `OPENROUTER_MODEL_VERSION` | Model identifier            |       No |
| `ANALYSIS_PROMPT_VERSION`  | `v1.0`                      |       No |
| `AI_SERVICE_PORT`          | `8000`                      |       No |

**Never commit `.env` files or API credentials to Git.**

---

# Local Development Setup

## Prerequisites

Install:

* Java 21 JDK
* Maven 3.9+
* Node.js 20+
* Python 3.11+
* Docker Desktop
* OpenRouter API key

---

## 1. Clone the repository

```bash
git clone <repo-url>
cd medscope-ai
```

Create environment files:

```bash
cp .env.example .env
cp ai-service/.env.example ai-service/.env
```

Configure the required values.

---

## 2. Start PostgreSQL

```bash
docker compose up -d
```

Verify:

```bash
docker ps
```

PostgreSQL should be running and healthy.

---

## 3. Start the Spring Boot backend

Open PowerShell in the `backend/` directory:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/medscope"
$env:DB_USERNAME="medscope"
$env:DB_PASSWORD="change_me"
$env:JWT_SECRET="a-long-random-string-at-least-32-characters"
$env:JWT_EXPIRATION_MS="86400000"
$env:UPLOAD_DIR="./uploads"
$env:AI_SERVICE_URL="http://localhost:8000"

mvn spring-boot:run
```

Flyway automatically executes the database migrations.

Verify:

```powershell
curl http://localhost:8080/api/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

## 4. Start the AI service

Open another PowerShell terminal:

```powershell
cd ai-service

python -m venv venv
venv\Scripts\activate

pip install -r requirements.txt

$env:OPENROUTER_API_KEY="your-key-here"

uvicorn app.main:app --reload --port 8000
```

Verify:

```powershell
curl http://localhost:8000/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

## 5. Start the frontend

Open another terminal:

```powershell
cd frontend
npm install
npm run dev
```

The frontend will be available at:

```text
http://localhost:3000
```

---

# Testing

## Backend

```powershell
cd backend
mvn clean test
```

The backend test suite can use an in-memory H2 database and therefore does not require a running PostgreSQL instance for standard tests.

## AI Service

```powershell
cd ai-service
python -m pytest tests/ -v
```

---

# Development Stages

MedScope AI was designed and implemented incrementally.

| Step   | Module         | Purpose                               | Status      |
| ------ | -------------- | ------------------------------------- | ----------- |
| Step 1 | Foundation     | Core backend/database structure       | Completed   |
| Step 2 | Authentication | Registration, login, JWT              | Completed   |
| Step 3 | Reports        | Upload and report management          | Completed   |
| Step 4 | Extraction     | PDF extraction and structured results | Frozen      |
| Step 5 | Interpretation | AI-assisted report interpretation     | Frozen      |
| Step 6 | Timeline       | Longitudinal trend analysis           | Frozen      |
| Step 7 | Intelligence   | Cross-report health intelligence      | Implemented |
| Step 8 | OCR            | Scanned-report processing             | In progress |

The architecture intentionally preserves earlier deterministic stages while allowing new capabilities to be added independently.

---

# Known Limitations

| Limitation            | Description                                                                |
| --------------------- | -------------------------------------------------------------------------- |
| OCR engine            | Production OCR engine still needs to be configured/integrated              |
| OCR confidence        | Confidence-aware validation requires further implementation                |
| Page attribution      | Some results may not yet contain page-level source information             |
| Test date             | Test dates may require manual assignment                                   |
| Unit conversion       | Mixed units may result in `UNSUPPORTED` trend classification               |
| Test vocabulary       | Extraction depends on the supported canonical test definitions             |
| AI latency            | Response time depends on the selected AI model/provider                    |
| Refresh tokens        | JWT refresh-token flow is not currently implemented                        |
| File storage          | Files are stored locally rather than in cloud object storage               |
| Production compliance | Deployment-specific medical/privacy compliance still needs to be addressed |

---

# Future Improvements

## OCR

* Integrate Tesseract or PaddleOCR
* Add OCR confidence scores
* Add page-level coordinates
* Detect table structures
* Improve preprocessing
* Implement automatic OCR quality checks
* Add OCR regression datasets

## Laboratory Data

* Expand test-definition vocabulary
* Normalize laboratory units
* Support unit conversion
* Improve reference-range parsing
* Automatically detect test dates
* Support more report formats

## Intelligence

* More advanced longitudinal pattern detection
* Better multi-test correlation
* Confidence-aware insights
* More sophisticated deterministic prioritization
* Insight severity calibration
* User-configurable insight explanations

## Infrastructure

* Cloud object storage
* Refresh-token authentication
* Rate limiting
* HTTPS/TLS
* Secrets management
* Audit logging
* Production observability
* Background processing for large reports

## AI Reliability

* Model evaluation datasets
* Prompt versioning
* Automated response regression testing
* Structured output enforcement
* Hallucination detection
* Model comparison
* Human review workflows

---

# Design Philosophy

MedScope AI follows one central architectural principle:

> **Deterministic software should establish the facts; AI should explain those facts.**

The architecture can therefore be summarized as:

```text
             DOCUMENT
                 │
                 ▼
        PDF / OCR Processing
                 │
                 ▼
        STRUCTURED FACTS
                 │
        ┌────────┴────────┐
        ▼                 ▼
   Trend Engine      AI Context
        │                 │
        ▼                 ▼
 Deterministic       AI Explanation
 Prioritization           │
        │                 │
        └────────┬────────┘
                 ▼
        VALIDATED INSIGHT
                 │
                 ▼
          SOURCE REFERENCES
```

This separation provides several advantages:

### Reliability

Numerical calculations are performed by deterministic code.

### Explainability

Insights can reference actual laboratory observations.

### Reproducibility

The same input data produces the same deterministic trend classifications.

### Flexibility

The AI model can be changed without redesigning the extraction and trend engines.

### Safety

AI output is treated as an interpretation layer rather than the authoritative source of medical measurements.

---

# Project Status

MedScope AI currently provides the core architecture for an intelligent medical-report analysis platform.

### Implemented

* User authentication
* JWT security
* Secure password hashing
* PDF report upload
* PDF validation
* Report management
* Structured laboratory-result extraction
* AI-assisted report interpretation
* PostgreSQL persistence
* Flyway migrations
* Longitudinal health trends
* Cross-report intelligence
* Deterministic insight prioritization
* Source-linked insights
* Next.js frontend
* FastAPI AI service
* OCR processing architecture

### Current Engineering Focus

The primary next stage is strengthening the OCR pipeline for scanned laboratory reports.

The focus areas are:

```text
OCR Accuracy
     +
Numerical Validation
     +
Confidence Scoring
     +
Page-Level Traceability
     +
Laboratory Pattern Validation
     =
Trustworthy Scanned-Report Processing
```

---

# Medical Safety Disclaimer

MedScope AI is an **informational and educational software project**.

It does not provide medical diagnosis, treatment recommendations, prescriptions, or emergency medical decisions.

AI-generated content may contain errors or omissions. Laboratory results should always be interpreted in the context of the individual's medical history and reviewed by a qualified healthcare professional.

If a user has concerning symptoms or believes they may have a medical emergency, they should seek appropriate professional medical care rather than relying on MedScope AI.

---

## License

Add the project's applicable license here.

---

## Author

**MedScope AI**

An intelligent laboratory-report analysis platform focused on structured extraction, explainable AI interpretation, longitudinal health analysis, and cross-report intelligence.
