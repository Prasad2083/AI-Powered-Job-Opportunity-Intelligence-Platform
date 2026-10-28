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
    TailoringController(ResumeTailoringService tailoring, ResumeRepository resumes, JobRepository jobs, UserRepository users) {
        this.tailoring=tailoring;this.resumes=resumes;this.jobs=jobs;this.users=users;
    }
    @PostMapping("/{jobId}")
    public Map<String,Object> preview(@PathVariable Long jobId, Authentication auth) {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        var resume=resumes.findFirstByUserIdAndIsPrimaryTrue(uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume missing"));
        var job=jobs.findById(jobId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job missing"));
        String draft=tailoring.tailor(resume.getContentText(),job.getJobDescription());
        return Map.of("jobId",jobId,"draft",draft,"status","DRAFT_REVIEW_REQUIRED","applyUrl",job.getApplyUrl()==null?"":job.getApplyUrl());
    }
}
