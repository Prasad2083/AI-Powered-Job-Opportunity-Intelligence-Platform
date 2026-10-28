package com.jobpulse.application.service;

import com.jobpulse.domain.entity.*;
import com.jobpulse.domain.repository.*;
import com.jobpulse.infrastructure.adapter.PublicJobFeedService;
import com.jobpulse.infrastructure.client.MockJobBoardClient;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class JobIngestionServiceTest {
    @Test void deduplicatesWithinProviderAndArchivesOnlyAfterCompleteSuccessfulRefresh() {
        var jobs=mock(JobRepository.class);var sources=mock(JobSourceRepository.class);var feeds=mock(PublicJobFeedService.class);
        var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(call->new SimpleTransactionStatus());
        var service=new JobIngestionService(mock(MockJobBoardClient.class),jobs,sources,feeds,manager);
        ReflectionTestUtils.setField(service,"minimumRefreshMs",0L);
        var source=JobSource.builder().id(1L).name("GREENHOUSE:example").isEnabled(true).build();
        when(sources.findByName(source.getName())).thenReturn(Optional.of(source));
        var existing=Job.builder().id(1L).source(source).sourceJobId("example-1").title("Old title").isArchived(false).build();
        when(jobs.findBySource_NameAndSourceJobId(source.getName(),"example-1")).thenReturn(Optional.of(existing));
        var external=new MockJobBoardClient.ExternalJob();external.source=source.getName();external.externalId="example-1";external.title="SDET";external.company="Example";external.description="Java";external.url="https://example.com/jobs/1";
        when(feeds.fetchReport()).thenReturn(new PublicJobFeedService.FeedBatch(List.of(external),Set.of(source.getName()),Map.of()));
        assertEquals(1,service.ingestJobs().saved());assertEquals("SDET",existing.getTitle());assertNotNull(existing.getLastSeenAt());
        verify(jobs).findBySource_NameAndSourceJobId(source.getName(),"example-1");
        when(jobs.findBySource_NameAndIsArchivedFalse(source.getName())).thenReturn(List.of(existing));
        when(feeds.fetchReport()).thenReturn(new PublicJobFeedService.FeedBatch(List.of(),Set.of(),Map.of(source.getName(),"Unavailable")));
        assertEquals(0,service.ingestJobs().archived());assertFalse(existing.getIsArchived());
        when(feeds.fetchReport()).thenReturn(new PublicJobFeedService.FeedBatch(List.of(),Set.of(source.getName()),Map.of()));
        assertEquals(1,service.ingestJobs().archived());assertTrue(existing.getIsArchived());
    }
}
