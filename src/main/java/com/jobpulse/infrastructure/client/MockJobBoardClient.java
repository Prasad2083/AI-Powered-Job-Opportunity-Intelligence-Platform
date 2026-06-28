package com.jobpulse.infrastructure.client;

import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.entity.Resume;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mock client that simulates fetching jobs from external job boards
 * and applying to them. Replace with real adapters in production.
 */
@Component
public class MockJobBoardClient {

    public static class ExternalJob {
        public String externalId;
        public String title;
        public String company;
        public String location;
        public String description;
        public String url;
        public Instant postedAt;
        public String source;
    }

    public List<ExternalJob> fetchJobs() {
        List<ExternalJob> list = new ArrayList<>();

        ExternalJob j1 = new ExternalJob();
        j1.externalId = "mock-" + UUID.randomUUID();
        j1.title = "Software Engineer - Backend";
        j1.company = "Acme Corp";
        j1.location = "Remote";
        j1.description = "Work on backend services";
        j1.url = "https://jobs.acme.example/1";
        j1.postedAt = Instant.now().minusSeconds(3600);
        j1.source = "MOCK";
        list.add(j1);

        ExternalJob j2 = new ExternalJob();
        j2.externalId = "mock-" + UUID.randomUUID();
        j2.title = "Frontend Engineer";
        j2.company = "Beta LLC";
        j2.location = "New York, NY";
        j2.description = "React + TypeScript";
        j2.url = "https://jobs.beta.example/2";
        j2.postedAt = Instant.now().minusSeconds(7200);
        j2.source = "MOCK";
        list.add(j2);

        return list;
    }

    /**
     * Simulate applying to a job. Returns true if simulated success.
     */
    public boolean apply(Job job, Resume resume) {
        // Simulate network latency and success
        return true;
    }
}

