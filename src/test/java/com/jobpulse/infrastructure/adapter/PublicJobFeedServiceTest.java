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
}
