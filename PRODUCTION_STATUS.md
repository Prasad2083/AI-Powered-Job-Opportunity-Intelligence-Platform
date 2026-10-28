# Release status: local development candidate; do not merge or deploy yet

The existing project now builds and has validated local workflows using PostgreSQL 16, Java 21, React and mocked local Ollama protocol responses. See [verification](docs/VERIFICATION.md) for executed tests and their limits, and [implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md) for acceptance status.

Implemented: secure user-scoped workflows and revocable JWT sessions; PDF/DOCX/TXT resume management with original-byte preservation; configurable Greenhouse/Lever ingestion and freshness; explained deterministic matching; source-constrained Ollama drafts with edit/review/version saving; manual application history/status tracking; profile preferences; improved local launcher; meaningful CI and vulnerability scans.

No OpenAI API key is required. No provider-confirmed submission capability is implemented. Real submissions stay disabled; manual completions are explicitly user-reported. Free-form factual AI rewriting and automated eligibility remain incomplete.

Release gates still include native macOS/Ollama acceptance, historical credential rotation, review of known limitations, and successful current CI. The PR should remain draft; no merge or deployment is authorized until the original release requirements are met. Nothing has been publicly hosted.
