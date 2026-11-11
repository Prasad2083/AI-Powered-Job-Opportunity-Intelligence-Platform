package com.jobpulse.application.controller;

import com.jobpulse.application.service.DocumentExportService;
import com.jobpulse.application.service.TailoringTaskService;
import com.jobpulse.domain.entity.*;
import com.jobpulse.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TailoringControllerTest {
    final ResumeRepository resumes=mock(ResumeRepository.class);
    final JobRepository jobs=mock(JobRepository.class);
    final UserRepository users=mock(UserRepository.class);
    final TailoredDocumentRepository documents=mock(TailoredDocumentRepository.class);
    final TailoringTaskService tasks=mock(TailoringTaskService.class);
    final Authentication auth=new UsernamePasswordAuthenticationToken("alice",null);
    final TailoringController controller=new TailoringController(tasks,resumes,jobs,users,documents,mock(DocumentExportService.class));
    final Resume resume=new Resume();

    @BeforeEach void setup() {
        var user=new User();user.setId(1L);user.setUsername("alice");
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        resume.setId(2L);resume.setUserId(1L);resume.setContentText("Current editable source text");
        when(resumes.findById(2L)).thenReturn(Optional.of(resume));
        when(documents.save(any())).thenAnswer(call->call.getArgument(0));
    }

    TailoringController.SaveRequest input(Long taskId,Long documentId) {
        return new TailoringController.SaveRequest(2L,3L,"Edited fixture draft",false,taskId,documentId);
    }

    @Test void completedOwnedTaskCanBeSavedAfterListingArchival() {
        when(tasks.originalForSave(1L,7L,2L,3L)).thenReturn("Original at generation time");
        var saved=controller.save(input(7L,null),auth);
        assertEquals("Original at generation time",saved.getOriginalSnapshot());
        assertEquals("Edited fixture draft",saved.getContent());
        assertEquals(1L,saved.getUserId());assertFalse(saved.isApproved());
        assertEquals("Current editable source text",resume.getContentText());
        verifyNoInteractions(jobs);
    }

    @Test void ownedSavedVersionKeepsItsSnapshotAfterListingArchival() {
        var source=new TailoredDocument();source.setId(8L);source.setUserId(1L);
        source.setResumeId(2L);source.setJobId(3L);source.setOriginalSnapshot("Original from saved version");
        when(documents.findByIdAndUserId(8L,1L)).thenReturn(Optional.of(source));
        var saved=controller.save(input(null,8L),auth);
        assertEquals("Original from saved version",saved.getOriginalSnapshot());
        assertEquals("Current editable source text",resume.getContentText());
        verifyNoInteractions(jobs,tasks);
    }

    @Test void unanchoredDraftStillRequiresVisibleJob() {
        when(jobs.findVisibleById(3L,1L)).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.save(input(null,null),auth)).getStatusCode().value());
        verify(documents,never()).save(any());
    }

    @Test void visibleJobStillAllowsAnUnanchoredDraftUsingCurrentSource() {
        var job=new Job();job.setId(3L);
        when(jobs.findVisibleById(3L,1L)).thenReturn(Optional.of(job));
        assertEquals(resume.getContentText(),controller.save(input(null,null),auth).getOriginalSnapshot());
    }

    @Test void anotherUsersSourceDocumentCannotBypassJobVisibility() {
        when(documents.findByIdAndUserId(8L,1L)).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.save(input(null,8L),auth)).getStatusCode().value());
        verify(documents,never()).save(any());
    }

    @Test void sourceDocumentForAnotherJobCannotBeReused() {
        var source=new TailoredDocument();source.setResumeId(2L);source.setJobId(99L);
        when(documents.findByIdAndUserId(8L,1L)).thenReturn(Optional.of(source));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.save(input(null,8L),auth)).getStatusCode().value());
        verify(documents,never()).save(any());
    }

    @Test void rejectedTaskProvenanceNeverCreatesADocument() {
        when(tasks.originalForSave(1L,7L,2L,3L)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.save(input(7L,null),auth)).getStatusCode().value());
        verify(documents,never()).save(any());
    }

    @Test void provenanceNeverBypassesResumeOwnership() {
        resume.setUserId(99L);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.save(input(7L,null),auth)).getStatusCode().value());
        verifyNoInteractions(tasks,jobs);
        verify(documents,never()).save(any());
    }
}
