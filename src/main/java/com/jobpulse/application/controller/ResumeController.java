package com.jobpulse.application.controller;

import com.jobpulse.application.service.ResumeService;
import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.repository.ResumeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
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
    public ResponseEntity<Map<String,Object>> upload(@RequestParam("file") MultipartFile file,
                                         Authentication authentication) throws Exception {
        Long ownId = users.findByUsername(authentication.getName()).orElseThrow().getId();
        Resume r = resumeService.storeResume(file, ownId);
        return ResponseEntity.ok(summary(r));
    }

    @GetMapping
    public ResponseEntity<List<Map<String,Object>>> list(Authentication authentication) {
        return ResponseEntity.ok(resumeRepository.findByUserIdOrderByCreatedAtDesc(users.findByUsername(authentication.getName()).orElseThrow().getId()).stream().map(this::summary).toList());
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String,Object>> active(Authentication authentication) {
        return ResponseEntity.of(resumeRepository.findFirstByUserIdAndIsPrimaryTrue(users.findByUsername(authentication.getName()).orElseThrow().getId()).map(this::summary));
    }
    @GetMapping("/{id}/text")
    public ResponseEntity<Map<String,Object>> text(@PathVariable Long id, Authentication authentication) {
        Long uid=users.findByUsername(authentication.getName()).orElseThrow().getId();
        return ResponseEntity.of(resumeRepository.findById(id).filter(r -> r.getUserId().equals(uid))
            .map(r -> Map.<String,Object>of("id",r.getId(),"text",r.getContentText()==null?"":r.getContentText())));
    }

    @PutMapping("/{id}/text")
    public ResponseEntity<Map<String,Object>> updateText(@PathVariable Long id, @RequestBody Map<String,String> body, Authentication authentication) {
        Long uid=users.findByUsername(authentication.getName()).orElseThrow().getId();
        String text=body.getOrDefault("text","").trim();
        if(text.isBlank() || text.length()>100000) return ResponseEntity.badRequest().build();
        var resume=resumeRepository.findById(id).filter(r -> r.getUserId().equals(uid));
        if(resume.isEmpty()) return ResponseEntity.notFound().build();
        Resume r=resume.get();
        r.setContentText(text);
        resumeRepository.save(r);
        return ResponseEntity.ok(summary(r));
    }

    private Map<String,Object> summary(Resume resume) {
        return Map.of("id",resume.getId(), "filename",resume.getFileName(),"active",Boolean.TRUE.equals(resume.getIsPrimary()));
    }
}


