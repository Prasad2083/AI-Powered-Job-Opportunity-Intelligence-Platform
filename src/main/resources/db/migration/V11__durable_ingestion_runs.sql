CREATE TABLE ingestion_queue_lock (id INTEGER PRIMARY KEY CHECK (id=1));
INSERT INTO ingestion_queue_lock VALUES (1);

-- Shared public-feed operational reports only; no private jobs/resumes/user payloads.
CREATE TABLE ingestion_runs (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(16) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED','RUNNING','SUCCEEDED','PARTIAL','FAILED')),
    report JSONB,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    heartbeat_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_ingestion_one_active ON ingestion_runs ((true))
    WHERE status IN ('QUEUED','RUNNING');
