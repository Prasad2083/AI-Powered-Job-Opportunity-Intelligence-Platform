package com.jobpulse.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "job_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "match_percentage")
    private BigDecimal matchPercentage;

    @Column(name = "similarity_score")
    private BigDecimal similarityScore;

    @Column(name = "missing_skills")
    private String missingSkills;

    @Column(name = "strengths_alignment")
    private String strengthsAlignment;

    @Column(name = "ats_optimization_score")
    private BigDecimal atsOptimizationScore;

    @Column(name = "interview_probability_score")
    private BigDecimal interviewProbabilityScore;

    @Column(name = "match_explanation")
    private String matchExplanation;

    @Column(name = "nonmatch_explanation")
    private String nonmatchExplanation;

    @Column(name = "resume_improvements")
    private String resumeImprovements;

    @Column(name = "recruiter_message")
    private String recruiterMessage;

    @Column(name = "cover_letter")
    private String coverLetter;

    @Column(name = "is_starred")
    private Boolean isStarred;

    @Column(name = "is_dismissed")
    private Boolean isDismissed;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}