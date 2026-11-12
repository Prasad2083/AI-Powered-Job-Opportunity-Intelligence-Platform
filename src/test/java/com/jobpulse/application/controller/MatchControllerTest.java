package com.jobpulse.application.controller;

import com.jobpulse.domain.entity.*;
import com.jobpulse.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchControllerTest {
    final JobRepository jobs=mock(JobRepository.class);
    final ResumeRepository resumes=mock(ResumeRepository.class);
    final UserRepository users=mock(UserRepository.class);
    final MatchController controller=new MatchController(jobs,resumes,users);

    private User user(long id,String username,String roles,String locations) {
        var user=new User();user.setId(id);user.setUsername(username);
        user.setTargetRoles(roles);user.setTargetLocations(locations);return user;
    }

    @Test void repeatedRequestsOnTheSameControllerNeverReuseAnotherUsersResume() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(user(1,"alice","Java Engineer","Toronto")));
        when(users.findByUsername("bob")).thenReturn(Optional.of(user(2,"bob","C# Engineer","Remote Canada")));
        var alice=new Resume();alice.setUserId(1L);alice.setContentText("Senior Java engineer with 5 years experience");
        var bob=new Resume();bob.setUserId(2L);bob.setContentText("C# engineer with 2 years experience");
        when(resumes.findFirstByUserIdAndIsPrimaryTrue(1L)).thenReturn(Optional.of(alice));
        when(resumes.findFirstByUserIdAndIsPrimaryTrue(2L)).thenReturn(Optional.of(bob));
        var source=new JobSource();source.setName("GREENHOUSE:fixture");
        var job=new Job();job.setId(3L);job.setSource(source);job.setTitle("Senior Java Engineer");
        job.setCompanyName("Explicit test fixture");job.setLocation("Toronto");job.setJobDescription("Java, 5 years experience");
        when(jobs.findVisible(1L)).thenReturn(List.of(job));when(jobs.findVisible(2L)).thenReturn(List.of(job));
        var first=controller.matches(new UsernamePasswordAuthenticationToken("alice",null)).getFirst();
        var second=controller.matches(new UsernamePasswordAuthenticationToken("bob",null)).getFirst();
        var third=controller.matches(new UsernamePasswordAuthenticationToken("alice",null)).getFirst();
        assertEquals(Set.of("java"),first.get("matchedSkills"));assertEquals(Set.of(),second.get("matchedSkills"));
        assertEquals(true,first.get("locationPreferenceMatched"));assertEquals(false,second.get("locationPreferenceMatched"));
        assertEquals(false,first.get("experienceReviewRequired"));assertEquals(true,second.get("experienceReviewRequired"));
        assertEquals(first.get("score"),third.get("score"));
        verify(resumes,times(2)).findFirstByUserIdAndIsPrimaryTrue(1L);
        verify(resumes).findFirstByUserIdAndIsPrimaryTrue(2L);
    }

    @Test void missingPrimaryResumeStopsBeforeReadingJobs() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(user(1,"alice",null,null)));
        when(resumes.findFirstByUserIdAndIsPrimaryTrue(1L)).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,
            ()->controller.matches(new UsernamePasswordAuthenticationToken("alice",null))).getStatusCode().value());
        verifyNoInteractions(jobs);
    }
}
