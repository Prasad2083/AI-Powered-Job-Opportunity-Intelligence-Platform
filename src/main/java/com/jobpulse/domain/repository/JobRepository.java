package com.jobpulse.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jobpulse.domain.entity.Job;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findBySourceJobId(String sourceJobId);
    List<Job> findByAppliedFalse();
}

