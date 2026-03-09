package com.ai.career.backend.repository;

import com.ai.career.backend.model.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, UUID> {

    Optional<ResumeAnalysis> findByContentHash(String contentHash);

}
