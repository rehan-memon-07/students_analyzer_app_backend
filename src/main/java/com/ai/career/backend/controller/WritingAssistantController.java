package com.ai.career.backend.controller;

import com.ai.career.backend.dto.ApiResponse;
import com.ai.career.backend.dto.WritingAssistantRequest;
import com.ai.career.backend.service.GeminiService;
import com.ai.career.backend.service.PromptService;
import com.ai.career.backend.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/writing")
public class WritingAssistantController {

    private final PromptService promptService;
    private final GeminiService geminiService;
    private final SessionService sessionService;

    public WritingAssistantController(
            PromptService promptService,
            GeminiService geminiService,
            SessionService sessionService
    ) {
        this.promptService = promptService;
        this.geminiService = geminiService;
        this.sessionService = sessionService;
    }

    // ===============================
    // ✍️ WRITING ASSISTANT (FINAL)
    // ===============================
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generate(
            @RequestBody WritingAssistantRequest request
    ) {

        // 1️⃣ Validate session
        if (sessionService.getUserFromSession(request.getSessionToken()) == null) {
            return ResponseEntity.ok(
                    new ApiResponse<>(false, null, "INVALID_SESSION")
            );
        }

        try {
            // 2️⃣ Build prompt
            String prompt = promptService.writingAssistantPrompt(
                    request.getTask(),
                    request.getContent()
            );

            // 3️⃣ Call Gemini
            String aiRaw = geminiService.generateResponse(prompt);

            if (aiRaw == null || aiRaw.trim().isEmpty()) {
                return ResponseEntity.ok(
                        new ApiResponse<>(false, null, "AI_EMPTY_RESPONSE")
                );
            }

            // 4️⃣ Normalize response (frontend-safe)
            Map<String, Object> data = new HashMap<>();
            data.put("type", request.getTask());
            data.put("content", aiRaw.trim());

            return ResponseEntity.ok(
                    new ApiResponse<>(true, data, null)
            );

        } catch (Exception e) {
            // 🔥 Never expose 500 to frontend for AI failures
            return ResponseEntity.ok(
                    new ApiResponse<>(false, null, "WRITING_ASSISTANT_FAILED")
            );
        }
    }
}
