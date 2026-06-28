package com.jobpulse.domain.repository;

import com.jobpulse.domain.entity.ApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRecordRepository extends JpaRepository<ApplicationRecord, Long> {
}

