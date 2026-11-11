# Release status: local development candidate; do not merge or deploy yet

Application source was published to the existing development branch as `5e357dc` on 2026-11-11 using the authorized GitHub connector with an expected-head check and a non-force fast-forward. The published source tree exactly matched the locally tested tree. The five original development commits are retained in local merged history, a preservation branch and private Git bundles; no existing work was discarded.

## Verified in GitHub Actions

[CI run 42945174233](https://github.com/Prasad2083/AI-Powered-Job-Opportunity-Intelligence-Platform/actions/runs/42945174233) passed all four jobs on `5e357dc`:

- Java 21 backend: **67 tests passed**, zero failures/errors/skips, including real PostgreSQL 16, authentication/ownership, persistent retry-state concurrency and archived-listing document preservation.
- Flyway V1–V10 fresh startup and V3→V10 historical upgrade preserve existing resume text; historical migrations were not edited.
- Frontend: **13 tests passed**, production build passed and npm audit reported **0 vulnerabilities**.
- Chromium: **8 browser tests passed**, including the real backend/PostgreSQL workflow with only Ollama mocked in that flow.
- OSV: **160 dependencies scanned**, **0 unreviewed/actionable findings**; two version-specific Spring MVC applicability reviews remain visible, not patched or erased.
- Launcher shell syntax passed.

Implemented local workflows include secure user-scoped authentication, original-preserving PDF/DOCX/TXT resume management, public employer discovery with persistent provider retry metadata, explained matching with canonical spellings and immutable per-request preparation, durable source-constrained AI tasks, owned document version preservation after listing archival, TXT/Word downloads, manual application tracking, preferences and the unified home/workspace UI. Original resumes are retained; successful submissions are never invented. No paid AI API is required.

## Remaining release gates and scope

Native Mac/Docker Desktop and actual Ollama/Qwen quality still need acceptance on the user's machine. Resolve/rotate any real credentials formerly tracked in repository history. Keep current CI green and review the remaining [implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md) and [security gates](SECURITY.md) before merging.

Durable ingestion scheduling and dashboard status are implemented in the current increment with local tests; PostgreSQL/Flyway/browser CI acceptance is pending. Broader operational metrics, geographic/mandatory-qualification reasoning and free-form factual AI rewriting remain incomplete. Real submissions require provider/employer authorization, credentials, approved payloads, eligibility rules and verified receipts; they remain disabled. Manual completions are explicitly user-reported. OCR, formatted PDF export and public-hosting hardening are tracked separately in the backlog. Nothing has been publicly hosted; repository visibility has not changed.

The November 15 target remains conditional on the remaining work and external acceptance. Passing Linux/mocked-model checks does not establish real-model or native Mac acceptance.

## Managed execution and publication

The command sandbox still denies the Git proxy, Docker socket and local application-port binding. Those local execution limits do not prevent GitHub connector publication or CI validation: both routes were confirmed working on November 11. Use connector Git-tree/commit/ref operations with exact tree comparison, the current expected head and `force=false` if normal shell Git remains unavailable; preserve local history. Check current GitHub head before every write. Do not keep repeating stalled privileged shell requests or reset the local branch to discard unpublished work.

Users can now pull the feature branch normally; the [offline bundle handoff](docs/OFFLINE_HANDOFF.md) is an optional preservation/recovery route, not required to obtain the published application. Current verification and historical local-only evidence are recorded in [VERIFICATION.md](docs/VERIFICATION.md).
