package com.ai.career.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysis {

    @Id
    @GeneratedValue
    private UUID id;

    // SHA256 hash of normalized resume text
    @Column(nullable = false, unique = true, length = 64)
    private String contentHash;

    // Stored AI analysis result (JSON)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String analysisJson;

    // When analysis was generated
    @Column(nullable = false)
    private Instant createdAt;
}
