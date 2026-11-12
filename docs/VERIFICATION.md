# Verification evidence

Environment: managed Linux cloud workspace, Java 21 JDK, Maven 3.9.9, PostgreSQL 16 Docker containers, Node/npm and Chromium. Date: 2026-10-28. Repository baseline `9ada487`; see git history for the resulting implementation commits.

## Resume editor request/save race increment (2026-11-11; CI pending)

Superseded previews are aborted and their late responses ignored. Open editors block competing preview/upload/primary changes; save freezes the editor and Cancel, then closes on success or retains text on failure. No backend/migration/dependency changes. Cross-tab conflicts, navigation guards and offline persistence remain incomplete; see [editing safeguards](RESUME_EDITING.md).

Executed locally: **21 frontend component tests passed**, zero failures, production frontend build and whitespace checks passed. New tests cover out-of-order previews, the exact saved resume/text, mutation locking, failure/retry draft retention and abort on unmount. Added a delayed mocked-browser save/failure scenario and extended real-stack save/reopen/original-byte assertions; browser execution is pending. Backend checks were not rerun for this frontend-only increment; earlier 64 local non-PostgreSQL tests remain prior evidence. The previous matching CI run 43095411148 is still queued with no jobs started, so its new PostgreSQL/Flyway/browser/dependency acceptance is not claimed.

## Explicit skill denial increment (2026-11-11; CI pending)

Recognized resume denials now withhold skill credit, including contradictory mentions, and appear in an additive `skillsRequiringContextReview` field and the Matches explanation. Existing API detection remains dictionary-based; no semantic eligibility or whole-score negation filtering is claimed. Original resume text/bytes, scoring weights and migrations are unchanged. See [limitations](SKILL_EVIDENCE.md).

Executed locally: **64 non-PostgreSQL backend tests**, **17 frontend tests**, production frontend build and whitespace checks passed. New PostgreSQL REST/original-preservation and browser review assertions are included for CI; not claimed executed locally. Native Mac/model and historical credential gates remain open.

## Background discovery acceptance (2026-11-11 UTC)

Published application increment **`68dac00`** adds V11 with a single shared employer-refresh queue and operational reports. Manual/scheduled requests coalesce; active work returns 202, recent terminal results return 200. A separate worker/heartbeat records interrupted runs without fabricating success; late results cannot replace failures. Jobs restores and polls status while preserving private manual-entry edits. Existing V1–V10 and provider backoff/archival rules remain unchanged. Workflow/documentation follow-up **`5c5795f`** retains exactly the same application source.

