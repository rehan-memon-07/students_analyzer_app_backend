package com.ai.career.backend.repository;

import com.ai.career.backend.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    // Get all resumes of a user
    List<Resume> findByUserId(UUID userId);
}
