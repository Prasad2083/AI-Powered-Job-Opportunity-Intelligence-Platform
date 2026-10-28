package com.jobpulse.application.service;

import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.entity.JobSource;
import com.jobpulse.domain.repository.JobRepository;
import com.jobpulse.domain.repository.JobSourceRepository;
import com.jobpulse.infrastructure.client.MockJobBoardClient;
import com.jobpulse.infrastructure.adapter.PublicJobFeedService;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobIngestionService {

    private final Logger log = LoggerFactory.getLogger(JobIngestionService.class);
    private final MockJobBoardClient client;
    private final PublicJobFeedService realFeeds;
    @Value("${jobpulse.feeds.include-mock:false}") private boolean includeMock;
    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;

    public JobIngestionService(MockJobBoardClient client, JobRepository jobRepository, JobSourceRepository jobSourceRepository, PublicJobFeedService realFeeds) {
        this.client = client;
        this.realFeeds = realFeeds;
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
    }

    // Run every hour by default; can be overridden with property
    @Scheduled(fixedDelayString = "${job.ingest.delay:3600000}")
    @Transactional
    public void ingestJobs() {
        log.info("Starting job ingestion");
        List<MockJobBoardClient.ExternalJob> externalJobs = new ArrayList<>(realFeeds.fetch());
        if (includeMock) externalJobs.addAll(client.fetchJobs());

        for (MockJobBoardClient.ExternalJob ej : externalJobs) {
            jobRepository.findBySourceJobId(ej.externalId).ifPresentOrElse(
                    existing -> log.debug("Skipping existing job {}", ej.externalId),
                    () -> {
                        // Get or create job source
                        JobSource jobSource = jobSourceRepository.findByName(ej.source)
                                .orElseGet(() -> {
                                    JobSource newSource = JobSource.builder()
                                            .name(ej.source)
                                            .isEnabled(true)
                                            .build();
                                    return jobSourceRepository.save(newSource);
                                });

                        Job job = Job.builder()
                                .source(jobSource)
                                .sourceJobId(ej.externalId)
                                .title(ej.title)
                                .companyName(ej.company)
                                .location(ej.location)
                                .jobDescription(ej.description)
                                .applyUrl(ej.url)
                                .postedDate(LocalDateTime.now())
                                .applied(false)
                                .isArchived(false)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        jobRepository.save(job);
                        log.info("Ingested job {} - {}", ej.externalId, ej.title);
                    }
            );
        }
    }
}