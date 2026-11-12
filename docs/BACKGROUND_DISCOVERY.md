# Background employer discovery

Employer-feed refreshes now use a shared PostgreSQL queue (additive Flyway V11). This replaces long-running browser refresh requests. Private pasted descriptions, resumes and model tasks do not enter this queue.

## API and user behavior

Authenticated `POST /jobs/ingest` returns a recorded run immediately: HTTP 202 for `QUEUED`/`RUNNING`, or HTTP 200 when reusing a terminal result inside the refresh cooldown (default 60 seconds). Concurrent manual/scheduled requests coalesce into one active run across users. Feed listings are shared public data, so authenticated users see the same operational run; it contains counts, configured source names and controlled failure messages, no private documents or manual descriptions.

`GET /jobs/ingestion` returns the latest run, or 204 before any run exists. `GET /jobs/ingestion/{id}` reads a recorded run. The Jobs page restores status after navigation/reload, polls active runs and refreshes saved listings on completion without replacing the manual-entry form. Reports distinguish `SUCCEEDED`, `PARTIAL` (some storage/provider errors), and `FAILED`. A completed run with no successfully checked employer feeds does not verify any live vacancies. Update/restart both backend and frontend together; the previous POST response was a bare synchronous report.

## Processing and recovery

A dedicated single-thread ingestion scheduler polls every second. Database admission locks and a partial unique index allow only one queued/running run. Provider HTTP work occurs outside queue transactions. Existing complete-feed validation, safe archival and per-provider persistent backoff (V10) remain in force; healthy providers can still complete when another fails.

A separate maintenance scheduler updates a running heartbeat every 30 seconds. On polling, a heartbeat older than two minutes becomes a visible `FAILED` run with an interrupted-refresh explanation; already committed listing updates may remain. Queued work survives a process restart. Interrupted work is not silently declared successful or automatically replayed; a later explicit/scheduled request may retry after the shared cooldown. A late result cannot replace a terminal failure. This is lease-based recovery with idempotent provider-scoped upserts, not distributed exactly-once execution or fencing of a partitioned worker.

`app.job-ingestion.enabled=false` disables scheduling and rejects new requests. `jobpulse.ingestion.worker-enabled=false` stops scheduled admission/processing for isolated tests; do not set this in normal local use. Existing `job.ingest.delay` controls periodic admission (default hourly). Existing `jobpulse.feeds.minimum-refresh-ms` controls refresh cooldown. No Kafka, new dependency, paid service or application submission was introduced. Run/report retention and richer operational metrics remain backlog items.

## Review workflow

1. Register/login; on Jobs choose **Find jobs**. Expect queued/running status or a recent cached result.
2. Navigate to Resumes and return, or reload. The recorded status remains available.
3. Review counts and source failure explanations. Only complete successful feeds can archive missing listings. No successful feed checks means no newly verified vacancy claim.
4. Continue private pasted-job entry while discovery runs. Review the original employer page before applying; submissions remain manual and user-reported.

Automated coverage includes worker heartbeat/failure behavior, PostgreSQL request coalescing, cooldown persistence, interrupted-worker recovery, late-result rejection, report persistence/classification, authenticated REST status, component status restoration and real-stack browser reload. See [verification evidence](VERIFICATION.md) for what actually ran.
