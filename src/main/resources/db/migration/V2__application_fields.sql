-- Add fields needed by the implemented application entities without modifying historical V1.
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(32) NOT NULL DEFAULT 'USER';
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS applied BOOLEAN NOT NULL DEFAULT FALSE;
-- Keep migration idempotent for pre-existing development DBs; verify Hibernate validate in CI.
