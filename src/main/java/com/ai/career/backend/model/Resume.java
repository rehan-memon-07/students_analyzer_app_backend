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

    @Column(nullable = false)
    private UUID userId;

    // Original file name e.g. "rehan_resume_v2.pdf"
    @Column(name = "file_name", nullable = true)
    private String fileName;

    // No longer used — kept for schema compatibility
    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Instant uploadedAt;

    // Version grouping
    @Column(nullable = true)
    private UUID groupId;

    @Column(nullable = true)
    private int versionNumber;

    // SHA256 hash of normalized resume text
    @Column(length = 64)
    private String contentHash;

    // Extracted text saved immediately on upload
    @Column(columnDefinition = "TEXT")
    private String extractedText;
}