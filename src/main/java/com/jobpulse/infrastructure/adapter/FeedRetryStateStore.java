package com.jobpulse.infrastructure.adapter;

import java.time.Instant;
import java.util.Optional;

public interface FeedRetryStateStore {
    record Failure(int attempts, Instant nextAttempt) {}
    Optional<Failure> find(String source);
    void recordFailure(String source,Instant now,Instant providerRetry);
    void clear(String source);
}
