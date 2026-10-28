package com.jobpulse.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name="tailored_documents")
@Data
public class TailoredDocument {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long userId;
    private Long resumeId;
    private Long jobId;
    @Column(columnDefinition="TEXT") private String content;
    @Column(columnDefinition="TEXT") private String originalSnapshot;
    private boolean approved;
    private LocalDateTime createdAt=LocalDateTime.now();
}
