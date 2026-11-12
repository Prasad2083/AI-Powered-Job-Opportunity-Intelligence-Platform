package com.jobpulse.application.service;

import com.jobpulse.domain.repository.JobRepository;
import com.jobpulse.domain.repository.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Database transactions are short; no database lock is held during model execution. */
@Service
public class TailoringTaskService {
    private final JdbcTemplate db;
    private final ResumeRepository resumes;
    private final JobRepository jobs;
    public TailoringTaskService(JdbcTemplate db, ResumeRepository resumes, JobRepository jobs) {
        this.db=db; this.resumes=resumes; this.jobs=jobs;
    }

    public record Task(long id, long resumeId, long jobId, String status, String draft,
                       String error, String applyUrl, Instant createdAt) {}
    public record Work(long id, String original, String description) {}
    private static final RowMapper<Task> VIEW=(r,n)->new Task(r.getLong("id"),r.getLong("resume_id"),
        r.getLong("job_id"),r.getString("status"),r.getString("draft"),r.getString("error"),
        r.getString("apply_url"),r.getTimestamp("created_at").toInstant());
    private static final String ACTIVE="('QUEUED','RUNNING','CANCEL_REQUESTED')";

    private void lockQueue() {
        db.queryForObject("SELECT id FROM tailoring_queue_lock WHERE id=1 FOR UPDATE", Integer.class);
    }

    @Transactional
    public Task enqueue(long uid,long jobId) {
        var resume=resumes.findFirstByUserIdAndIsPrimaryTrue(uid)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Upload a primary resume first"));
        var job=jobs.findVisibleById(jobId,uid)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Job missing"));
        if(resume.getContentText()==null || resume.getContentText().isBlank()
            || job.getJobDescription()==null || job.getJobDescription().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Resume text and a job description are required");
        lockQueue();
        if(db.queryForObject("SELECT count(*) FROM tailoring_tasks WHERE user_id=? AND status IN "+ACTIVE,Long.class,uid)>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"A draft is already queued or running. Review its status before starting another.");
        if(db.queryForObject("SELECT count(*) FROM tailoring_tasks WHERE status IN "+ACTIVE,Long.class)>=20)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"The local AI queue is full. Please try again later.");
        Long id=db.queryForObject("INSERT INTO tailoring_tasks(user_id,resume_id,job_id,original_snapshot,description_snapshot,apply_url) VALUES (?,?,?,?,?,?) RETURNING id",
            Long.class,uid,resume.getId(),jobId,resume.getContentText(),job.getJobDescription(),job.getApplyUrl()==null?"":job.getApplyUrl());
        return get(uid,id);
    }

    public Task get(long uid,long id) {
        return db.query("SELECT * FROM tailoring_tasks WHERE id=? AND user_id=?",VIEW,id,uid).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Task missing"));
    }

    public List<Task> list(long uid) {
        // List metadata only; fetch a single result when the user opens it.
        return db.query("SELECT id,resume_id,job_id,status,NULL AS draft,error,apply_url,created_at FROM tailoring_tasks WHERE user_id=? ORDER BY id DESC LIMIT 20",VIEW,uid);
    }

    @Transactional
    public Task cancel(long uid,long id) {
        // Atomic transition: cancellation racing completion never overwrites a finished draft.
        db.update("UPDATE tailoring_tasks SET status=CASE WHEN status='QUEUED' THEN 'CANCELLED' ELSE 'CANCEL_REQUESTED' END, finished_at=CASE WHEN status='QUEUED' THEN clock_timestamp() ELSE NULL END WHERE id=? AND user_id=? AND status IN ('QUEUED','RUNNING')",id,uid);
        return get(uid,id);
    }

    @Transactional
    public Optional<Work> claim() {
        lockQueue();
        // Survives a process crash: an abandoned lease is failed visibly, never retried silently.
        // Five minutes exceeds the model client's bounded 5s connect + 120s read timeouts.
        db.update("UPDATE tailoring_tasks SET status=CASE WHEN status='CANCEL_REQUESTED' THEN 'CANCELLED' ELSE 'FAILED' END, error=CASE WHEN status='RUNNING' THEN 'Processing was interrupted or timed out. Start a new draft to retry.' ELSE NULL END,finished_at=clock_timestamp() WHERE status IN ('RUNNING','CANCEL_REQUESTED') AND started_at < clock_timestamp()-interval '5 minutes'");
        if(db.queryForObject("SELECT count(*) FROM tailoring_tasks WHERE status IN ('RUNNING','CANCEL_REQUESTED')",Long.class)>0)
            return Optional.empty();
        var ids=db.queryForList("SELECT id FROM tailoring_tasks WHERE status='QUEUED' ORDER BY id LIMIT 1",Long.class);
        if(ids.isEmpty()) return Optional.empty();
        long id=ids.getFirst();
        // Cancellation may win between selection and claim.
        if(db.update("UPDATE tailoring_tasks SET status='RUNNING',started_at=clock_timestamp() WHERE id=? AND status='QUEUED'",id)==0)
            return Optional.empty();
        return db.query("SELECT id,original_snapshot,description_snapshot FROM tailoring_tasks WHERE id=?",
            (r,n)->new Work(r.getLong("id"),r.getString("original_snapshot"),r.getString("description_snapshot")),id).stream().findFirst();
    }

    @Transactional
    public void complete(long id,String draft,String error) {
        // Late model results cannot resurrect an expired or cancelled task.
        db.update("UPDATE tailoring_tasks SET status=CASE WHEN status='CANCEL_REQUESTED' THEN 'CANCELLED' WHEN CAST(? AS text) IS NULL THEN 'SUCCEEDED' ELSE 'FAILED' END,draft=CASE WHEN status='RUNNING' AND CAST(? AS text) IS NULL THEN ? ELSE NULL END,error=CASE WHEN status='RUNNING' THEN ? ELSE NULL END,finished_at=clock_timestamp() WHERE id=? AND status IN ('RUNNING','CANCEL_REQUESTED')",
            error,error,draft,error,id);
    }

    public String originalForSave(long uid,long taskId,long resumeId,long jobId) {
        return db.query("SELECT original_snapshot FROM tailoring_tasks WHERE id=? AND user_id=? AND resume_id=? AND job_id=? AND status='SUCCEEDED'",
            (r,n)->r.getString(1),taskId,uid,resumeId,jobId).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Completed source task missing"));
    }
}
