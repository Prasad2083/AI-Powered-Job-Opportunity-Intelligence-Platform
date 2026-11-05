package com.jobpulse.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;

class OllamaClientTest {
    @Test void returnsDraftFromLocalOllamaProtocol() throws Exception {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:11434/api/chat"))
            .andExpect(jsonPath("$.stream").value(false)).andExpect(jsonPath("$.model").value("qwen3:4b"))
            .andRespond(withSuccess(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("message",java.util.Map.of("content","{\"highlightLineNumbers\":[1]}"))),MediaType.APPLICATION_JSON));
        var service=new ResumeTailoringService(builder.build(),"http://localhost:11434","qwen3:4b");
        assertTrue(service.tailor("Java engineer","Java role").endsWith("COMPLETE ORIGINAL RESUME\nJava engineer"));server.verify();
    }
    @Test void rejectsEmptyModelResponse() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:11434/api/chat")).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new ResumeTailoringService(builder.build(),"http://localhost:11434","qwen3:4b").tailor("Java engineer","Java role"));
    }

    @Test void rejectsInventedEvidenceReferences() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:11434/api/chat"))
            .andRespond(withSuccess("{\"message\":{\"content\":\"{\\\"highlightLineNumbers\\\":[999]}\"}}",MediaType.APPLICATION_JSON));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new ResumeTailoringService(builder.build(),"http://localhost:11434","qwen3:4b").tailor("Java engineer","Java role"));
    }
}
