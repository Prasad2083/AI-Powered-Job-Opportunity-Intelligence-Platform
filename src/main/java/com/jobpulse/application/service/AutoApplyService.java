package com.jobpulse.application.service;

import org.springframework.stereotype.Service;

/**
 * Explicitly unavailable until an authorized job-board submission adapter,
 * per-user application consent, and provider receipt verification exist.
 * Never report a simulated application as submitted.
 */
@Service
public class AutoApplyService {
    public void autoApplyPendingJobs() {
        throw new UnsupportedOperationException(
            "Automatic job application is not configured. Use the official provider apply URL.");
    }
}
