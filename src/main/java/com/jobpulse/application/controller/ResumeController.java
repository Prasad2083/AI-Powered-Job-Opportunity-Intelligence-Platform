package com.jobpulse.application.controller;

import com.jobpulse.application.service.ResumeService;
import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.repository.ResumeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import org.springframework.security.core.Authentication;
import com.jobpulse.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeRepository resumeRepository;
    private final UserRepository users;

    public ResumeController(ResumeService resumeService, ResumeRepository resumeRepository, UserRepository users) {
        this.resumeService = resumeService;
        this.resumeRepository = resumeRepository;
        this.users = users;
    }

    @PostMapping(value = "/upload")
    public ResponseEntity<Resume> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "userId", required = false) Long userId, Authentication authentication) throws Exception {
        Long ownId = users.findByUsername(authentication.getName()).orElseThrow().getId();
        Resume r = resumeService.storeResume(file, ownId);
        return ResponseEntity.ok(r);
    }

    @GetMapping
    public ResponseEntity<List<Resume>> list(Authentication authentication) {
        return ResponseEntity.ok(resumeRepository.findByUserIdOrderByCreatedAtDesc(users.findByUsername(authentication.getName()).orElseThrow().getId()));
    }

    @GetMapping("/active")
    public ResponseEntity<Resume> active(Authentication authentication) {
        return ResponseEntity.of(resumeRepository.findFirstByUserIdAndIsPrimaryTrue(users.findByUsername(authentication.getName()).orElseThrow().getId()));
    }
}

