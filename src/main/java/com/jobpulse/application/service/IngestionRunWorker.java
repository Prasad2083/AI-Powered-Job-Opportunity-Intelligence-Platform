package com.jobpulse.application.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicReference;

@Component
@ConditionalOnProperty(name={"jobpulse.ingestion.worker-enabled","app.job-ingestion.enabled"},havingValue="true",matchIfMissing=true)
public class IngestionRunWorker {
    private final IngestionRunService runs;
    private final JobIngestionService ingestion;
    private final AtomicReference<Long> running=new AtomicReference<>();
    public IngestionRunWorker(IngestionRunService runs,JobIngestionService ingestion) { this.runs=runs;this.ingestion=ingestion; }

    @Scheduled(fixedDelayString="${job.ingest.delay:3600000}",scheduler="taskScheduler")
    public void requestScheduled() { runs.request(); }

    @Scheduled(fixedDelayString="${jobpulse.ingestion.poll-delay-ms:1000}",scheduler="ingestionScheduler")
    public void processNext() {
        var claimed=runs.claim();if(claimed.isEmpty()) return;
        Long id=claimed.get();running.set(id);
        try {
            JobIngestionService.IngestionReport report;
            try { report=ingestion.ingestJobs(); }
            catch(Exception ex) {
                runs.complete(id,null,"Refresh failed. Some updates may have completed; review listings and retry later.");return;
            }
            runs.complete(id,report,null);
        } finally { running.compareAndSet(id,null); }
    }

    @Scheduled(fixedDelay=30000,initialDelay=30000,scheduler="taskScheduler")
    public void heartbeat() { Long id=running.get();if(id!=null) runs.heartbeat(id); }
}
