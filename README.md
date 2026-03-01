# QuestionnaireAI — Structured Questionnaire Answering Tool

> **Almabase GTM Engineering Internship Assignment**  
> Built with Java 21 + Spring Boot 3 | Full-stack | AI-powered

---

## What Was Built

**QuestionnaireAI** is a full-stack Java web application that automates the process of answering structured questionnaires — such as security assessments, vendor audits, and compliance forms — using internal reference documents.

The system parses uploaded questionnaires into individual questions, retrieves the most relevant content from reference documents, generates grounded answers with citations, and exports a formatted Word document ready for submission.

---

## Fictional Company

**Industry:** HealthTech / SaaS  
**Company:** *NovaMed Health* — a cloud-based Electronic Health Records (EHR) and practice management SaaS platform serving outpatient clinics and specialty practices across India. Founded 2019, headquartered in Bengaluru, serving 1,200+ clinics with 3,500 active providers and INR 18.2 Crore ARR.

**6 Reference Documents (in `/data/`):**

| File | Contents |
|------|----------|
| `company_overview.txt` | Company profile, ARR, active users, leadership |
| `security_and_compliance.txt` | ISO 27001, SOC 2, encryption, access control, pen testing |
| `data_privacy_policy.txt` | DPDP Act 2023 compliance, data retention, subject rights |
| `infrastructure_sla.txt` | AWS hosting, SLA tiers (99.9% enterprise), RTO/RPO, backups |
| `integrations_and_api.txt` | Lab/pharmacy integrations, REST + FHIR R4 API, OAuth 2.0 |
| `support_and_training.txt` | Onboarding, training modules, support SLAs by severity |

**Sample Questionnaire:** `data/sample_questionnaire.txt` — 15-question vendor security assessment

---

## Features Implemented

### Phase 1 — Core Workflow ✅
- **User Authentication** — Register, login, logout via Spring Security 6 + BCrypt
- **Reference Document Upload** — PDF, DOCX, XLSX, TXT with auto text extraction
- **Questionnaire Upload** — Accepts PDF, DOCX, TXT; auto-parsed into individual questions
- **Intelligent Question Parser** — Detects numbered lists (Q1., 1., *), question marks, question-starter words
- **AI Answer Generation** — Google Gemini API integration; falls back to keyword-based retrieval if no key set
- **Citations** — Every answer cites the exact source document
- **"Not found in references"** — Explicit fallback when no relevant content found
- **Structured Web Review** — Question + Answer + Confidence + Evidence + Citation per question

### Phase 2 — Review & Export ✅
- **Inline Answer Editing** — Edit any answer in place, saved instantly via AJAX
- **Exportable DOCX** — Structured Word document preserving original question order
- **Export includes** — Questions, answers, citations, confidence scores, evidence snippets, edited indicators, coverage summary

### Nice-to-Have Features ✅ (4 out of 5)

| Feature | Implementation |
|---------|---------------|
| Confidence Score | Keyword match ratio shown as % with colour bar (green/yellow/red) |
| Evidence Snippets | 120-char excerpt from source doc shown under each answer |
| Partial Regeneration | Checkbox per question + "Regenerate Selected" button |
| Coverage Summary | Total / Answered / With Citations / Not Found + progress bar |

---

## How Answer Generation Works

The system uses a **3-stage keyword retrieval pipeline**:

1. **Keyword extraction** — Strips stop words from the question, extracts meaningful terms (e.g. "encryption", "AES", "transit", "breach", "notification")
2. **Document scoring** — Scores each reference document by how many keywords it contains; selects the best matching document
3. **Paragraph extraction** — Within the best document, finds the paragraph or top 3 sentences with the highest keyword density and returns them as the answer

**Example — Q1 (Encryption):**
> *"All data at rest is encrypted using AES-256 encryption. All data in transit is encrypted using TLS 1.2 and TLS 1.3."*  
> — cited from `security_and_compliance.txt` | Confidence: 92%

**Example — Q5 (Incident Response):**
> *"0-1 hour: Incident detected and contained. 24 hours: Affected customers notified. 72 hours: Regulatory breach notification per DPDP Act 2023."*  
> — cited from `security_and_compliance.txt` | Confidence: 78%

**Gemini AI integration** is also built in. When a valid `GEMINI_API_KEY` environment variable is set, the system automatically switches to Google Gemini API for higher-quality natural language answers grounded in the same reference documents.

---

## Technology Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Language | Java 21 | LTS, modern features |
| Framework | Spring Boot 3.2 | Production-grade, fast setup |
| Security | Spring Security 6 + BCrypt | Secure auth out of the box |
| Database | H2 file-based + Spring Data JPA | Persistent, zero-config for demo |
| Templates | Thymeleaf | Server-side rendering |
| AI Integration | Google Gemini API (via Java HttpClient) | Free tier available |
| Document Parsing | Apache PDFBox + Apache POI | PDF, DOCX, XLSX, TXT support |
| Document Export | Apache POI XWPF | Structured DOCX generation |
| Build | Maven 3 | Standard Java build tool |

