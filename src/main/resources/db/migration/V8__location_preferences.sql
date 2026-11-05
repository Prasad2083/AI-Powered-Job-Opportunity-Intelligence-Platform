-- Preserve company_target for its original purpose; locations have their own field.
ALTER TABLE users ADD COLUMN target_locations VARCHAR(255);
