package com.jobpulse.infrastructure.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

/** Row locking preserves retry counts/deadlines across processes and restarts. */
@Repository
public class JdbcFeedRetryStateStore implements FeedRetryStateStore {
    private final JdbcTemplate db;
    public JdbcFeedRetryStateStore(JdbcTemplate db) { this.db=db; }

    @Override public Optional<Failure> find(String source) {
        return db.query("SELECT attempts,retry_epoch_second,retry_nano FROM feed_retry_state WHERE source=?",
            (r,n)->new Failure(r.getInt(1),Instant.ofEpochSecond(r.getLong(2),r.getInt(3))),source).stream().findFirst();
    }

    @Override @Transactional
    public void recordFailure(String source,Instant now,Instant providerRetry) {
        db.update("INSERT INTO feed_retry_state(source,attempts,retry_epoch_second,retry_nano) VALUES (?,0,0,0) ON CONFLICT (source) DO NOTHING",source);
        Failure previous=db.queryForObject("SELECT attempts,retry_epoch_second,retry_nano FROM feed_retry_state WHERE source=? FOR UPDATE",
            (r,n)->new Failure(r.getInt(1),Instant.ofEpochSecond(r.getLong(2),r.getInt(3))),source);
        Failure next=FeedRetryPolicy.nextFailure(previous,now,providerRetry);
        db.update("UPDATE feed_retry_state SET attempts=?,retry_epoch_second=?,retry_nano=? WHERE source=?",
            next.attempts(),next.nextAttempt().getEpochSecond(),next.nextAttempt().getNano(),source);
    }

    @Override public void clear(String source) {
        db.update("DELETE FROM feed_retry_state WHERE source=?",source);
    }
}
