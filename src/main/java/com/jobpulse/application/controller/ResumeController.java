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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String,Object>> upload(@RequestParam("file") MultipartFile file,
                                         Authentication authentication) throws Exception {
        Long ownId = users.lockByUsername(authentication.getName()).orElseThrow().getId();
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
        String text=java.util.Objects.toString(body.get("text"),"").trim();
        if(text.isBlank() || text.length()>100000) return ResponseEntity.badRequest().build();
        var resume=resumeRepository.findById(id).filter(r -> r.getUserId().equals(uid));
        if(resume.isEmpty()) return ResponseEntity.notFound().build();
        Resume r=resume.get();
        r.setContentText(text);
        resumeRepository.save(r);
        return ResponseEntity.ok(summary(r));
    }

    @PutMapping("/{id}/primary")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String,Object>> primary(@PathVariable Long id, Authentication auth) {
        Long uid=users.lockByUsername(auth.getName()).orElseThrow().getId();
        var own=resumeRepository.findById(id).filter(r -> r.getUserId().equals(uid));
        if (own.isEmpty()) return ResponseEntity.notFound().build();
        for (Resume resume : resumeRepository.findByUserIdOrderByCreatedAtDesc(uid)) {
            resume.setIsPrimary(resume.getId().equals(id));
            resumeRepository.save(resume);
        }
        return ResponseEntity.ok(summary(own.get()));
    }

    @GetMapping("/{id}/original")
    public ResponseEntity<byte[]> original(@PathVariable Long id, Authentication auth) {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        var own=resumeRepository.findById(id).filter(r -> r.getUserId().equals(uid));
        if (own.isEmpty() || own.get().getOriginalFile()==null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", org.springframework.http.ContentDisposition.attachment().filename(own.get().getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
            .body(own.get().getOriginalFile());
    }

    private Map<String,Object> summary(Resume resume) {
        return Map.of("id",resume.getId(), "filename",resume.getFileName(),"active",Boolean.TRUE.equals(resume.getIsPrimary()));
    }
}


