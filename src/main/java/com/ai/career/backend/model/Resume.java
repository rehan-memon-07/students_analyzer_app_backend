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
}
