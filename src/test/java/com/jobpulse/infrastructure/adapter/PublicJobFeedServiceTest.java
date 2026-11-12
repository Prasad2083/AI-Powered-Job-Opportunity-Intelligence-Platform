package com.jobpulse.infrastructure.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;

class PublicJobFeedServiceTest {
    @Test void failedEmployerDoesNotBlockOtherFeedsAndLeverIncludesRequirements() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://boards-api.greenhouse.io/v1/boards/example/jobs?content=true"))
            .andRespond(withServerError());
        server.expect(requestTo("https://api.lever.co/v0/postings/example?mode=json&limit=100&skip=0"))
            .andRespond(withSuccess("""
                [{"id":"abc","text":"SDET","categories":{"location":"Toronto"},"descriptionPlain":"Quality role",
                "lists":[{"text":"Requirements","content":"Java Selenium"}],"hostedUrl":"https://jobs.lever.co/example/abc"}]
                """,MediaType.APPLICATION_JSON));
        var jobs=new PublicJobFeedService(builder.build(),"example,invalid/slug","example").fetch();
        assertEquals(1,jobs.size());assertTrue(jobs.getFirst().description.contains("Selenium"));
        assertNull(jobs.getFirst().postedAt);assertEquals("LEVER:example",jobs.getFirst().source);server.verify();
    }

    @Test void leverReadsEveryPageAndKeepsRealPublicationTime() throws Exception {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var first=new java.util.ArrayList<java.util.Map<String,Object>>();
        for(int i=0;i<100;i++) first.add(java.util.Map.of("id",String.valueOf(i),"text","SDET","hostedUrl","https://jobs.lever.co/example/"+i,"createdAt",1700000000000L));
        server.expect(requestTo("https://api.lever.co/v0/postings/example?mode=json&limit=100&skip=0"))
            .andRespond(withSuccess(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(first),MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.lever.co/v0/postings/example?mode=json&limit=100&skip=100"))
            .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        var result=new PublicJobFeedService(builder.build(),"","example").fetchReport();
        assertEquals(100,result.jobs().size());assertEquals(java.util.Set.of("LEVER:example"),result.successfulSources());
        assertEquals(java.time.Instant.ofEpochMilli(1700000000000L),result.jobs().getFirst().postedAt);server.verify();
    }

    @Test void rateLimitedEmployerIsDeferredWhileHealthyFeedsKeepRefreshing() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        String greenhouse="https://boards-api.greenhouse.io/v1/boards/example/jobs?content=true";
        String lever="https://api.lever.co/v0/postings/example?mode=json&limit=100&skip=0";
        server.expect(requestTo(greenhouse)).andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS).header("Retry-After","600"));
        server.expect(requestTo(lever)).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        server.expect(requestTo(lever)).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        server.expect(requestTo(greenhouse)).andRespond(withSuccess("{\"jobs\":[]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo(lever)).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        var clock=new MutableFeedClock();
        var feeds=new PublicJobFeedService(builder.build(),"example","example",new FeedRetryPolicy(clock));
        var first=feeds.fetchReport();assertEquals(java.util.Set.of("LEVER:example"),first.successfulSources());
        clock.advance(60);
        var deferred=feeds.fetchReport();assertEquals(java.util.Set.of("LEVER:example"),deferred.successfulSources());
        assertTrue(deferred.failures().get("GREENHOUSE:example").contains("Retry deferred until"));
        clock.advance(540);
        var recovered=feeds.fetchReport();assertEquals(java.util.Set.of("LEVER:example","GREENHOUSE:example"),recovered.successfulSources());
        assertTrue(recovered.failures().isEmpty());server.verify();
    }

    @Test void failedLaterLeverPageNeverProducesPartialSuccessOrImmediateRepeatedRequests() throws Exception {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var page=new java.util.ArrayList<java.util.Map<String,Object>>();
        for(int i=0;i<100;i++) page.add(java.util.Map.of("id",String.valueOf(i),"text","SDET","hostedUrl","https://jobs.lever.co/example/"+i));
        server.expect(requestTo("https://api.lever.co/v0/postings/example?mode=json&limit=100&skip=0"))
            .andRespond(withSuccess(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(page),MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.lever.co/v0/postings/example?mode=json&limit=100&skip=100"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        var feeds=new PublicJobFeedService(builder.build(),"","example",new FeedRetryPolicy(new MutableFeedClock()));
        var failed=feeds.fetchReport();assertTrue(failed.jobs().isEmpty());assertTrue(failed.successfulSources().isEmpty());
        assertTrue(feeds.fetchReport().successfulSources().isEmpty());server.verify();
    }

    @Test void invalidEmployerSlugsNeverReachHttpOrPersistentState() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var store=new FeedRetryStateStore() {
            public java.util.Optional<Failure> find(String source) { throw new AssertionError("Invalid slug reached state lookup"); }
            public void recordFailure(String source,java.time.Instant now,java.time.Instant provider) { throw new AssertionError("Invalid slug reached state write"); }
            public void clear(String source) { throw new AssertionError("Invalid slug reached state reset"); }
        };
        var feed=new PublicJobFeedService(builder.build(),"invalid/slug,"+"x".repeat(101),"../invalid",new FeedRetryPolicy(new MutableFeedClock(),store));
        var report=feed.fetchReport();
        assertEquals(3,report.failures().size());assertTrue(report.successfulSources().isEmpty());
        server.verify();
    }

    @Test void failedRetryStateResetDoesNotMarkTheFeedSuccessfullyChecked() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://boards-api.greenhouse.io/v1/boards/example/jobs?content=true"))
            .andRespond(withSuccess("{\"jobs\":[]}",MediaType.APPLICATION_JSON));
        var store=new FeedRetryStateStore() {
            public java.util.Optional<Failure> find(String source) { return java.util.Optional.empty(); }
            public void recordFailure(String source,java.time.Instant now,java.time.Instant provider) {}
            public void clear(String source) { throw new IllegalStateException("Fixture state reset failure"); }
        };
        var feed=new PublicJobFeedService(builder.build(),"example","",new FeedRetryPolicy(new MutableFeedClock(),store));
        var report=feed.fetchReport();assertTrue(report.successfulSources().isEmpty());
        assertTrue(report.failures().containsKey("GREENHOUSE:example"));server.verify();
    }
}
