# Verification evidence

Environment: managed Linux cloud workspace, Java 21 JDK, Maven 3.9.9, PostgreSQL 16 Docker containers, Node/npm and Chromium. Date: 2026-10-28. Repository baseline `9ada487`; see git history for the resulting implementation commits.

## Executed successfully

| Check | Evidence and practical limit |
| --- | --- |
| Backend build and tests | `mvn -B -ntp verify`: 22 tests, no failures/errors/skips at the latest recorded run. Security/REST tests use the real Spring filter chain and PostgreSQL 16. |
| Fresh schema | Flyway V1–V8 migration followed by Hibernate `validate`. No historical migration was edited. |
| Historical upgrade | Seed V3 schema with existing user/resume text, migrate to latest, validate checksums, verify original text retained and previously unstored file bytes remain null. |
| Authentication/isolation | Registration/login; malformed and wrong-signature JWTs; revoked sessions; disabled login; foreign resume edit/read/download/primary access; private manual jobs and saved documents; isolated history; duplicate completion rejection. |
| Resume extraction | Generated real PDF and DOCX fixtures plus TXT uploads; malformed/unsupported/empty files; original-byte preservation. No OCR claim. |
| Matching | Deterministic skill evidence and seniority/location explanations, bounded score, explicit years versus unknown date-derived totals. |
| Feed tests | Failed employer isolation, Lever requirement content, stable provider IDs, refresh versus archive semantics. |
| Ollama protocol | `/api/chat` request/response mocks, schema-constrained source references, empty response and invalid evidence rejection. No live Qwen model test. |
| Frontend component | `npm test`: registration success returns to login without a fabricated token. |
| Frontend production build | `npm run build` succeeds with Vite 7. |
| Browser with mocked API | Login → resume preview → logout in Chromium. |
| Full-stack browser | 2 tests passed: real React + Spring Boot + isolated PostgreSQL 16; only Ollama mocked. Register → upload TXT → private job → match → draft → approval/save → report manual completion → update reported status → verify original unchanged/download → logout. |
| Frontend dependencies | `npm audit`: zero reported vulnerabilities after tooling upgrades. |
| Backend dependencies | OSV queried for resolved Maven dependencies. Previous baseline scan: 36 affected packages. Patched scan: no unreviewed actionable findings; two reviewed non-applicable Spring MVC advisories remain in the report. |
| Launcher syntax | `bash -n start-mac.sh`, plus build/readiness behavior exercised by the isolated Linux E2E runner. Native Mac launcher execution remains unverified. |
| Default live feeds | Greenhouse canonical/reddit/gitlab HTTP 200 with listings. Lever ataccama/caseware HTTP 200, including Toronto locations. These checks establish response availability at that time, not continued vacancy status. |

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
