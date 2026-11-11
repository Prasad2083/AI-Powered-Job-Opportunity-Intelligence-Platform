package com.jobpulse.application.service;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;

class IngestionRunWorkerTest {
    @Test void idlePollDoesNotContactProviders() {
        var runs=mock(IngestionRunService.class);var ingestion=mock(JobIngestionService.class);
        when(runs.claim()).thenReturn(Optional.empty());
        new IngestionRunWorker(runs,ingestion).processNext();verifyNoInteractions(ingestion);
    }
    @Test void heartbeatTracksLargeIdsAndStopsAfterCompletion() {
        var runs=mock(IngestionRunService.class);var ingestion=mock(JobIngestionService.class);
        var worker=new IngestionRunWorker(runs,ingestion);
        when(runs.claim()).thenReturn(Optional.of(1024L));
        var report=new JobIngestionService.IngestionReport(0,0,0,0,Set.of(),Map.of(),java.time.LocalDateTime.now());
        when(ingestion.ingestJobs()).thenAnswer(call->{worker.heartbeat();return report;});
        worker.processNext();worker.heartbeat();
        verify(runs,times(1)).heartbeat(1024L);verify(runs).complete(1024L,report,null);
    }
    @Test void unexpectedProviderFailureIsRecordedWithoutLeakingException() {
        var runs=mock(IngestionRunService.class);var ingestion=mock(JobIngestionService.class);
        var worker=new IngestionRunWorker(runs,ingestion);
        when(runs.claim()).thenReturn(Optional.of(1L));
        when(ingestion.ingestJobs()).thenThrow(new IllegalStateException("private internal connection details"));
        worker.processNext();worker.heartbeat();
        verify(runs).complete(eq(1L),isNull(),argThat(message->message.contains("Some updates may have completed")&&!message.contains("private")));
        verify(runs,never()).heartbeat(anyLong());
    }
}