---

## Project Structure

```
questionnaire-tool/
├── pom.xml
├── data/                                    # Sample data files
│   ├── company_overview.txt
│   ├── security_and_compliance.txt
│   ├── data_privacy_policy.txt
│   ├── infrastructure_sla.txt
│   ├── integrations_and_api.txt
│   ├── support_and_training.txt
│   └── sample_questionnaire.txt
└── src/main/
    ├── java/com/almabase/questionnaire/
    │   ├── QuestionnaireApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   └── AppConfig.java
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   ├── DashboardController.java
    │   │   ├── ReferenceDocumentController.java
    │   │   └── QuestionnaireController.java
    │   ├── dto/
    │   │   ├── AnswerUpdateRequest.java
    │   │   └── RegenerateRequest.java
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── ReferenceDocument.java
    │   │   ├── QuestionnaireRun.java
    │   │   └── QuestionAnswer.java
    │   ├── repository/
    │   ├── security/
    │   │   └── CustomUserDetailsService.java
    │   └── service/
    │       ├── AnthropicService.java       # Gemini API + keyword fallback
    │       ├── DocumentParserService.java  # Text extraction + question parsing
    │       ├── ExportService.java          # DOCX generation
    │       ├── QuestionnaireService.java   # Core orchestration
    │       ├── ReferenceDocumentService.java
    │       └── UserService.java
    └── resources/
        ├── application.properties
        ├── static/css/style.css
        └── templates/
            ├── dashboard.html
            ├── auth/login.html
            ├── auth/register.html
            └── questionnaire/
                ├── upload.html
                └── review.html
```

---

## Getting Started

### Prerequisites
- Java 21+ → `java -version`
- Maven 3.8+ → `mvn -version`

### Run

```bash
cd questionnaire-tool
mvn spring-boot:run
```

Open: `http://localhost:8080`

### Optional: Enable Gemini AI answers

```bash
# Windows
set GEMINI_API_KEY=your-key-here

# Mac/Linux
export GEMINI_API_KEY=your-key-here
```

Get a free key at: https://aistudio.google.com/apikey

> Without a key, the system uses built-in keyword retrieval which produces accurate, grounded answers from the reference documents.

### Demo Flow

1. Go to `/register` → create an account
2. Login
3. Upload the 6 reference `.txt` files from `data/` (not `sample_questionnaire.txt`)
4. Click **New Questionnaire** → upload `data/sample_questionnaire.txt`
5. Click **Generate Answers**
6. Review and edit any answers
7. Click **Export DOCX** → download completed Word document

---

## Assumptions Made

1. **Per-user document scoping** — Each user's reference documents are isolated; no cross-account sharing
2. **All docs retrieved per question** — Every reference document is searched for every question. Simpler than vector search, works well for under 20 documents
3. **Question detection by heuristics** — Handles numbered lists, question marks, question-starter words. Unusual formats may need custom handling
4. **Content truncation for AI** — Documents truncated to 2,500 chars when sent to Gemini API to stay within token limits. Full content used for local keyword matching
5. **H2 for persistence** — File-based H2 used for zero-config demo. Production would use PostgreSQL
6. **Synchronous processing** — Questions answered sequentially with rate-limit delay. Fine for 15 questions; async would be needed at scale

---

## Trade-offs

| Decision | Trade-off Made | Better Alternative |
|----------|---------------|-------------------|
| H2 file database | Zero config, works immediately | PostgreSQL for production |
| Keyword retrieval (default) | Free, no API needed, deterministic | Vector embeddings + semantic search |
| All docs in every query | Simple, no indexing needed | Pre-index with embeddings, retrieve top-k |
| Synchronous API calls | Simple code | Async/parallel with Java virtual threads |
| Thymeleaf SSR | No JS build step needed | React/Vue SPA for richer editing UX |
| Sequential question processing | Avoids rate limits | Parallel processing with backoff |

---

## What I Would Improve With More Time

1. **Vector embeddings** — Replace keyword matching with sentence-transformer embeddings + pgvector for semantic retrieval. Handles paraphrased questions much better
2. **Async parallel processing** — Use Java 21 virtual threads to answer all questions in parallel, reducing wait time from ~N seconds to ~1 second
3. **PostgreSQL** — Production-grade database with proper connection pooling
4. **Streaming answers** — Show answers appearing in real-time using SSE instead of waiting for all to complete
5. **Docker Compose** — `docker-compose up` for one-command deployment with no Java/Maven install needed
6. **Version history with diff** — Store multiple runs per questionnaire and show what changed between versions
7. **Excel questionnaire support** — Parse XLSX files where questions are in a specific column
8. **Answer confidence improvement** — Use BM25 ranking instead of simple keyword counting for better relevance scoring
9. **Admin dashboard** — View all users, runs, and system health metrics
10. **Email notifications** — Notify user when long-running processing completes