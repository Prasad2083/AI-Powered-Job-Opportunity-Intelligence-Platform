package com.jobpulse.application.controller;

import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.entity.User;
import com.jobpulse.domain.repository.ResumeRepository;
import com.jobpulse.domain.repository.UserRepository;
import com.jobpulse.application.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumeControllerTest {
    @Test void uploadAlwaysUsesAuthenticatedUserNotSuppliedId() throws Exception {
        ResumeService service=mock(ResumeService.class);
        ResumeRepository resumes=mock(ResumeRepository.class);
        UserRepository users=mock(UserRepository.class);
        User user=new User(); user.setId(42L);user.setUsername("alice");
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(users.lockByUsername("alice")).thenReturn(Optional.of(user));
        Resume saved=new Resume();saved.setId(3L);saved.setUserId(42L);saved.setFileName("cv.txt");saved.setIsPrimary(true);
        var upload=new MockMultipartFile("file","cv.txt","text/plain","Java".getBytes(StandardCharsets.UTF_8));
        when(service.storeResume(upload,42L)).thenReturn(saved);
        var response=new ResumeController(service,resumes,users).upload(upload,new UsernamePasswordAuthenticationToken("alice",null));
        assertEquals(3L,response.getBody().get("id"));
        verify(service).storeResume(upload,42L);
    }
    @Test void cannotEditAnotherUsersResume() {
        ResumeService service=mock(ResumeService.class);
        ResumeRepository resumes=mock(ResumeRepository.class);
        UserRepository users=mock(UserRepository.class);
        User user=new User();user.setId(42L);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(users.lockByUsername("alice")).thenReturn(Optional.of(user));
        Resume other=new Resume();other.setId(9L);other.setUserId(99L);
        when(resumes.findById(9L)).thenReturn(Optional.of(other));
        var result=new ResumeController(service,resumes,users).updateText(9L,java.util.Map.of("text","stolen"),new UsernamePasswordAuthenticationToken("alice",null));
        assertEquals(HttpStatus.NOT_FOUND,result.getStatusCode());
        verify(resumes,never()).save(any());
    }
}
