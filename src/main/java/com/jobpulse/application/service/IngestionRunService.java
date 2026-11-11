package com.jobpulse.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;

/** Coalesces shared employer-feed refreshes; transactions never include network calls. */
@Service
public class IngestionRunService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    @Value("${jobpulse.feeds.minimum-refresh-ms:60000}") private long minimumRefreshMs=60000;
    @Value("${app.job-ingestion.enabled:true}") private boolean enabled=true;
    public IngestionRunService(JdbcTemplate db,ObjectMapper json) { this.db=db;this.json=json; }

    public record Run(long id,String status,JsonNode report,String error,Instant createdAt,Instant finishedAt) {}
    private final RowMapper<Run> view=this::readRun;
    private Run readRun(java.sql.ResultSet r,int n) throws java.sql.SQLException {
        String report=r.getString("report");
        JsonNode parsed=null;
        try { if(report!=null) parsed=json.readTree(report); }
        catch(java.io.IOException ex) { throw new IllegalStateException("Stored refresh report unavailable"); }
        var finished=r.getTimestamp("finished_at");
        return new Run(r.getLong("id"),r.getString("status"),parsed,r.getString("error"),
            r.getTimestamp("created_at").toInstant(),finished==null?null:finished.toInstant());
    }
    private void lockQueue() {
        db.queryForObject("SELECT id FROM ingestion_queue_lock WHERE id=1 FOR UPDATE",Integer.class);
    }

    @Transactional
    public Run request() {
        if(!enabled) throw new ResponseStatusException(HttpStatus.CONFLICT,"Job discovery is disabled by configuration");
        lockQueue();
        var active=db.query("SELECT * FROM ingestion_runs WHERE status IN ('QUEUED','RUNNING') ORDER BY id DESC LIMIT 1",view);
        if(!active.isEmpty()) return active.getFirst();
        var recent=db.query("SELECT * FROM ingestion_runs WHERE finished_at > clock_timestamp() - (? * interval '1 millisecond') ORDER BY id DESC LIMIT 1",view,Math.max(0,minimumRefreshMs));
        if(!recent.isEmpty()) return recent.getFirst();
        Long id=db.queryForObject("INSERT INTO ingestion_runs(status) VALUES ('QUEUED') RETURNING id",Long.class);
        return get(id);
    }

    public Optional<Run> latest() {
        return db.query("SELECT * FROM ingestion_runs ORDER BY id DESC LIMIT 1",view).stream().findFirst();
    }
    public Run get(long id) {
        return db.query("SELECT * FROM ingestion_runs WHERE id=?",view,id).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Discovery run missing"));
    }

    @Transactional
    public Optional<Long> claim() {
        lockQueue();
        db.update("UPDATE ingestion_runs SET status='FAILED',error='Refresh was interrupted. Some updates may have completed; review listings and request another refresh.',finished_at=clock_timestamp() WHERE status='RUNNING' AND heartbeat_at < clock_timestamp()-interval '2 minutes'");
        var ids=db.queryForList("SELECT id FROM ingestion_runs WHERE status='QUEUED' ORDER BY id LIMIT 1",Long.class);
        if(ids.isEmpty()) return Optional.empty();
        long id=ids.getFirst();
        db.update("UPDATE ingestion_runs SET status='RUNNING',started_at=clock_timestamp(),heartbeat_at=clock_timestamp() WHERE id=? AND status='QUEUED'",id);
        return Optional.of(id);
    }

    public void heartbeat(long id) {
        db.update("UPDATE ingestion_runs SET heartbeat_at=clock_timestamp() WHERE id=? AND status='RUNNING'",id);
    }

    @Transactional
    public void complete(long id,JobIngestionService.IngestionReport report,String error) {
        String state="FAILED",payload=null;
        if(report!=null && error==null) {
            boolean problems=report.failed()>0 || !report.sourceFailures().isEmpty();
            state=problems?"PARTIAL":"SUCCEEDED";
            if(report.checkedSources().isEmpty() && !report.sourceFailures().isEmpty()) {
                state="FAILED";error="No employer feed completed. Previous listings are retained; review the feed messages.";
            }
            try { payload=json.writeValueAsString(report); }
            catch(java.io.IOException ex) { throw new IllegalStateException("Unable to store refresh report"); }
        } else if(error==null) error="Refresh returned no report. Review listings before retrying.";
        // A recovered/terminal run cannot be resurrected by a late worker result.
        db.update("UPDATE ingestion_runs SET status=?,report=CAST(? AS jsonb),error=?,finished_at=clock_timestamp() WHERE id=? AND status='RUNNING'",state,payload,error,id);
    }
}
