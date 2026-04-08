package com.ai.career.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ai.career.backend.model.Resume;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findByUserIdAndContentHash(UUID userId, String contentHash);

}