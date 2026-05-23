-- V1__initial_schema.sql
-- Initial database schema for JobPulse Auto Apply platform

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    company_target VARCHAR(255),
    target_roles VARCHAR(1000),
    notifications_enabled BOOLEAN DEFAULT TRUE,
    dark_mode_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Resumes table
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    content_text TEXT,
    skills TEXT,
    experience_years NUMERIC(3, 1),
    certifications TEXT,
    tools TEXT,
    embedding_generated BOOLEAN DEFAULT FALSE,
    embedding_vector BYTEA,
    embedding_model VARCHAR(100),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Job sources (e.g., LinkedIn, Indeed, etc.)
CREATE TABLE job_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    icon_url VARCHAR(500),
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Jobs table - raw job data from various sources
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL,
    source_job_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_logo_url VARCHAR(500),
    company_website VARCHAR(500),
    location VARCHAR(255),
    salary_min NUMERIC(10, 2),
    salary_max NUMERIC(10, 2),
    salary_currency VARCHAR(10),
    job_description TEXT NOT NULL,
    skills_required TEXT,
    experience_required VARCHAR(255),
    job_type VARCHAR(50),
    posted_date TIMESTAMP,
    apply_url VARCHAR(500),
    company_size VARCHAR(100),
    industry VARCHAR(100),
    remote_option VARCHAR(50),
    raw_json JSONB,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_source FOREIGN KEY (source_id) REFERENCES job_sources(id) ON DELETE CASCADE,
    CONSTRAINT uq_source_job_id UNIQUE (source_id, source_job_id)
);

-- Job embeddings table
CREATE TABLE job_embeddings (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    embedding_vector BYTEA,
    embedding_model VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_embedding_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT uq_job_embedding UNIQUE (job_id)
);

-- Job matches - semantic similarity between resume and job
CREATE TABLE job_matches (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    match_percentage NUMERIC(5, 2),
    similarity_score NUMERIC(5, 4),
    missing_skills TEXT,
    strengths_alignment TEXT,
    ats_optimization_score NUMERIC(5, 2),
    interview_probability_score NUMERIC(5, 2),
    match_explanation TEXT,
    nonmatch_explanation TEXT,
    resume_improvements TEXT,
    recruiter_message TEXT,
    cover_letter TEXT,
    is_starred BOOLEAN DEFAULT FALSE,
    is_dismissed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_job_resume UNIQUE (user_id, job_id, resume_id)
);

-- Applications table - tracks user applications
CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    job_match_id BIGINT,
    application_status VARCHAR(50) NOT NULL,
    applied_date TIMESTAMP NOT NULL,
    follow_up_date TIMESTAMP,
    notes TEXT,
    custom_cover_letter TEXT,
    form_data_json JSONB,
    is_approved_by_user BOOLEAN DEFAULT FALSE,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_match FOREIGN KEY (job_match_id) REFERENCES job_matches(id) ON DELETE SET NULL
);

-- Application events - for tracking status changes
CREATE TABLE application_events (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_app FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);

-- Saved jobs - user's bookmarked jobs
CREATE TABLE saved_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saved_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_saved_job UNIQUE (user_id, job_id)
);

-- Analytics events - for tracking user interactions
CREATE TABLE analytics_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analytics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Notification preferences
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    email_daily_digest BOOLEAN DEFAULT TRUE,
    email_high_match_alerts BOOLEAN DEFAULT TRUE,
    email_application_updates BOOLEAN DEFAULT TRUE,
    email_new_matches BOOLEAN DEFAULT TRUE,
    digest_day_of_week VARCHAR(10),
    digest_time_of_day TIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_resumes_primary ON resumes(user_id, is_primary);
CREATE INDEX idx_jobs_source_id ON jobs(source_id);
CREATE INDEX idx_jobs_posted_date ON jobs(posted_date DESC);
CREATE INDEX idx_jobs_company ON jobs(company_name);
CREATE INDEX idx_job_embeddings_job_id ON job_embeddings(job_id);
CREATE INDEX idx_matches_user_id ON job_matches(user_id);
CREATE INDEX idx_matches_job_id ON job_matches(job_id);
CREATE INDEX idx_matches_resume_id ON job_matches(resume_id);
CREATE INDEX idx_matches_similarity ON job_matches(similarity_score DESC);
CREATE INDEX idx_matches_created ON job_matches(created_at DESC);
CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_job_id ON applications(job_id);
CREATE INDEX idx_applications_status ON applications(application_status);
CREATE INDEX idx_applications_date ON applications(applied_date DESC);
CREATE INDEX idx_app_events_app_id ON application_events(application_id);
CREATE INDEX idx_saved_user_job ON saved_jobs(user_id, job_id);
CREATE INDEX idx_analytics_user_id ON analytics_events(user_id);
CREATE INDEX idx_analytics_created ON analytics_events(created_at DESC);

