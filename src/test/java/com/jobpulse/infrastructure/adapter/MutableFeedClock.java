package com.jobpulse.infrastructure.adapter;

import java.time.*;

final class MutableFeedClock extends Clock {
    private Instant now=Instant.parse("2026-11-09T12:00:00Z");
    void advance(long seconds) { now=now.plusSeconds(seconds); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now,zone); }
    @Override public Instant instant() { return now; }
}
