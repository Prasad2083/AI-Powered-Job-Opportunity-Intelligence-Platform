package com.jobpulse.infrastructure.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;

class FeedRetryPolicyTest {
    private RestClientResponseException response(int status,String retryAfter) {
        var headers=new HttpHeaders();headers.set(HttpHeaders.RETRY_AFTER,retryAfter);
        return new RestClientResponseException("Fixture error",status,"Fixture",headers,new byte[0],java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test void exponentialDelayIsBoundedAndSuccessfulResponseResetsIt() {
        var clock=new MutableFeedClock();var policy=new FeedRetryPolicy(clock);
        for(long seconds:new long[]{60,120,240,480,960,1920,3600,3600}) {
            policy.failed("a",new IllegalArgumentException("Malformed fixture"));
            assertTrue(policy.deferred("a").isPresent());assertTrue(policy.deferred("healthy").isEmpty());
            clock.advance(seconds-1);assertTrue(policy.deferred("a").isPresent());
            clock.advance(1);assertTrue(policy.deferred("a").isEmpty());
        }
        policy.succeeded("a");policy.failed("a",new IllegalArgumentException());
        clock.advance(60);assertTrue(policy.deferred("a").isEmpty());
    }

    @Test void honorsRetryAfterSecondsAndDatesWithoutTrustingMalformedOrPastDates() {
        var clock=new MutableFeedClock();var policy=new FeedRetryPolicy(clock);
        policy.failed("seconds",response(429,"600"));clock.advance(599);
        assertTrue(policy.deferred("seconds").isPresent());clock.advance(1);assertTrue(policy.deferred("seconds").isEmpty());
        String future=DateTimeFormatter.RFC_1123_DATE_TIME.format(clock.instant().plusSeconds(7200).atZone(ZoneOffset.UTC));
        policy.failed("date",response(503,future));clock.advance(7199);
        assertTrue(policy.deferred("date").isPresent());clock.advance(1);assertTrue(policy.deferred("date").isEmpty());
        for(String invalid:new String[]{"invalid","-1","Mon, 9 Nov 2020 12:00:00 GMT"}) {
            policy.failed(invalid,response(429,invalid));clock.advance(60);assertTrue(policy.deferred(invalid).isEmpty());
        }
    }

    @Test void overflowingProviderDelayDoesNotCauseAnEarlyRetryOrExposeProviderContent() {
        var clock=new MutableFeedClock();var policy=new FeedRetryPolicy(clock);
        policy.failed("limited",response(429,"99999999999999999999999999"));
        policy.failed("limited",new IllegalStateException("A concurrent failure must not shorten the provider delay"));
        clock.advance(86400);assertTrue(policy.deferred("limited").isPresent());
        assertFalse(policy.deferred("limited").orElseThrow().contains("Fixture error"));
    }
}
