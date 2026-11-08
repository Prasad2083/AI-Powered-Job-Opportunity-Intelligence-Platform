package com.jobpulse.application.controller;

import com.jobpulse.application.service.TailoringTaskService;
import com.jobpulse.domain.repository.JobRepository;
import com.jobpulse.domain.repository.ResumeRepository;
import com.jobpulse.domain.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/tailoring")
public class TailoringController {
    private final TailoringTaskService tasks;
    private final com.jobpulse.application.service.DocumentExportService exports;
    private final ResumeRepository resumes;
    private final JobRepository jobs;
    private final UserRepository users;
    private final com.jobpulse.domain.repository.TailoredDocumentRepository documents;
    TailoringController(TailoringTaskService tasks, ResumeRepository resumes, JobRepository jobs, UserRepository users, com.jobpulse.domain.repository.TailoredDocumentRepository documents, com.jobpulse.application.service.DocumentExportService exports) {
        this.tasks=tasks;this.resumes=resumes;this.jobs=jobs;this.users=users;this.documents=documents;this.exports=exports;
    }
    // The former preview route also queues work; it cannot bypass model concurrency limits.
    @PostMapping({"/{jobId}","/tasks/{jobId}"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TailoringTaskService.Task preview(@PathVariable Long jobId, Authentication auth) {
        return tasks.enqueue(userId(auth),jobId);
    }

    @GetMapping("/tasks")
    public java.util.List<TailoringTaskService.Task> tasks(Authentication auth) {
        return tasks.list(userId(auth));
    }

    @GetMapping("/tasks/{id}")
    public TailoringTaskService.Task task(@PathVariable long id,Authentication auth) {
        return tasks.get(userId(auth),id);
    }

    @PostMapping("/tasks/{id}/cancel")
    public TailoringTaskService.Task cancel(@PathVariable long id,Authentication auth) {
        return tasks.cancel(userId(auth),id);
    }

    private long userId(Authentication auth) {
        return users.findByUsername(auth.getName()).orElseThrow().getId();
    }

    public record SaveRequest(@jakarta.validation.constraints.NotNull Long resumeId, @jakarta.validation.constraints.NotNull Long jobId,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100000) String content,
        boolean approved, Long taskId, Long sourceDocumentId) {}

    @PostMapping("/documents")
    public com.jobpulse.domain.entity.TailoredDocument save(@jakarta.validation.Valid @RequestBody SaveRequest input, Authentication auth) {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        var resume=resumes.findById(input.resumeId()).filter(r->r.getUserId().equals(uid))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume missing"));
        jobs.findVisible(uid).stream().filter(j->j.getId().equals(input.jobId())).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job missing"));
        var document=new com.jobpulse.domain.entity.TailoredDocument();
        document.setUserId(uid);document.setResumeId(resume.getId());document.setJobId(input.jobId());
        document.setContent(input.content());
        String snapshot=resume.getContentText();
        if(input.sourceDocumentId()!=null) snapshot=documents.findByIdAndUserId(input.sourceDocumentId(),uid)
            .filter(source->source.getResumeId().equals(input.resumeId()) && source.getJobId().equals(input.jobId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Source document missing")).getOriginalSnapshot();
        if(input.taskId()!=null) snapshot=tasks.originalForSave(uid,input.taskId(),resume.getId(),input.jobId());
        document.setOriginalSnapshot(snapshot);
        document.setApproved(input.approved());
        return documents.save(document);
    }

    @GetMapping("/documents")
    public java.util.List<com.jobpulse.domain.entity.TailoredDocument> list(Authentication auth) {
        return documents.findByUserIdOrderByCreatedAtDesc(users.findByUsername(auth.getName()).orElseThrow().getId());
    }

    @GetMapping("/documents/{id}/download")
    public org.springframework.http.ResponseEntity<byte[]> download(@PathVariable Long id, @RequestParam(defaultValue="txt") String format, Authentication auth) throws java.io.IOException {
        Long uid=users.findByUsername(auth.getName()).orElseThrow().getId();
        var document=documents.findByIdAndUserId(id,uid)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Document missing"));
        if (!format.equals("txt") && !format.equals("docx"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Supported export formats are txt and docx");
        byte[] bytes = format.equals("docx") ? exports.docx(document.getContent()) : document.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var type = org.springframework.http.MediaType.parseMediaType(format.equals("docx")
            ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "text/plain;charset=UTF-8");
        return org.springframework.http.ResponseEntity.ok().contentType(type)
            .header("Content-Disposition","attachment; filename=jobpulse-tailored-"+id+"."+format).body(bytes);
    }
}
