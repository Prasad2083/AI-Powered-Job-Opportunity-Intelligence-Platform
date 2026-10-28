package com.jobpulse.domain.repository;

import com.jobpulse.domain.entity.TailoredDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TailoredDocumentRepository extends JpaRepository<TailoredDocument,Long> {
    List<TailoredDocument> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<TailoredDocument> findByIdAndUserId(Long id, Long userId);
}
