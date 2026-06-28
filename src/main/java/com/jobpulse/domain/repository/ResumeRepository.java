package com.jobpulse.domain.repository;

import com.jobpulse.domain.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findFirstByActiveTrue();
}

