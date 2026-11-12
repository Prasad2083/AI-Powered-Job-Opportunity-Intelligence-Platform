package com.jobpulse.application.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** One scheduled worker plus a database-wide running slot bounds local model use. */
@Component
@ConditionalOnProperty(name="jobpulse.tailoring.worker-enabled",havingValue="true",matchIfMissing=true)
public class TailoringTaskWorker {
    private final TailoringTaskService tasks;
    private final ResumeTailoringService model;
    public TailoringTaskWorker(TailoringTaskService tasks,ResumeTailoringService model) {
        this.tasks=tasks;this.model=model;
    }
    @Scheduled(fixedDelayString="${jobpulse.tailoring.poll-delay-ms:1000}",scheduler="tailoringScheduler")
    public void processNext() {
        var claimed=tasks.claim();
        if(claimed.isEmpty()) return;
        var task=claimed.get();
        String draft=null,error=null;
        try {
            draft=model.tailor(task.original(),task.description());
            if(draft.length()>100000) {
                draft=null;
                error="The generated draft exceeds the editor limit. Shorten the source text and start a new draft.";
            }
        } catch(Exception ex) {
            // Do not persist arbitrary provider responses, resume text or exception details.
            error="Local AI unavailable or returned invalid source references. Check Ollama and the configured model, then start a new draft.";
        }
        tasks.complete(task.id(),draft,error);
    }
}
