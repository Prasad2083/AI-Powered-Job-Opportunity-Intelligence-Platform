-- A short-lived database lock serializes admission and claims across instances.
CREATE TABLE tailoring_queue_lock (id INTEGER PRIMARY KEY CHECK (id = 1));
INSERT INTO tailoring_queue_lock VALUES (1);

CREATE TABLE tailoring_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    resume_id BIGINT NOT NULL REFERENCES resumes(id),
    job_id BIGINT NOT NULL REFERENCES jobs(id),
    original_snapshot TEXT NOT NULL,
    description_snapshot TEXT NOT NULL,
    apply_url TEXT NOT NULL DEFAULT '',
    status VARCHAR(24) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED','RUNNING','CANCEL_REQUESTED','SUCCEEDED','FAILED','CANCELLED')),
    draft TEXT,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);
CREATE INDEX idx_tailoring_tasks_owner ON tailoring_tasks(user_id, id DESC);
CREATE INDEX idx_tailoring_tasks_pending ON tailoring_tasks(id)
    WHERE status IN ('QUEUED','RUNNING','CANCEL_REQUESTED');
CREATE UNIQUE INDEX idx_tailoring_tasks_one_active_per_user ON tailoring_tasks(user_id)
    WHERE status IN ('QUEUED','RUNNING','CANCEL_REQUESTED');
