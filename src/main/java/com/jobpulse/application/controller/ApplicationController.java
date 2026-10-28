package com.jobpulse.application.controller;

import com.jobpulse.domain.entity.ApplicationRecord;
import com.jobpulse.domain.repository.ApplicationRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import com.jobpulse.domain.repository.UserRepository;

/**
 * Phase 7: Applications Controller for frontend
 */
@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationRecordRepository applicationRecordRepository;
    private final UserRepository users;
    private final com.jobpulse.domain.repository.JobRepository jobs;

    public ApplicationController(ApplicationRecordRepository applicationRecordRepository, UserRepository users, com.jobpulse.domain.repository.JobRepository jobs) {
        this.applicationRecordRepository = applicationRecordRepository;
        this.users = users;
        this.jobs = jobs;
    }

    @GetMapping
    public ResponseEntity<List<java.util.Map<String,Object>>> listApplications(Authentication authentication) {
        return ResponseEntity.ok(applicationRecordRepository.findByUserId(users.findByUsername(authentication.getName()).orElseThrow().getId()).stream().map(this::summary).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<java.util.Map<String,Object>> getApplication(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.of(applicationRecordRepository.findByIdAndUserId(id, users.findByUsername(authentication.getName()).orElseThrow().getId()).map(this::summary));
    }

    public record ManualCompletion(@jakarta.validation.constraints.NotNull Long jobId, boolean completed,
        @jakarta.validation.constraints.Size(max=5000) String notes) {}

    @PostMapping("/manual")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<java.util.Map<String,Object>> manual(@jakarta.validation.Valid @RequestBody ManualCompletion input,Authentication auth) {
        if (!input.completed()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Confirm that you completed the official application first");
        var user=users.lockByUsername(auth.getName()).orElseThrow();
        var job=jobs.findVisible(user.getId()).stream().filter(j->j.getId().equals(input.jobId())).findFirst()
            .orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Job missing"));
        if (job.getSource().getName().equals("MOCK")) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Mock jobs cannot be applied to");
        if (applicationRecordRepository.existsByUserIdAndJobId(user.getId(),job.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Application already recorded for this job");
        var now=java.time.LocalDateTime.now();
        var record=ApplicationRecord.builder().user(user).job(job).applicationStatus("USER_REPORTED_SUBMITTED")
            .appliedDate(now).notes(input.notes()).isApprovedByUser(true).approvedAt(now).createdAt(now).updatedAt(now).build();
        return ResponseEntity.ok(summary(applicationRecordRepository.save(record)));
    }

    public enum UserReportedStatus { USER_REPORTED_SUBMITTED, USER_REPORTED_INTERVIEW, USER_REPORTED_OFFER, USER_REPORTED_REJECTED, USER_REPORTED_WITHDRAWN }
    public record RecordUpdate(@jakarta.validation.constraints.NotNull UserReportedStatus status,
        @jakarta.validation.constraints.Size(max=5000) String notes) {}

    @PatchMapping("/{id}")
    public ResponseEntity<java.util.Map<String,Object>> update(@PathVariable Long id,@jakarta.validation.Valid @RequestBody RecordUpdate input,Authentication auth) {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        var record=applicationRecordRepository.findByIdAndUserId(id,uid)
            .orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Application missing"));
        record.setApplicationStatus(input.status().name());record.setNotes(input.notes());record.setUpdatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(summary(applicationRecordRepository.save(record)));
    }

    private java.util.Map<String,Object> summary(ApplicationRecord r) {
        var result=new java.util.LinkedHashMap<String,Object>();
        result.put("id",r.getId());result.put("job",java.util.Map.of("id",r.getJob().getId(),"title",r.getJob().getTitle()));
        result.put("applicationStatus",r.getApplicationStatus());result.put("appliedDate",r.getAppliedDate());result.put("notes",r.getNotes());
        result.put("providerConfirmed",false);return result;
    }
}