[CI run 43035912511](https://github.com/Prasad2083/AI-Powered-Job-Opportunity-Intelligence-Platform/actions/runs/43035912511) passed **all four jobs** on `5c5795f`:

- Java 21 Maven: **72 tests passed**, zero failures/errors/skips, including **12 real PostgreSQL/REST integration tests**. New cases cover authenticated/coalesced refresh admission across users, persisted reports/cooldowns, heartbeat recovery, late-result rejection and partial/total feed failures. Worker unit cases cover idle polling, controlled errors and heartbeat cleanup for large IDs.
- Flyway fresh **V1–V11**, Hibernate validation and historical **V3→V11** upgrade passed, preserving original resume text. Only V11 was added; historical migrations were not changed.
- Frontend: **16 tests passed**, production build passed, npm audit **0 vulnerabilities**. Component tests cover status restoration, manual-entry preservation, failure messages and duplicate-request prevention.
- Browser: **8 tests passed** in the full-stack job, including real backend/PostgreSQL registration → discovery completion/reload → original-preserving resume/draft/manual-history flow, with only Ollama mocked in that flow. The standalone frontend job separately passed **7 mocked browser scenarios** and intentionally skipped the real-API scenario; it is executed in the full-stack job. Responsive workspace checks include discovery status at 1440px, 390px and 320px.
- OSV scanned **160 dependencies**: **0 unreviewed/actionable advisories**; two existing Spring MVC applicability reviews remain visible, not claimed patched. Launcher shell syntax passed. No live application submission occurred.

The first [run 43034008967](https://github.com/Prasad2083/AI-Powered-Job-Opportunity-Intelligence-Platform/actions/runs/43034008967) passed backend/security and frontend component/build/audit checks, but both browser jobs failed **before tests** because Ubuntu apt/font mirrors could not resolve. Logs confirmed native Chromium libraries were already installed. The follow-up installs Chromium using hosted native libraries rather than repeating apt setup; browser startup and real tests still fail normally if a required library is missing. The successful run above verifies this correction, not a suppressed error.

Executed locally: **60 non-PostgreSQL backend tests**, **16 frontend tests**, production frontend build and whitespace checks passed. Local browser startup stopped before tests under sandbox port restrictions; Docker/socket restrictions prevented local PostgreSQL execution. CI supplied the database/browser evidence. Native Mac, actual Qwen quality/performance and historical credential rotation remain unverified. See [background discovery](BACKGROUND_DISCOVERY.md) and [Mac acceptance](../MAC_LOCAL_SETUP.md).

## Published source and CI acceptance (2026-11-11 UTC)

Published application snapshot **`5e357dc`** to `feature/production-workflow-20261027` without a force update. Its Git tree SHA exactly matched the tested local source. Original local commits were retained and reconciled by merging identical trees; no source changes or user work were discarded.

[GitHub Actions run 42945174233](https://github.com/Prasad2083/AI-Powered-Job-Opportunity-Intelligence-Platform/actions/runs/42945174233) passed backend, frontend, full-stack browser and shell jobs. Job logs confirm **67 backend tests passed** (zero failures/errors/skips), including **10 PostgreSQL/REST integration tests**. These executed the V10 retry-state persistence/concurrency/extreme-deadline checks and archived-listing provenance workflow that were previously pending. Fresh V1–V10 migrations and a historical V3→V10 upgrade passed, preserving existing resume text.

Frontend logs confirm **13 tests passed**, production build and npm audit **0 vulnerabilities**. Full-stack browser logs confirm **8 tests passed**, using the real backend and isolated PostgreSQL with only Ollama mocked in that flow; other browser scenarios use explicit API fixtures. OSV scanned **160 dependencies**, with **0 unreviewed/actionable advisories** and the same two retained Spring MVC applicability reviews. Launcher syntax passed. No live application submission was attempted.

The local shell's Docker/proxy/port restrictions remain, but the GitHub connector can publish reviewed source and CI provides the previously blocked integration checks. The dated local sections below are historical evidence; their then-pending publication/CI statements are superseded by this section for the code in `5e357dc`. Real Mac/Qwen acceptance, historical credential rotation and remaining release gates are not superseded or claimed complete.

## Per-request matching preparation (local, 2026-11-11 UTC)

The Matches controller now prepares immutable resume skill mentions, text vectors, explicit years, seniority keywords and role/location preferences once per authenticated request. Each job still gets a fresh result. Nothing is stored in a shared user cache, and no API shape, scoring weights, database schema or dependencies changed. Seniority detection also handles titles containing line breaks.

Executed offline: **57 non-PostgreSQL backend tests passed**, zero failures/errors/skips. New checks cover known cosine results, empty vectors, repeated and concurrent context use, result mutation isolation, different users/preferences on the same controller, missing primary resumes and multiline senior titles. A one-off reference harness compiled the pre-refactor matcher/text scorer from `b02fd15` under separate class names and confirmed exact result-map parity for **576 scenarios** (six resumes, eight jobs, three role lists and four location lists); this corpus intentionally excludes the corrected multiline-title case. This is regression evidence, not a latency/throughput benchmark.

No frontend code changed in this increment; the previous 13-test/build evidence is retained rather than described as a new run. Current access probes again failed: Git proxy connection refused/unreachable from sandbox, Docker socket operation not permitted, and local bind denied. No new PostgreSQL/Flyway, browser, dependency-scan or CI pass is claimed. Rebuild the private Git bundle after this commit and follow [the handoff](OFFLINE_HANDOFF.md) if managed execution cannot be restored.

## Skill spelling normalization (local, 2026-11-10 UTC)

Recognizes C#/C Sharp/c-sharp as csharp; REST-Assured/RESTAssured as rest assured; Postgres as postgresql; and K8s as kubernetes. Matching uses canonical names without duplicate alias credit, precompiled Unicode-aware boundaries and flexible whitespace. Locale-independent handling covers Turkish default-locale behavior without altering the source resume. Ambiguous ordinary words such as “go” are not newly promoted into a programming qualification.

Executed offline: **49 non-PostgreSQL backend tests passed**, zero failures/errors/skips, including 5 new parser cases and a matcher alias regression; frontend **13 tests passed** and production build passed. The match explanation/UI explicitly describes detected skills as text mentions, not proficiency evidence; negation and qualification context still require review.

No dependency or migration changes in this increment. Current PostgreSQL/Flyway/browser/scan/CI gates remain pending because runtime access and publication are still blocked. A private [offline handoff](OFFLINE_HANDOFF.md) offers a transfer/verification path without resetting or overwriting the existing branch; successful download, import or execution is not assumed.

## Archived-listing draft preservation (local, 2026-11-10 UTC)

Fixed the save path so a completed owned task or an owned saved version can preserve its draft after the source listing is archived. Provenance still has to match the user, resume and job. Unanchored drafts require a currently visible job, and new tasks for archived listings remain rejected. Resume text and original uploads are not mutated.

Executed locally: offline Java 21 Maven **43 non-PostgreSQL tests passed**, zero failures/errors/skips, including **8 controller regression cases** for owned source snapshots, missing/foreign/mismatched provenance, unanchored saves and resume ownership; frontend **13 tests passed**; production build passed; `git diff --check` passed. The existing PostgreSQL REST workflow now archives the source job before saving task/document versions and asserts new generation is rejected, but this regression has **not run** in the current sandbox.

A Chromium/Playwright attempt stopped before tests because its web server could not start; a direct Vite probe confirmed `listen EPERM: operation not permitted 127.0.0.1:3000`. Docker/proxy access and network-enabled validation/publication requests remain unconfirmed or blocked, so no current PostgreSQL/Flyway, browser, dependency-scan or remote CI pass is claimed. The last confirmed remote head is `8d295ed`; newer local commits must be published and validated before release.

## Persistent feed retry increment (2026-11-10 UTC)

Provider retry counts/deadlines now use PostgreSQL through a transactional row-locked adapter; isolated feed tests retain an in-memory adapter. V10 adds operational state without editing V1–V9 or resume data. Deadline epoch seconds/nanoseconds preserve extreme provider delays without overflowing PostgreSQL timestamp range. Invalid slugs are rejected before state or HTTP access.

Local offline Maven execution passed **35 non-PostgreSQL tests**, including 9 feed/retry cases, using cached dependencies and a preloaded Mockito agent because dynamic self-attachment is blocked in this sandbox. The current shell sandbox denies both proxy and Docker-socket access, so fresh/upgrade migrations, Spring/database integration and full browser acceptance must run in GitHub Actions before this increment is marked verified. GitHub read operations remain available, but source publication through shell or connector has not been confirmed. See the PR for current CI state; no Mac/model acceptance is implied.

## Feed retry resilience increment (2026-11-09 UTC)

Executed Java 21 Maven verify: **42 tests passed**, zero failures/errors/skips, including real PostgreSQL/REST/security/Flyway coverage. The **7 feed/retry tests** use controlled HTTP responses and a mutable clock, without sleeping or hitting live providers. They check bounded exponential delay, successful-response reset, HTTP 429/503 Retry-After seconds and RFC dates, malformed/past/overflowing delay values, preserved longer provider deadlines, independent healthy feeds and failed later-page retention.

No dependencies, frontend behavior or database migrations changed. Backoff is per employer and held in memory; a restart resets it. Retry eligibility is checked during normal scheduled/manual refresh, not by a separate wake-up timer. Failed/deferred feeds are excluded from successful-source checks, preserving existing archival safeguards. Frontend/browser/security evidence below refers to preceding runs until current CI revalidates this commit.

## Background tailoring increment (2026-11-08 UTC)

Executed in managed Linux: Java 21 Maven verify **37 tests passed**, zero failures/errors/skips; frontend **13 tests passed** and production build passed; Chromium **8 browser tests passed** against isolated real PostgreSQL/backend with only Ollama mocked in the full-stack workflow. The browser queued a draft, refreshed the application, reopened its persisted result, saved/exported it and completed the existing manual-tracking workflow. Responsive workspace checks passed at 1440px, 390px and 320px.

New PostgreSQL cases cover task ownership and inaccessible jobs, duplicate/concurrent admission, queue capacity, global running-slot exclusion, queued/running cancellation, discarded cancelled output, immutable input snapshots, saved-version provenance, worker failure/explicit retry, interrupted-lease recovery and rejection of late results. Flyway V1–V9 validated on fresh startup and V3→V9 preserved historical resume text; V1–V8 were not edited.

Dependency scans rerun: npm **0 vulnerabilities**; OSV **160 resolved dependencies**, **0 unreviewed/actionable advisories**, with the same two version-specific Spring MVC applicability reviews retained. No dependency changes. `git diff --check` passed. Actual Qwen execution, native Mac behavior, production load and destructive crash testing remain unverified; lease recovery was exercised by persisted expired running state and a fresh service instance, not by killing the user's application.

## Matching accuracy increment (2026-11-05 UTC)

The matcher now uses explicit experience statements (for example, `3–5 years of professional experience` → 3 years), rather than every year mention. Regression cases cover unrelated company age/education, fractional experience, multiple mentions, zero-year requirements, missing values, location word boundaries, punctuation and `Canada (Remote)` versus `Remote US`.

This intentionally conservative parser does not recognize every wording (for example, `five years` or `5 years Java`), distinguish mandatory from preferred requirements, infer career length from dates, or verify work authorization. It takes the largest recognized experience minimum, which can refer to a particular skill rather than overall experience. Location matching is still a phrase heuristic, not a geographic eligibility decision. No new dependencies or database changes.

Executed for this increment: Java 21 `mvn verify` **32 tests passed**, including real PostgreSQL 16, REST/security isolation and Flyway migration coverage; **10 matcher tests passed**. Frontend/browser/security entries below describe earlier runs until current CI revalidates them.

## Executed successfully

| Check | Evidence and practical limit |
| --- | --- |
| Backend build and tests | `mvn -B -ntp clean verify`: 67 tests, no failures/errors/skips in CI on published snapshot 5e357dc (2026-11-11 UTC). Security/REST tests use the real Spring filter chain and PostgreSQL 16. |
| Fresh schema | Flyway V1–V10 migration followed by Hibernate `validate`. No historical migration was edited. |
| Historical upgrade | Seed V3 schema with existing user/resume text, migrate to latest, validate checksums, verify original text retained and previously unstored file bytes remain null. |
| Authentication/isolation | Registration/login; malformed and wrong-signature JWTs; revoked sessions; disabled login; foreign resume edit/read/download/primary access; private manual jobs and saved documents; isolated history; duplicate completion rejection. |
| Resume extraction | Generated real PDF and DOCX fixtures plus TXT uploads; malformed/unsupported/empty files; original-byte preservation. Saved Word exports are reopened with POI to verify Unicode text and facts; invalid export formats and foreign-user downloads are rejected. No OCR claim. |
| Matching | Deterministic skill evidence and seniority/location explanations, bounded score, explicit years versus unknown date-derived totals. |
| Feed tests | Failed employer isolation, Lever requirement content, stable provider IDs, refresh versus archive semantics. |
| Ollama protocol | `/api/chat` request/response mocks, schema-constrained source references, empty response and invalid evidence rejection. No live Qwen model test. |
| Frontend component | `npm test`: 13 tests pass across home/account navigation, registration, inline errors, pending login and safe job-description rendering. |
| Frontend production build | `npm run build` succeeds with Vite 7. |
| Browser with mocked API | Login → resume preview → logout → login again in Chromium; verifies no blank dashboard on re-entry. |
| Full-stack browser | 8 tests passed: real React + Spring Boot + isolated PostgreSQL 16; only Ollama mocked. Home → signup → login → upload TXT → private job → read job description → match → draft → approval/save → report manual completion → update reported status → verify original unchanged/download → logout. The current real-stack test also downloads and checks DOCX bytes, edits application notes, and verifies unsaved edits disable stale Word downloads. |
| Frontend dependencies | `npm audit`: zero reported vulnerabilities after tooling upgrades. |
| Backend dependencies | OSV queried for resolved Maven dependencies. Previous baseline scan: 36 affected packages. Patched scan: no unreviewed actionable findings; two reviewed non-applicable Spring MVC advisories remain in the report. |
| Launcher syntax | `bash -n start-mac.sh`, plus build/readiness behavior exercised by the isolated Linux E2E runner. Native Mac launcher execution remains unverified. |
| Default live feeds | Greenhouse canonical/reddit/gitlab HTTP 200 with listings. Lever ataccama/caseware HTTP 200, including Toronto locations. These checks establish response availability at that time, not continued vacancy status. |

## Home page and job-review increment

The application now opens on a public product home page when signed out. The visual workflow is labelled illustrative and makes no claims of successful submissions or fabricated account activity. The page describes implemented capabilities and clearly retains human review and manual completion.

Additional verification: Chromium at 1440px, 390px and 320px; horizontal-overflow checks; reduced-motion layout; signup/login deep links and reload; browser back navigation; heading focus; inline credential errors; no private API calls from guest pages. Desktop/mobile screenshots were visually reviewed. The full-stack test reads the new private job-description endpoint. PostgreSQL-backed API assertions deny other-user and unauthenticated description access. No database migrations or dependencies were changed in this increment.

The user has reported successful local Mac startup; this is user-reported evidence, not an independent real-model or Apple Silicon test.

## Workspace design and export increment

All five signed-in pages now share the public home page's ivory/green palette, editorial headings, brand, controls and spacing. Desktop and mobile previews were visually reviewed. Browser checks exercise each page at 1440px, 390px and 320px and assert no document-wide horizontal overflow; wide tables scroll within their panel.

Additional checks cover location filtering, 20-row paging and page reset, recently-seen public-feed filtering, notes editing and preference saving. Word exports use the existing Apache POI dependency; no new dependency or database migration was added. The exporter does not infer, add or rewrite professional facts. Downloading the wrong text format from an outdated backend produces restart guidance rather than a falsely labelled DOCX.

## Not completed or not available

- Native macOS, Docker Desktop lifecycle, Apple Silicon and actual Qwen3 4B model execution.
- Real authorized job submission: no employer-managed API keys/permission available; disabled by design.
- OWASP Dependency-Check was attempted, then stopped during the unkeyed initial NVD download (415,957 records). It produced no completed vulnerability verdict. The completed backend scan is OSV, not OWASP.
- Historical credential rotation and validation of all past secrets. Git metadata confirms historical `.env`/log tracking; values were not printed.
- Load, penetration, OCR, cross-browser Safari/WebKit and full geographic eligibility tests.

## Reproduce

```bash
mvn -B -ntp clean verify
mvn -B -ntp dependency:list -DoutputFile=target/security-dependencies.txt
python3 scripts/audit-maven.py
npm --prefix frontend ci
npm --prefix frontend test
npm --prefix frontend run build
npm --prefix frontend audit --audit-level=moderate
(cd frontend && npx playwright install chromium)
bash scripts/run-full-stack-e2e.sh
bash -n start-mac.sh
```

The E2E runner creates its own ephemeral database with random credentials and removes it afterwards. It checks ports first and never reuses the user's normal database. CI installs Chromium system dependencies using Playwright's `--with-deps` option.

Maven test reports: `target/surefire-reports/`; OSV details: `target/osv-report.json`; isolated browser/backend logs: `.local-logs/e2e/`; failures: `frontend/test-results/`. Generated artifacts and local credentials are excluded from git.
