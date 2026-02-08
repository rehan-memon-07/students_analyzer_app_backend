package com.ai.career.backend.controller;

import com.ai.career.backend.dto.CareerPlannerRequest;
import com.ai.career.backend.model.Resume;
import com.ai.career.backend.repository.ResumeRepository;
import com.ai.career.backend.service.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/career")
public class CareerPlannerController {

    private final ResumeRepository resumeRepository;
    private final SessionService sessionService;
    private final ResumeTextExtractorService extractorService;
    private final PromptService promptService;
    private final GeminiService geminiService;

    public CareerPlannerController(
            ResumeRepository resumeRepository,
            SessionService sessionService,
            ResumeTextExtractorService extractorService,
            PromptService promptService,
            GeminiService geminiService
    ) {
        this.resumeRepository = resumeRepository;
        this.sessionService = sessionService;
        this.extractorService = extractorService;
        this.promptService = promptService;
        this.geminiService = geminiService;
    }

    // ===============================
    // CAREER PLANNER
    // ===============================
    @PostMapping("/plan")
    public String planCareer(
            @RequestBody CareerPlannerRequest request
    ) throws IOException {

        UUID userId = sessionService.getUserFromSession(request.getSessionToken());
        if (userId == null) {
            return "INVALID_SESSION";
        }

        Resume resume = resumeRepository
                .findById(request.getResumeId())
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        String resumeText = extractorService
                .extractTextFromPdf(resume.getFilePath());

        String prompt = promptService
                .careerPlannerPrompt(resumeText);

        return geminiService.generateResponse(prompt);
    }
}