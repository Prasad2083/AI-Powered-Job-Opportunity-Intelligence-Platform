-- Immutable saved versions: each save creates a separate document.
CREATE TABLE tailored_documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    resume_id BIGINT NOT NULL REFERENCES resumes(id),
    job_id BIGINT NOT NULL REFERENCES jobs(id),
    content TEXT NOT NULL,
    original_snapshot TEXT NOT NULL,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tailored_documents_owner ON tailored_documents(user_id);
