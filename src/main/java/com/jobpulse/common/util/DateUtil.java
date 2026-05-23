package com.jobpulse.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Date and time utility functions for consistent date handling.
 */
public class DateUtil {
    private static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * Get current instant in UTC
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Check if a timestamp is within the last 24 hours
     */
    public static boolean isWithin24Hours(Instant timestamp) {
        return timestamp != null && ChronoUnit.HOURS.between(timestamp, now()) <= 24;
    }

    /**
     * Check if a timestamp is within specified hours
     */
    public static boolean isWithinHours(Instant timestamp, long hours) {
        return timestamp != null && ChronoUnit.HOURS.between(timestamp, now()) <= hours;
    }

    /**
     * Get local date from instant
     */
    public static LocalDate getLocalDate(Instant instant) {
        return instant == null ? null : instant.atZone(UTC).toLocalDate();
    }

    /**
     * Add days to instant
     */
    public static Instant addDays(Instant instant, int days) {
        return instant == null ? null : instant.plus(days, ChronoUnit.DAYS);
    }

    /**
     * Add hours to instant
     */
    public static Instant addHours(Instant instant, int hours) {
        return instant == null ? null : instant.plus(hours, ChronoUnit.HOURS);
    }
}

