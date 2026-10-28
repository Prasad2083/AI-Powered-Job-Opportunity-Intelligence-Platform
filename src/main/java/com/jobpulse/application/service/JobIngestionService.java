package com.jobpulse.application.service;

import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.entity.JobSource;
import com.jobpulse.domain.repository.JobRepository;
import com.jobpulse.domain.repository.JobSourceRepository;
import com.jobpulse.infrastructure.client.MockJobBoardClient;
import com.jobpulse.infrastructure.adapter.PublicJobFeedService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JobIngestionService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(JobIngestionService.class);
    private final MockJobBoardClient mock;
    private final PublicJobFeedService feeds;
    private final JobRepository jobs;
    private final JobSourceRepository sources;
    private final TransactionTemplate transaction;
    @Value("${jobpulse.feeds.include-mock:false}") private boolean includeMock;
    @Value("${jobpulse.feeds.minimum-refresh-ms:60000}") private long minimumRefreshMs;
    @Value("${app.job-ingestion.enabled:true}") private boolean enabled=true;
    private long lastAttempt;
    private IngestionReport lastReport;

    public JobIngestionService(MockJobBoardClient mock,JobRepository jobs,JobSourceRepository sources,PublicJobFeedService feeds,PlatformTransactionManager manager) {
        this.mock=mock;this.jobs=jobs;this.sources=sources;this.feeds=feeds;
        transaction=new TransactionTemplate(manager);transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public record IngestionReport(int fetched,int saved,int archived,int failed,Set<String> checkedSources,Map<String,String> sourceFailures,LocalDateTime checkedAt) {}

    @Scheduled(fixedDelayString="${job.ingest.delay:3600000}")
    public synchronized IngestionReport ingestJobs() {
        if(!enabled) return new IngestionReport(0,0,0,0,Set.of(),Map.of("ingestion","Disabled by configuration"),LocalDateTime.now());
        long now=System.currentTimeMillis();
        if(lastReport!=null && now-lastAttempt<minimumRefreshMs) return lastReport;
        lastAttempt=now;
        var batch=feeds.fetchReport();List<MockJobBoardClient.ExternalJob> entries=new ArrayList<>(batch.jobs());
        if(includeMock) entries.addAll(mock.fetchJobs());
        int saved=0,failed=0,archived=0;Set<String> failedSources=new HashSet<>();
        Map<String,Set<String>> seen=new HashMap<>();
        for(var external:entries) {
            seen.computeIfAbsent(external.source,key->new HashSet<>()).add(external.externalId);
            try {
                transaction.executeWithoutResult(status->{
                    JobSource source=sources.findByName(external.source).orElseGet(()->sources.save(JobSource.builder().name(external.source).isEnabled(true).build()));
                    Job job=jobs.findBySource_NameAndSourceJobId(external.source,external.externalId).orElseGet(Job::new);
                    job.setSource(source);job.setSourceJobId(external.externalId);job.setTitle(external.title);job.setCompanyName(external.company);
                    job.setLocation(external.location);job.setJobDescription(external.description);job.setApplyUrl(external.url);
                    if(external.postedAt!=null) job.setPostedDate(LocalDateTime.ofInstant(external.postedAt,java.time.ZoneOffset.UTC));
                    job.setIsArchived(false);job.setLastSeenAt(LocalDateTime.now());
                    if(job.getApplied()==null) job.setApplied(false);
                    jobs.saveAndFlush(job);
                });saved++;
            } catch(Exception ex) {failed++;failedSources.add(external.source);log.warn("Unable to store provider job {}: {}",external.externalId,ex.getClass().getSimpleName());}
        }
        // Absence means closure only after a complete successful provider response.
        for(String source:batch.successfulSources()) {
            if(failedSources.contains(source)) continue;
            try {
                Integer count=transaction.execute(status->{
                    int changed=0;
                    for(Job job:jobs.findBySource_NameAndIsArchivedFalse(source)) {
                        if(!seen.getOrDefault(source,Set.of()).contains(job.getSourceJobId())) {job.setIsArchived(true);jobs.save(job);changed++;}
                    }
                    return changed;
                });archived+=count==null?0:count;
            } catch(Exception ex) {failed++;log.warn("Unable to archive missing jobs for {}",source);}
        }
        lastReport=new IngestionReport(entries.size(),saved,archived,failed,batch.successfulSources(),batch.failures(),LocalDateTime.now());
        return lastReport;
    }
}
