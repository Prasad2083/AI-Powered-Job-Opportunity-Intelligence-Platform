-- Public feed operational state only; no resume, user or application payloads.
CREATE TABLE feed_retry_state (
    source VARCHAR(100) PRIMARY KEY,
    attempts INTEGER NOT NULL CHECK (attempts BETWEEN 0 AND 7),
    -- Epoch seconds preserve even an effectively unbounded Retry-After safely.
    retry_epoch_second BIGINT NOT NULL,
    retry_nano INTEGER NOT NULL CHECK (retry_nano BETWEEN 0 AND 999999999)
);
