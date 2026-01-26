package com.ai.career.backend.controller;

import com.ai.career.backend.model.Resume;
import com.ai.career.backend.repository.ResumeRepository;
import com.ai.career.backend.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final SessionService sessionService;
    private final ResumeTextExtractorService textExtractorService;
    private final GeminiService geminiService;
    private final PromptService promptService;

    // 👉 Constructor Injection
    public ResumeController(
            ResumeRepository resumeRepository,
            SessionService sessionService,
            ResumeTextExtractorService textExtractorService,
            GeminiService geminiService,
            PromptService promptService
    ) {
        this.resumeRepository = resumeRepository;
        this.sessionService = sessionService;
        this.textExtractorService = textExtractorService;
        this.geminiService = geminiService;
        this.promptService = promptService;
    }

    // ===============================
    // UPLOAD RESUME
    // ===============================
    @PostMapping("/upload")
    public String uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionToken") String sessionToken
    ) throws IOException {

        UUID userId = sessionService.getUserFromSession(sessionToken);
        if (userId == null) {
            return "INVALID_SESSION";
        }

        File uploadDir = new File("E:/Resume analyzer/uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File destinationFile = new File(uploadDir, storedFileName);
        file.transferTo(destinationFile);

        Resume resume = Resume.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .filePath(destinationFile.getAbsolutePath())
                .uploadedAt(Instant.now())
                .build();

        Resume savedResume = resumeRepository.save(resume);

        return savedResume.getId().toString();
    }

    // ===============================
    // EXTRACT TEXT
    // ===============================
    @PostMapping("/extract-text")
    public String extractResumeText(
            @RequestParam("resumeId") UUID resumeId,
            @RequestParam("sessionToken") String sessionToken
    ) throws IOException {

        UUID userId = sessionService.getUserFromSession(sessionToken);
        if (userId == null) {
            return "INVALID_SESSION";
        }

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        if (!resume.getUserId().equals(userId)) {
            return "NOT_ALLOWED";
        }

        return textExtractorService.extractTextFromPdf(resume.getFilePath());
    }

    // ===============================
    // RESUME ANALYZER (GEMINI)
    // ===============================
    @PostMapping("/analyze")
    public String analyzeResume(
            @RequestParam("resumeId") UUID resumeId,
            @RequestParam("sessionToken") String sessionToken
    ) throws IOException {

        // 1. Validate session
        UUID userId = sessionService.getUserFromSession(sessionToken);
        if (userId == null) {
            return "INVALID_SESSION";
        }

        // 2. Fetch resume
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        // 3. Ownership check
        if (!resume.getUserId().equals(userId)) {
            return "NOT_ALLOWED";
        }

        // 4. Extract resume text
        String resumeText = textExtractorService
                .extractTextFromPdf(resume.getFilePath());

        // 5. Build Gemini prompt
        String prompt = promptService.resumeAnalysisPrompt(resumeText);

        // 6. Call Gemini
        return geminiService.generateResponse(prompt);
    }
}
