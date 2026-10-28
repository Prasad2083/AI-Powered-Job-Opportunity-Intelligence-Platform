package com.jobpulse.domain.repository;

import com.jobpulse.domain.entity.ApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRecordRepository extends JpaRepository<ApplicationRecord, Long> {
    boolean existsByUserIdAndJobId(Long userId, Long jobId);
    java.util.List<ApplicationRecord> findByUserId(Long userId);
    java.util.Optional<ApplicationRecord> findByIdAndUserId(Long id, Long userId);
}

