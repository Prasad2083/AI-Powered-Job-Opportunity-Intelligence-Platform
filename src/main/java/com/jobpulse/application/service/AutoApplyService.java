package com.jobpulse.application.service;

import com.jobpulse.domain.entity.ApplicationRecord;
import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.repository.ApplicationRecordRepository;
import com.jobpulse.domain.repository.JobRepository;
import com.jobpulse.domain.repository.ResumeRepository;
import com.jobpulse.infrastructure.client.MockJobBoardClient;
import com.jobpulse.common.util.TextMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutoApplyService {

    private final Logger log = LoggerFactory.getLogger(AutoApplyService.class);
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationRecordRepository applicationRecordRepository;
    private final MockJobBoardClient client;

    @Value("${job.match.threshold:0.05}")
    private double matchThreshold;

    public AutoApplyService(JobRepository jobRepository,
                            ResumeRepository resumeRepository,
                            ApplicationRecordRepository applicationRecordRepository,
                            MockJobBoardClient client) {
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.applicationRecordRepository = applicationRecordRepository;
        this.client = client;
    }

    // Run every 10 minutes by default
    @Scheduled(fixedDelayString = "${job.autoapply.delay:600000}")
    @Transactional
    public void autoApplyPendingJobs() {
        log.info("Starting auto-apply run");
        List<Job> pending = jobRepository.findByAppliedFalse();
        if (pending.isEmpty()) {
            log.info("No pending jobs to apply");
            return;
        }

        Resume resume = resumeRepository.findFirstByActiveTrue().orElse(null);
        if (resume == null) {
            log.warn("No active resume found; skipping apply");
            return;
        }

        for (Job job : pending) {
            try {
                String jobText = (job.getTitle() == null ? "" : job.getTitle() + " ")
                        + (job.getCompanyName() == null ? "" : job.getCompanyName() + " ")
                        + (job.getJobDescription() == null ? "" : job.getJobDescription());
                double score = TextMatcher.score(jobText, resume.getContentText());
                log.debug("Matching job {} score={}", job.getId(), score);

                if (score >= matchThreshold) {
                    boolean ok = client.apply(job, resume);

                    ApplicationRecord ar = ApplicationRecord.builder()
                            .job(job)
                            .applicationStatus(ok ? "APPLIED" : "FAILED")
                            .appliedDate(LocalDateTime.now())
                            .notes(ok
                                    ? String.format("Auto-applied (score=%.4f)", score)
                                    : "Apply attempt failed")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    applicationRecordRepository.save(ar);

                    if (ok) {
                        job.setApplied(true);
                        jobRepository.save(job);
                        log.info("Auto-applied to job {} (score={})", job.getId(), score);
                    } else {
                        log.warn("Apply attempt failed for job {}", job.getId());
                    }
                } else {
                    log.debug("Skipping job {} due to low score {}", job.getId(), score);
                }
            } catch (Exception e) {
                log.error("Error applying to job {}: {}", job.getId(), e.getMessage(), e);
            }
        }
    }
}