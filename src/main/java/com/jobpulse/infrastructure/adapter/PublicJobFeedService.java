package com.jobpulse.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.infrastructure.client.MockJobBoardClient.ExternalJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.Instant;
import java.util.*;

@Service
public class PublicJobFeedService {
    private final RestClient client = RestClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();
    @Value("${jobpulse.feeds.greenhouse-boards:}") private String greenhouseBoards;
    @Value("${jobpulse.feeds.lever-sites:}") private String leverSites;

    public List<ExternalJob> fetch() {
        List<ExternalJob> jobs = new ArrayList<>();
        for (String board : greenhouseBoards.split(",")) {
            String id = board.trim();
            if (id.isEmpty() || !id.matches("[A-Za-z0-9_-]+")) continue;
            try {
                JsonNode result = client.get().uri("https://boards-api.greenhouse.io/v1/boards/" + id + "/jobs?content=true").retrieve().body(JsonNode.class);
                if (result == null) continue;
                for (JsonNode item : result.path("jobs")) {
                    ExternalJob job = new ExternalJob();
                    job.externalId = id + "-" + item.path("id").asText();
                    job.title = item.path("title").asText();
                    job.company = id;
                    job.location = item.path("location").path("name").asText("");
                    job.description = item.path("content").asText("");
                    job.url = item.path("absolute_url").asText();
                    job.source = "GREENHOUSE:" + id;
                    job.postedAt = Instant.now();
                    if (job.url.startsWith("https://")) jobs.add(job);
                }
            } catch (Exception ignored) { /* one unavailable provider must not block other feeds */ }
        }
        for (String site : leverSites.split(",")) {
            String id = site.trim();
            if (id.isEmpty() || !id.matches("[A-Za-z0-9_-]+")) continue;
            try {
                JsonNode result = client.get().uri("https://api.lever.co/v0/postings/" + id + "?mode=json").retrieve().body(JsonNode.class);
                if (result == null) continue;
                for (JsonNode item : result) {
                    ExternalJob job = new ExternalJob();
                    job.externalId = id + "-" + item.path("id").asText();
                    job.title = item.path("text").asText();
                    job.company = id;
                    job.location = item.path("categories").path("location").asText("");
                    job.description = item.path("descriptionPlain").asText("");
                    job.url = item.path("hostedUrl").asText();
                    job.source = "LEVER:" + id;
                    job.postedAt = Instant.now();
                    if (job.url.startsWith("https://")) jobs.add(job);
                }
            } catch (Exception ignored) { /* log and metrics should be added before production */ }
        }
        return jobs;
    }
}
