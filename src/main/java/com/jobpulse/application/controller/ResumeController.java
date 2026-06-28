package com.jobpulse.application.controller;

import com.jobpulse.application.service.ResumeService;
import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.repository.ResumeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeRepository resumeRepository;

    public ResumeController(ResumeService resumeService, ResumeRepository resumeRepository) {
        this.resumeService = resumeService;
        this.resumeRepository = resumeRepository;
    }

    @PostMapping(value = "/upload")
    public ResponseEntity<Resume> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "userId", required = false) Long userId) throws Exception {
        Resume r = resumeService.storeResume(file, userId);
        return ResponseEntity.ok(r);
    }

    @GetMapping
    public ResponseEntity<List<Resume>> list() {
        return ResponseEntity.ok(resumeRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<Resume> active() {
        return ResponseEntity.of(resumeRepository.findFirstByActiveTrue());
    }
}

