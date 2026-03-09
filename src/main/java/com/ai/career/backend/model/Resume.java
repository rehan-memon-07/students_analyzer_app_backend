package com.ai.career.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue
    private UUID id;

    // Resume kis user ka hai
    @Column(nullable = false)
    private UUID userId;

    // Original file name
    private String fileName;

    // Server me file ka path
    @Column(nullable = false)
    private String filePath;

    // Upload time
    @Column(nullable = false)
    private Instant uploadedAt;

    // =========================
    // VERSION GROUPING
    // =========================

    // Same resume versions group
    @Column(nullable = true)
    private UUID groupId;

    // Version number (V1, V2, V3...)
    @Column(nullable = true)
    private int versionNumber;

    // =========================
    // HASHING SYSTEM
    // =========================

    // SHA256 hash of normalized resume text
    @Column(length = 64)
    private String contentHash;

    // Extracted text stored for future use
    @Column(columnDefinition = "TEXT")
    private String extractedText;
}