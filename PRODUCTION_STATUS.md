# Production integration status — NOT READY TO DEPLOY

This branch is under active development. Do not merge or deploy it yet.

## Implemented in source
- Public job discovery from explicitly configured Greenhouse board tokens and Lever site tokens (if no providers are configured, zero real jobs are returned).
- Mock job ingestion only when `jobpulse.feeds.include-mock=true`.
- Authenticated job matching with local text similarity (a heuristic, not AI embeddings).
- OpenAI Responses API draft tailoring; source resume is preserved, drafts require review.
- Automatic submissions disabled: no provider application submission integration is implemented.
- GitHub CI workflow and foundational unit tests added, results pending.
- Render blueprint prepared, not deployed.

## Required environment
- `JWT_SECRET`: strong random secret; do not use development defaults.
- `OPENAI_API_KEY`: API credential; never commit or expose to client.
- `JOBPULSE_FEEDS_GREENHOUSE_BOARDS`: comma-separated official board slugs.
- `JOBPULSE_FEEDS_LEVER_SITES`: comma-separated public company site slugs.
- `JOBPULSE_FEEDS_INCLUDE_MOCK`: optional true for demos only.

## Open blockers
1. Reconcile JPA entities and original Flyway V1 schema on a fresh PostgreSQL instance.
2. Complete secure user tenancy, registration validation, and token/role persistence.
3. Replace approximate PDF parsing and verify DOCX/text extraction.
4. Add review and explicit approval lifecycle; real submission needs an authorized provider integration.
5. Confirm Greenhouse/Lever integrations against live providers, handle pagination, rate limits, and observability.
6. Test the backend build, migration, security cases and frontend browser journey in CI.
7. Rotate potential secrets found in prior git history; removing .env from this branch does not erase its historical exposure.
8. Perform staging rollout, backups, smoke tests and performance/security checks.

No production deployment or merge has been performed.
