package com.ai.career.backend.controller;

import com.ai.career.backend.dto.MockInterviewNextRequest;
import com.ai.career.backend.dto.MockInterviewStartRequest;
import com.ai.career.backend.dto.MockInterviewEndRequest;
import com.ai.career.backend.model.Resume;
import com.ai.career.backend.repository.ResumeRepository;
import com.ai.career.backend.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mock-interview")
public class MockInterviewController {

    private final ResumeRepository resumeRepository;
    private final SessionService sessionService;
    private final PromptService promptService;
    private final GeminiService geminiService;

    public MockInterviewController(
            ResumeRepository resumeRepository,
            SessionService sessionService,
            PromptService promptService,
            GeminiService geminiService
    ) {
        this.resumeRepository = resumeRepository;
        this.sessionService = sessionService;
        this.promptService = promptService;
        this.geminiService = geminiService;
    }

    // ===============================
    // START MOCK INTERVIEW
    // ===============================
    @PostMapping("/start")
    public Map<String, Object> startInterview(
            @RequestBody MockInterviewStartRequest request
    ) {
        UUID userId = sessionService.getUserFromSession(request.getSessionToken());
        if (userId == null) {
            throw new RuntimeException("INVALID_SESSION");
        }

        Resume resume = resumeRepository
                .findById(request.getResumeId())
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        String resumeText = resume.getExtractedText();
        if (resumeText == null || resumeText.isBlank()) {
            throw new RuntimeException("Resume text not available. Please re-upload your resume.");
        }

        String prompt = promptService.mockInterviewStartPrompt(resumeText, request.getRole());
        String firstQuestion = geminiService.generateResponse(prompt);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", UUID.randomUUID().toString());
        response.put("firstQuestion", firstQuestion.trim());

        return response;
    }

    // ===============================
    // NEXT QUESTION
    // ===============================
    @PostMapping("/next")
    public Map<String, Object> nextQuestion(
            @RequestBody MockInterviewNextRequest request
    ) {
        String prompt = promptService.mockInterviewFollowUpPrompt(
                request.getPreviousQuestion(),
                request.getAnswer(),
                request.getWarningCount()
        );

        String raw = geminiService.generateResponse(prompt);

        // Parse all 4 fields from response
        String feedback            = extractField(raw, "[Feedback]:");
        String difficultyAdjustment = extractField(raw, "[Difficulty Adjustment]:");
        String aiDetectedStr       = extractField(raw, "[AI Detected]:");
        String nextQuestion        = extractField(raw, "[Next Question]:");

        // Fallbacks
        if (nextQuestion.isBlank()) nextQuestion = raw.trim();
        if (difficultyAdjustment.isBlank()) difficultyAdjustment = "Maintained";

        boolean aiDetected = aiDetectedStr.toLowerCase().contains("true");

        Map<String, Object> response = new HashMap<>();
        response.put("nextQuestion", nextQuestion);
        response.put("feedback", feedback);
        response.put("difficultyAdjustment", difficultyAdjustment);
        response.put("aiDetected", aiDetected);

        return response;
    }

    // ===============================
    // END INTERVIEW
    // ===============================
    @PostMapping("/end")
    public Map<String, Object> endInterview(
            @RequestBody MockInterviewEndRequest request
    ) {
        UUID userId = sessionService.getUserFromSession(request.getSessionToken());
        if (userId == null) {
            throw new RuntimeException("INVALID_SESSION");
        }

        if (request.getConversation() == null || request.getConversation().isEmpty()) {
            throw new RuntimeException("EMPTY_CONVERSATION");
        }

        // Get total AI warnings from request (default 0 if not provided)
        int totalAiWarnings = request.getTotalAiWarnings();

        String prompt = promptService.mockInterviewEndPrompt(
                request.getConversation(),
                request.getRole(),
                totalAiWarnings
        );

        String evaluation = geminiService.generateResponse(prompt);

        Map<String, Object> response = new HashMap<>();
        response.put("evaluation", evaluation);
        response.put("status", "INTERVIEW_COMPLETED");

        return response;
    }

    // ===============================
    // HELPER — extract field from Gemini response
    // ===============================
    private String extractField(String raw, String fieldKey) {
        int start = raw.indexOf(fieldKey);
        if (start == -1) return "";

        int valueStart = start + fieldKey.length();
        int nextField = raw.indexOf("\n[", valueStart);
        String value = nextField != -1
                ? raw.substring(valueStart, nextField)
                : raw.substring(valueStart);

        return value.trim();
    }
}