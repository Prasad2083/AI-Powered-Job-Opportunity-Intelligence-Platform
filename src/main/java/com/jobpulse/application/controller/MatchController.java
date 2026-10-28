package com.jobpulse.application.controller;

import com.jobpulse.common.util.TextMatcher;
import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.repository.JobRepository;
import com.jobpulse.domain.repository.ResumeRepository;
import com.jobpulse.domain.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
@RequestMapping("/matches")
public class MatchController {
    private final JobRepository jobs;
    private final ResumeRepository resumes;
    private final UserRepository users;
    public MatchController(JobRepository jobs, ResumeRepository resumes, UserRepository users) {
        this.jobs=jobs; this.resumes=resumes; this.users=users;
    }
    @GetMapping
    public List<Map<String,Object>> matches(Authentication auth) {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        Resume resume=resumes.findFirstByUserIdAndIsPrimaryTrue(uid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Upload a resume first"));
        List<Map<String,Object>> results=new ArrayList<>();
        for (Job job : jobs.findAll()) {
            double score=TextMatcher.score(job.getTitle()+" "+job.getJobDescription(),resume.getContentText());
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("jobId",job.getId()); result.put("title",job.getTitle()); result.put("company",job.getCompanyName());
            result.put("applyUrl",job.getApplyUrl()); result.put("source",job.getSource().getName());
            result.put("score",Math.round(score*10000.0)/100.0); result.put("status","REVIEW_REQUIRED");
            results.add(result);
        }
        results.sort((a,b)->Double.compare((Double)b.get("score"),(Double)a.get("score")));
        return results;
    }
}
