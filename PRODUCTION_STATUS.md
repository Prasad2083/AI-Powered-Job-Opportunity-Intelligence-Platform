# Release status: tested development candidate; Mac/model acceptance remains open

Earlier application increment **`68dac00`** and workflow follow-up **`5c5795f`** were published to the existing feature branch on November 11 without force. Published trees exactly matched local trees; original granular development commits remain preserved in merged local history and preservation branches. No existing work, historical migration or original resume was discarded.

## Current verified editor and matching candidate

Published **`43865f1`** includes resume editor race safeguards and explicit-denial matching fix **`10a7ff3`**. [CI run 43237018570](https://github.com/Prasad2083/AI-Powered-Job-Opportunity-Intelligence-Platform/actions/runs/43237018570) passed all four jobs: **77 backend tests** (13 PostgreSQL/REST), fresh V1–V11 and V3→V11 preserved-resume upgrades, **21 frontend tests**, production build/npm audit 0, and **9 full-stack browser tests**. OSV scanned 160 dependencies with 0 unreviewed/actionable findings; two applicability reviews remain visible. Shell syntax passed.

The browser flow verifies denied-skill review, saved extracted text after reopening, and unchanged original download bytes. Failed saves retain drafts; superseded previews cannot replace the latest selection. See [editing safeguards](docs/RESUME_EDITING.md) and [skill evidence](docs/SKILL_EVIDENCE.md). Cross-tab conflicts, unsaved navigation and full semantic eligibility remain incomplete. The older matching CI run stayed queued; current combined source acceptance comes from the successful run above, not an invented completion of that old run.

Local frontend tests/build passed; a fresh local Vite probe failed `listen EPERM 127.0.0.1:3000`, so browser/database acceptance came from CI. Mac/model acceptance, historical credential rotation and release-scope approval remain open. No main merge, deployment or real submission occurred.

## Earlier discovery candidate (superseded by combined source above)

[CI run 43035912511](https://github.com/Prasad2083/AI-Powered-Job-Opportunity-Intelligence-Platform/actions/runs/43035912511) passed all four jobs on `5c5795f`:

- **72 backend tests passed**, zero failures/errors/skips, including **12 PostgreSQL/REST integration tests** and authentication/user-isolation coverage.
- Fresh **V1–V11** migrations and a historical **V3→V11** upgrade passed, preserving original resume text.
- **16 frontend tests**, production build and npm audit **0 vulnerabilities**.
- **8 full-stack browser tests**, including discovery completion/reload and original-preserving resume/tailoring/manual-history workflows. Only Ollama is mocked in the real-stack flow; standalone frontend scenarios use explicit API fixtures.
- OSV scanned **160 dependencies**: **0 unreviewed/actionable findings**. Two version-specific Spring MVC applicability reviews remain visible, not patched or erased. Launcher syntax passed.

The new shared employer-discovery queue coalesces refreshes, persists reports, records interrupted workers and restores/polls dashboard status without overwriting private manual-entry edits. Reports contain no private resume/manual-job payloads. Existing provider retry deadlines and complete-feed archival safeguards remain in place. See [background discovery](docs/BACKGROUND_DISCOVERY.md).

Initial browser CI failed during Ubuntu apt/font mirror DNS resolution before tests. The workflow now uses preinstalled native Chromium libraries and installs the browser without redundant apt setup; the successful run above verifies actual browser execution. Local Docker/port restrictions still prevent those checks in the command sandbox; CI supplied the integration evidence.

## Earlier published baseline (superseded by current candidate)

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

Durable ingestion scheduling and dashboard status now pass PostgreSQL/Flyway/browser CI. Broader operational metrics, geographic/mandatory-qualification reasoning and free-form factual AI rewriting remain incomplete. Real submissions require provider/employer authorization, credentials, approved payloads, eligibility rules and verified receipts; they remain disabled. Manual completions are explicitly user-reported. OCR, formatted PDF export and public-hosting hardening are tracked separately in the backlog. Nothing has been publicly hosted; repository visibility has not changed.

The November 15 target remains conditional on the remaining work and external acceptance. Passing Linux/mocked-model checks does not establish real-model or native Mac acceptance.

## Managed execution and publication

The command sandbox still denies the Git proxy, Docker socket and local application-port binding. Those local execution limits do not prevent GitHub connector publication or CI validation: both routes were confirmed working on November 11. Use connector Git-tree/commit/ref operations with exact tree comparison, the current expected head and `force=false` if normal shell Git remains unavailable; preserve local history. Check current GitHub head before every write. Do not keep repeating stalled privileged shell requests or reset the local branch to discard unpublished work.

Users can now pull the feature branch normally; the [offline bundle handoff](docs/OFFLINE_HANDOFF.md) is an optional preservation/recovery route, not required to obtain the published application. Current verification and historical local-only evidence are recorded in [VERIFICATION.md](docs/VERIFICATION.md).
