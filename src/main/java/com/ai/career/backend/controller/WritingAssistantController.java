package com.ai.career.backend.controller;

import com.ai.career.backend.dto.WritingAssistantRequest;
import com.ai.career.backend.service.*;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public String handleWritingTask(
            @RequestBody WritingAssistantRequest request
    ) {

        // session validation
        if (sessionService.getUserFromSession(request.getSessionToken()) == null) {
            return "INVALID_SESSION";
        }

        String prompt = promptService.writingAssistantPrompt(
                request.getTask(),
                request.getContent()
        );

        return geminiService.generateResponse(prompt);
    }
}
