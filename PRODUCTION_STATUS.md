# Release status: local development candidate; do not merge or deploy yet

The existing project now builds and has validated local workflows using PostgreSQL 16, Java 21, React and mocked local Ollama protocol responses. See [verification](docs/VERIFICATION.md) for executed tests and their limits, and [implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md) for acceptance status.

Implemented: durable background resume tailoring with owner-only task status, source snapshots, cancellation and recovery; a unified home/workspace visual system and saved Word/DOCX exports, paged job filtering and editable application notes; a professional public home page and account entry flow; private full job-description review; secure user-scoped workflows and revocable JWT sessions; PDF/DOCX/TXT resume management with original-byte preservation; configurable Greenhouse/Lever ingestion and freshness; explained deterministic matching; source-constrained Ollama drafts with edit/review/version saving; manual application history/status tracking; profile preferences; improved local launcher; meaningful CI and vulnerability scans.

No OpenAI API key is required. No provider-confirmed submission capability is implemented. Real submissions stay disabled; manual completions are explicitly user-reported. Free-form factual AI rewriting and automated eligibility remain incomplete.

Release gates still include native macOS/Ollama acceptance, historical credential rotation, review of known limitations, and successful current CI. The PR should remain draft; no merge or deployment is authorized until the original release requirements are met. Nothing has been publicly hosted.

## Current execution and publication gate (2026-11-11 UTC)

The remote development branch was last confirmed at `8d295ed`. Local commits include persistent retry metadata (`3087a67`), the product/hosting backlog (`ed7fb02`) and a subsequent archived-source document preservation fix. Those changes are not established as published or fully integrated.

Current local validation: 57 non-PostgreSQL backend tests pass, including matching/controller isolation checks; 576 scenarios match the pre-refactor scoring results. The last frontend run passed 13 tests and the production build; frontend source did not change in the latest increment. The sandbox denies outbound proxy/Docker-socket access and Vite binding to 127.0.0.1:3000. Network-enabled validation/push and source-publication attempts were interrupted without a confirmed execution result. Runtime network policy reports unrestricted access, which does not establish command-sandbox access. This is an execution/publication blocker, not evidence that provider credentials are missing.

To unblock: provide an execution context that can reach the configured Git proxy, the local Docker daemon and local application ports. Then verify V1–V10 fresh/upgrade migrations, run all backend/full-stack/browser and dependency checks, push without force and inspect current CI. Preserve the local commits; do not reset to the remote branch. The November 15 release target remains contingent on this gate, native Mac/model acceptance and the existing security/provider constraints.

A subsequent local matching increment normalizes common skill spellings and clarifies mention-versus-proficiency wording; its parser/matcher and frontend checks pass offline. A [Git bundle handoff](docs/OFFLINE_HANDOFF.md) is available as an optional route to a working Mac environment. This is a transfer option, not proof that the bundle has been downloaded, integrated, published or tested there.

The latest local matching refactor prepares immutable data once per request and corrects multiline senior-title detection. It introduces no shared user cache, schema change or scoring-weight change. Fresh access probes on November 11 confirmed the same command-sandbox failures. The November 15 target cannot be confirmed while integration/publication and external acceptance are unresolved.
