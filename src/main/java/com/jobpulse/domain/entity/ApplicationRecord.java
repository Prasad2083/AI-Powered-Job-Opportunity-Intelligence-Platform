package com.jobpulse.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_match_id")
    private JobMatch jobMatch;

    @Column(name = "application_status")
    private String applicationStatus;

    @Column(name = "applied_date")
    private LocalDateTime appliedDate;

    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate;

    private String notes;

    @Column(name = "custom_cover_letter")
    private String customCoverLetter;

    @Column(name = "form_data_json", columnDefinition = "jsonb")
    private String formDataJson;  // Store as String if you prefer

    @Column(name = "is_approved_by_user")
    private Boolean isApprovedByUser;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}