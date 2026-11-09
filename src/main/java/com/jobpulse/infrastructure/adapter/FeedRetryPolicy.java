package com.jobpulse.infrastructure.adapter;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Non-blocking, per-employer retry deferral. In-memory state resets on restart. */
final class FeedRetryPolicy {
    private record Failure(int attempts, Instant nextAttempt) {}
    private final ConcurrentHashMap<String,Failure> failures=new ConcurrentHashMap<>();
    private final Clock clock;
    FeedRetryPolicy(Clock clock) { this.clock=clock; }

    Optional<String> deferred(String source) {
        var failure=failures.get(source);
        if(failure==null || !clock.instant().isBefore(failure.nextAttempt())) return Optional.empty();
        return Optional.of("Retry deferred until "+failure.nextAttempt()+"; previous listings retained");
    }

    void succeeded(String source) { failures.remove(source); }

    void failed(String source,Exception cause) {
        Instant now=clock.instant();
        failures.compute(source,(key,previous)-> {
            int attempts=previous==null?1:Math.min(7,previous.attempts()+1);
            Instant next=now.plusSeconds(Math.min(3600,60L << (attempts-1)));
            if(cause instanceof RestClientResponseException response
                && (response.getStatusCode().value()==429 || response.getStatusCode().value()==503)
                && response.getResponseHeaders()!=null) {
                Instant provider=retryAfter(response.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER),now);
                if(provider!=null && provider.isAfter(next)) next=provider;
            }
            if(previous!=null && previous.nextAttempt().isAfter(next)) next=previous.nextAttempt();
            return new Failure(attempts,next);
        });
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
