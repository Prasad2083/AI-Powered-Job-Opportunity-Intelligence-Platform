ALTER TABLE jobs ADD COLUMN last_seen_at TIMESTAMP;
CREATE INDEX idx_jobs_last_seen ON jobs(last_seen_at);
