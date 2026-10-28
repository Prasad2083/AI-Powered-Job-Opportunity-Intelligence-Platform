# Implementation and acceptance checklist

Baseline: existing development branch `feature/production-workflow-20261027`, PR #1, initial audited commit `9ada487`. No application rewrite. Flyway V1–V3 remain byte-for-byte unchanged.

`[x]` means implemented with the evidence cited below; `[ ]` means incomplete or not verified in the required environment. Passing tests cover their stated cases, not all possible inputs.

## Foundation and security

- [x] Java 21 Maven build and Spring Boot executable packaging.
- [x] Upgrade Spring Boot 3.2.0 → 3.5.16 and patch affected transitive dependencies; remove unused WebFlux and automatic repository exposure through Spring Data REST.
- [x] BCrypt registration, input bounds, JWT signature/expiry validation, disabled-user checks, HTTP 401 behavior.
- [x] Logout increments a persisted token version and revokes prior sessions. Frontend clears expired sessions.
- [x] Own-user resume, tailored-document, preferences, manual-job and application access. DTOs avoid password-hash serialization.
- [x] PostgreSQL 16 fresh migrations and Hibernate validation; historical V3→latest upgrade preserves resume text. V4–V8 are additive.
- [x] No automatic seed resumes, schema dropping, mock submission success, or hard-coded runtime JWT default.
- [ ] Resolve/rotate any credentials formerly stored in repository history; current tree excludes `.env` and logs.
- [ ] Production abuse controls (distributed throttling, password recovery, TLS/cookie/session hardening) before public exposure. Current local launcher binds to loopback.

## Resumes

- [x] PDFBox PDF, POI DOCX and UTF-8 TXT extraction, empty/unsupported/malformed/oversize validation.
- [x] Store original upload bytes independently from editable extracted text; authorized download.
- [x] Preview/edit extracted text and select a primary resume; primary mutations serialize on the user row.
- [x] Replacement by new upload retains prior originals and selects the new resume for matching.
- [ ] Recover original bytes for historical uploads: impossible if the old application never retained them; re-upload required.
- [ ] OCR for image-only PDFs (currently rejected with a useful error); formatted PDF/DOCX export is not implemented. Tailored TXT export is implemented.

## Jobs and matching

- [x] Configurable multiple-employer Greenhouse and Lever discovery; no single-employer dependency.
- [x] Lever pagination, complete-response validation, bounded timeouts, per-feed failure isolation, explicit refresh reports and refresh cooldown.
- [x] Provider-scoped deduplication, updated listing content, last-seen timestamp and freshness display.
- [x] Archive missing listings only after a complete successful feed refresh; retain previous listings on provider failures.
- [x] Private manual descriptions; historical manual rows without provable ownership are hidden. Mock/manual entries are never labeled verified vacancies.
- [x] Frontend filtering by title/company/location; configurable role/location ranking preferences with Toronto/GTA/remote Canada/hybrid defaults.
- [x] Deterministic explained ranking: skill evidence, resume text, title preferences, seniority keywords, explicit experience-year mentions and location keywords.
- [x] Missing evidence is labeled for review, not asserted as a genuine skill deficiency. Date ranges are not invented into experience totals.
- [x] Live HTTP checks of default boards, including Toronto listings in both selected Lever feeds.
- [ ] Semantic verification of mandatory qualifications, overlapping work-history calculations, geographic/work-authorization eligibility and embeddings. All eligibility remains subject to human review.
- [ ] Provider 429 retry/backoff beyond the current refresh cooldown and failure retention, large-dataset paging/index tuning and ingestion operational metrics.

## Tailoring and applications

- [x] Local Ollama `qwen3:4b` protocol; no paid AI dependency.
- [x] Source-constrained generation: model proposes line references; server validates them and emits only original excerpts plus the unchanged original text. Arbitrary model prose never becomes a professional claim.
- [x] Editable drafts, explicit document approval, immutable saved versions and TXT downloads. Editing clears prior approval in the UI.
- [x] Official application links and explicitly confirmed manual completion; `USER_REPORTED_*`, never provider-confirmed status.
- [x] Duplicate manual record prevention under a per-user database lock; history and status updates.
- [x] Automatic submission globally disabled (501); mock adapter cannot report successful submission.
- [ ] Free-form AI paraphrasing with reliable factual validation. Current extractive mode intentionally does not claim to solve this.
- [ ] Authorized submission integrations, provider receipts, per-application approved payloads, idempotent reconciliation and user-defined automated eligibility rules. Requires employer/provider authorization unavailable in this session.
- [ ] Real Ollama/Qwen run on the user's Mac; protocol and failure behavior were tested with safe local mocks only.

## Frontend, launcher and delivery

- [x] Login/registration, resumes, jobs, matches, draft review/version saving, history and profile/preferences controls.
- [x] Validation/error messages, accessible status updates, basic responsive styling and original download.
- [x] Launcher reuses installed tools, Docker Desktop, database images, model and unchanged frontend dependencies; generates secrets only if absent; verifies both services.
- [x] Frontend component test, production build, mocked browser flow and real-backend/PostgreSQL browser workflow with only Ollama mocked.
- [x] Real dependency scans: npm audit and OSV resolved Maven coordinates. Two version-specific Spring MVC advisories are reviewed as non-applicable, remain visible and expire for re-review.
- [x] CI now runs builds, security/API/PostgreSQL/Flyway tests, frontend tests, browser tests and vulnerability scans; dependency listing alone is not called an audit.
- [ ] Actual macOS/Docker Desktop/Apple Silicon startup acceptance; Linux syntax/build/browser checks do not establish this.
- [ ] Release approval and merge: keep draft until outstanding release gates and current CI are reviewed. No public deployment performed.

Executed results and reproducible commands are in [VERIFICATION.md](VERIFICATION.md). Future features are not silently treated as finished.
