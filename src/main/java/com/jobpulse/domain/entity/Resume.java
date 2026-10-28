package com.jobpulse.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "original_file")
    private byte[] originalFile;

    private String skills;

    @Column(name = "experience_years")
    private java.math.BigDecimal experienceYears;

    private String certifications;

    private String tools;

    @Column(name = "embedding_generated")
    private Boolean embeddingGenerated = false;

    @Column(name = "embedding_vector")
    private byte[] embeddingVector;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}