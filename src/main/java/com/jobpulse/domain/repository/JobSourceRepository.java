package com.jobpulse.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jobpulse.domain.entity.JobSource;
import java.util.Optional;

public interface JobSourceRepository extends JpaRepository<JobSource, Long> {
    Optional<JobSource> findByName(String name);
}