package com.ai.career.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    // Firebase UID ya koi bhi external auth ID
    @Column(name = "external_user_id", unique = true, nullable = false)
    private String externalUserId;

    // Optional but useful
    private String email;

    // User kab create hua
    @Column(nullable = false)
    private Instant createdAt;
}
