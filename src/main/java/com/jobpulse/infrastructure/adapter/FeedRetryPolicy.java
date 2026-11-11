package com.jobpulse.infrastructure.adapter;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Non-blocking, per-employer retry deferral with an injectable state store. */
final class FeedRetryPolicy {
    private final FeedRetryStateStore state;
    private final Clock clock;
    FeedRetryPolicy(Clock clock) { this(clock,new MemoryStore()); }
    FeedRetryPolicy(Clock clock,FeedRetryStateStore state) { this.clock=clock;this.state=state; }

    Optional<String> deferred(String source) {
        var failure=state.find(source).orElse(null);
        if(failure==null || !clock.instant().isBefore(failure.nextAttempt())) return Optional.empty();
        return Optional.of("Retry deferred until "+failure.nextAttempt()+"; previous listings retained");
    }

    void succeeded(String source) { state.clear(source); }

    void failed(String source,Exception cause) {
        Instant now=clock.instant();
        Instant provider=null;
        if(cause instanceof RestClientResponseException response
            && (response.getStatusCode().value()==429 || response.getStatusCode().value()==503)
            && response.getResponseHeaders()!=null)
            provider=retryAfter(response.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER),now);
        state.recordFailure(source,now,provider);
    }

    static FeedRetryStateStore.Failure nextFailure(FeedRetryStateStore.Failure previous,Instant now,Instant provider) {
        int attempts=previous==null?1:Math.min(7,previous.attempts()+1);
        Instant next=now.plusSeconds(Math.min(3600,60L << (attempts-1)));
        if(provider!=null && provider.isAfter(next)) next=provider;
        if(previous!=null && previous.nextAttempt().isAfter(next)) next=previous.nextAttempt();
        return new FeedRetryStateStore.Failure(attempts,next);
    }

    /** Used only by deterministic isolated feed tests; application wiring uses PostgreSQL. */
    private static final class MemoryStore implements FeedRetryStateStore {
        private final ConcurrentHashMap<String,Failure> failures=new ConcurrentHashMap<>();
        @Override public Optional<Failure> find(String source) { return Optional.ofNullable(failures.get(source)); }
        @Override public void recordFailure(String source,Instant now,Instant provider) {
            failures.compute(source,(key,previous)->nextFailure(previous,now,provider));
        }
        @Override public void clear(String source) { failures.remove(source); }
    }

    private static Instant retryAfter(String value,Instant now) {
        if(value==null || value.isBlank()) return null;
        String header=value.trim();
        if(header.matches("[0-9]+")) {
            try { return now.plusSeconds(Long.parseLong(header)); }
            catch(java.time.DateTimeException | ArithmeticException | NumberFormatException ex) { return Instant.MAX; }
        }
        try { return ZonedDateTime.parse(header,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); }
        catch(java.time.format.DateTimeParseException ex) { return null; }
    }
}
