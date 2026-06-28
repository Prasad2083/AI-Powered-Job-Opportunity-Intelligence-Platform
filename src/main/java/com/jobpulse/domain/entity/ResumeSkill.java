package com.jobpulse.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "resume_skills")
public class ResumeSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long resumeId;
    private String skillName;
    private String category; // Languages, Frameworks, Databases, Tools, etc.
    private Integer proficiency; // 1-5 scale or null for unranked
    private Instant createdAt = Instant.now();

    public ResumeSkill() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getProficiency() { return proficiency; }
    public void setProficiency(Integer proficiency) { this.proficiency = proficiency; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

