package com.ai.career.backend.controller;

import com.ai.career.backend.dto.MockInterviewNextRequest;
import com.ai.career.backend.dto.MockInterviewStartRequest;
import com.ai.career.backend.model.Resume;
import com.ai.career.backend.repository.ResumeRepository;
import com.ai.career.backend.service.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mock-interview")
public class MockInterviewController {

    private final ResumeRepository resumeRepository;
    private final SessionService sessionService;
    private final ResumeTextExtractorService extractorService;
    private final PromptService promptService;
    private final GeminiService geminiService;

    public MockInterviewController(
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
    // START MOCK INTERVIEW
    // ===============================
    @PostMapping("/start")
    public Map<String, Object> startInterview(
            @RequestBody MockInterviewStartRequest request
    ) throws IOException {

        UUID userId = sessionService.getUserFromSession(request.getSessionToken());
        if (userId == null) {
            throw new RuntimeException("INVALID_SESSION");
        }

        Resume resume = resumeRepository
                .findById(request.getResumeId())
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        String resumeText = extractorService
                .extractTextFromPdf(resume.getFilePath());

        String prompt = promptService
                .mockInterviewStartPrompt(resumeText, request.getRole());

        String firstQuestion = geminiService.generateResponse(prompt);

        // ✅ Return JSON instead of raw text
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", UUID.randomUUID().toString());
        response.put("firstQuestion", firstQuestion);

        return response;
    }

    // ===============================
    // NEXT QUESTION
    // ===============================
    @PostMapping("/next")
    public Map<String, Object> nextQuestion(
            @RequestBody MockInterviewNextRequest request
    ) {

        String prompt = promptService
                .mockInterviewFollowUpPrompt(
                        request.getPreviousQuestion(),
                        request.getAnswer()
                );

        String nextQuestion = geminiService.generateResponse(prompt);

        // ✅ Return JSON
        Map<String, Object> response = new HashMap<>();
        response.put("nextQuestion", nextQuestion);

        return response;
    }
}
