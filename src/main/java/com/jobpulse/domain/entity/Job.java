package com.jobpulse.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_source_id", columnList = "source_id"),
        @Index(name = "idx_jobs_posted_date", columnList = "posted_date DESC"),
        @Index(name = "idx_jobs_company", columnList = "company_name"),
        @Index(name = "idx_jobs_applied", columnList = "applied")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private JobSource source;

    @Column(name = "source_job_id", nullable = false)
    private String sourceJobId;

    @Column(nullable = false)
    private String title;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_logo_url")
    private String companyLogoUrl;

    @Column(name = "company_website")
    private String companyWebsite;

    private String location;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    @Column(name = "salary_currency")
    private String salaryCurrency;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "skills_required", columnDefinition = "TEXT")
    private String skillsRequired;

    @Column(name = "experience_required")
    private String experienceRequired;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    @Column(name = "apply_url")
    private String applyUrl;

    @Column(name = "company_size")
    private String companySize;

    private String industry;

    @Column(name = "remote_option")
    private String remoteOption;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "jsonb")
    private JsonNode rawJson;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "applied")
    private Boolean applied = false;

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