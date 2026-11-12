-- Additive only: retain all historical migrations and resume text.
ALTER TABLE jobs ADD COLUMN owner_user_id BIGINT REFERENCES users(id);
CREATE INDEX idx_jobs_owner ON jobs(owner_user_id);
-- Historical manual rows have no provable owner and are hidden by visibility queries.
ALTER TABLE resumes ADD COLUMN original_file BYTEA;
