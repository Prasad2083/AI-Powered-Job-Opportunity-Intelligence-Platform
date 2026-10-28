package com.jobpulse.application.controller;

import com.jobpulse.application.service.ResumeTailoringService;
import com.jobpulse.domain.repository.JobRepository;
import com.jobpulse.domain.repository.ResumeRepository;
import com.jobpulse.domain.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/tailoring")
public class TailoringController {
    private final ResumeTailoringService tailoring;
    private final ResumeRepository resumes;
    private final JobRepository jobs;
    private final UserRepository users;
    private final com.jobpulse.domain.repository.TailoredDocumentRepository documents;
    TailoringController(ResumeTailoringService tailoring, ResumeRepository resumes, JobRepository jobs, UserRepository users, com.jobpulse.domain.repository.TailoredDocumentRepository documents) {
        this.tailoring=tailoring;this.resumes=resumes;this.jobs=jobs;this.users=users;this.documents=documents;
    }
    @PostMapping("/{jobId}")
    public Map<String,Object> preview(@PathVariable Long jobId, Authentication auth) {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        var resume=resumes.findFirstByUserIdAndIsPrimaryTrue(uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume missing"));
        var job=jobs.findVisible(uid).stream().filter(j -> j.getId().equals(jobId)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job missing"));
        String draft=tailoring.tailor(resume.getContentText(),job.getJobDescription());
        return Map.of("resumeId",resume.getId(),"jobId",jobId,"draft",draft,"status","DRAFT_REVIEW_REQUIRED","applyUrl",job.getApplyUrl()==null?"":job.getApplyUrl());
    }

    public record SaveRequest(@jakarta.validation.constraints.NotNull Long resumeId, @jakarta.validation.constraints.NotNull Long jobId,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100000) String content,
        boolean approved) {}

    @PostMapping("/documents")
    public com.jobpulse.domain.entity.TailoredDocument save(@jakarta.validation.Valid @RequestBody SaveRequest input, Authentication auth) {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        var resume=resumes.findById(input.resumeId()).filter(r->r.getUserId().equals(uid))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume missing"));
        jobs.findVisible(uid).stream().filter(j->j.getId().equals(input.jobId())).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job missing"));
        var document=new com.jobpulse.domain.entity.TailoredDocument();
        document.setUserId(uid);document.setResumeId(resume.getId());document.setJobId(input.jobId());
        document.setContent(input.content());document.setOriginalSnapshot(resume.getContentText());document.setApproved(input.approved());
        return documents.save(document);
    }

    @GetMapping("/documents")
    public java.util.List<com.jobpulse.domain.entity.TailoredDocument> list(Authentication auth) {
        return documents.findByUserIdOrderByCreatedAtDesc(users.findByUsername(auth.getName()).orElseThrow().getId());
    }

    @GetMapping("/documents/{id}/download")
    public org.springframework.http.ResponseEntity<String> download(@PathVariable Long id, Authentication auth) {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        var document=documents.findByIdAndUserId(id,uid)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Document missing"));
        return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType("text/plain;charset=UTF-8"))
            .header("Content-Disposition","attachment; filename=jobpulse-tailored-"+id+".txt").body(document.getContent());
    }
}
