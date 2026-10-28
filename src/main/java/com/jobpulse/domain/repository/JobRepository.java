package com.jobpulse.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jobpulse.domain.entity.Job;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    @org.springframework.data.jpa.repository.Query("select j from Job j where j.isArchived = false and (j.ownerUserId = :uid or (j.ownerUserId is null and j.source.name <> 'MANUAL'))")
    List<Job> findVisible(@org.springframework.data.repository.query.Param("uid") Long uid);
    Optional<Job> findBySource_NameAndSourceJobId(String sourceName,String sourceJobId);
    List<Job> findBySource_NameAndIsArchivedFalse(String sourceName);
    Optional<Job> findBySourceJobId(String sourceJobId);
    List<Job> findByAppliedFalse();
}

