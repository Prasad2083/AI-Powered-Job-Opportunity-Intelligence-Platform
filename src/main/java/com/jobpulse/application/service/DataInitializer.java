package com.jobpulse.application.service;

import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.repository.ResumeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Seed initial data for local development: a sample active resume.
 */
@Component
public class DataInitializer {

    private final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final ResumeRepository resumeRepository;

    public DataInitializer(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    @PostConstruct
    public void init() {
        if (resumeRepository.findFirstByActiveTrue().isEmpty()) {
            Resume r = new Resume();
            r.setUserId(1L);
            r.setFileName("default_resume.txt");
            r.setContentText("Experienced software engineer with Java and Spring Boot");
            r.setIsPrimary(true);
            resumeRepository.save(r);
            log.info("Seeded default resume id={}", r.getId());
        }
    }
}

