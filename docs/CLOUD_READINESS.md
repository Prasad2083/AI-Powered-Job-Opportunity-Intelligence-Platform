# Future cloud readiness

Initial release remains on the user's Mac. Do not apply the legacy Render blueprint: it specifies paid plans, contains obsolete OpenAI configuration, and is not a deployment recommendation. No hosting has been provisioned.

When cloud hosting is explicitly requested and approved:

1. Finish local acceptance, historical credential rotation and release-blocking checks. Select a provider and a reviewed cost ceiling before provisioning anything.
2. Separate runtime secrets from the repository; require TLS and narrow origins, add abuse controls and review browser token storage/session design.
3. Use PostgreSQL 16+ with verified Flyway upgrades, encrypted backups and a practiced restore. Preserve original resumes and version history; define retention/deletion policies.
4. Containerize the verified backend/frontend without default passwords. The old all-services compose stack and Render configuration need a separate review; the validated startup path is `docker-compose.local.yml` plus local Java/React.
5. Size free/open-source Ollama inference based on measured model latency and memory. Cloud CPU/GPU costs require approval; do not silently substitute paid APIs.
6. Add ingestion queues, provider-aware retries/metrics, resource limits, security headers and dependency/secret monitoring. Recheck the two current Spring MVC applicability reviews or patch when possible.
7. Implement authorized application adapters only after provider permission, explicit user eligibility/approval rules and receipt reconciliation exist. Cloud hosting does not confer submission authorization.
8. Deploy to a private test environment, verify data isolation and rollback/restore, then request any public release approval with concrete evidence.
