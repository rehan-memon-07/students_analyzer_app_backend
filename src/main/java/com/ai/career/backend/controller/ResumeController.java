package com.ai.career.backend.controller;

import com.ai.career.backend.dto.ApiResponse;
import com.ai.career.backend.model.Resume;
import com.ai.career.backend.model.ResumeAnalysis;
import com.ai.career.backend.repository.ResumeRepository;
import com.ai.career.backend.repository.ResumeAnalysisRepository;
import com.ai.career.backend.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final SessionService sessionService;
    private final ResumeTextExtractorService textExtractorService;
    private final GeminiService geminiService;
    private final PromptService promptService;
    private final TextNormalizerService textNormalizerService;
    private final HashService hashService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeController(
            ResumeRepository resumeRepository,
            ResumeAnalysisRepository resumeAnalysisRepository,
            SessionService sessionService,
            ResumeTextExtractorService textExtractorService,
            GeminiService geminiService,
            PromptService promptService,
            TextNormalizerService textNormalizerService,
            HashService hashService) {

        this.resumeRepository = resumeRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.sessionService = sessionService;
        this.textExtractorService = textExtractorService;
        this.geminiService = geminiService;
        this.promptService = promptService;
        this.textNormalizerService = textNormalizerService;
        this.hashService = hashService;
    }

    // =========================
    // UPLOAD RESUME (UNCHANGED)
    // =========================
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionToken") String sessionToken) throws Exception {

        UUID userId = sessionService.getUserFromSession(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, null, "INVALID_SESSION"));
        }

        File uploadDir = new File("/tmp/uploads");
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File destinationFile = new File(uploadDir, storedFileName);
        file.transferTo(destinationFile);

        Resume resume = Resume.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .filePath(destinationFile.getAbsolutePath())
                .uploadedAt(Instant.now())
                .build();

        Resume saved = resumeRepository.save(resume);

        return ResponseEntity.ok(
                new ApiResponse<>(true, saved.getId().toString(), null));
    }

    // =========================
    // EXTRACT TEXT (UNCHANGED)
    // =========================
    @PostMapping("/extract-text")
    public ResponseEntity<ApiResponse<String>> extractResumeText(
            @RequestParam UUID resumeId,
            @RequestParam String sessionToken) throws Exception {

        UUID userId = sessionService.getUserFromSession(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, null, "INVALID_SESSION"));
        }

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        if (!resume.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, null, "NOT_ALLOWED"));
        }

        String extractedText =
                textExtractorService.extractTextFromPdf(resume.getFilePath());

        return ResponseEntity.ok(
                new ApiResponse<>(true, extractedText, null));
    }

    // =========================
    // ANALYZE RESUME (UPDATED WITH SHA256 CACHE)
    // =========================
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeResume(
            @RequestParam UUID resumeId,
            @RequestParam String sessionToken) {

        UUID userId = sessionService.getUserFromSession(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, null, "INVALID_SESSION"));
        }

        Resume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null || !resume.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, null, "NOT_ALLOWED"));
        }

        try {

            // 1️⃣ Extract text
            String resumeText =
                    textExtractorService.extractTextFromPdf(resume.getFilePath());

            if (resumeText == null || resumeText.trim().length() < 50) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, null, "RESUME_TEXT_TOO_SHORT"));
            }

            // 2️⃣ Normalize text
            String normalizedText =
                    textNormalizerService.normalize(resumeText);

            // 3️⃣ Generate SHA256 hash
            String contentHash =
                    hashService.generateHash(normalizedText);

            // Save extractedText and contentHash back to resume
            resume.setExtractedText(resumeText);
            resume.setContentHash(contentHash);
            resumeRepository.save(resume);

            // 4️⃣ Check cached analysis
            Optional<ResumeAnalysis> cached =
                resumeAnalysisRepository.findByContentHash(contentHash);

                if (cached.isPresent()) {

                        Map<String, Object> cachedResult =
                                objectMapper.readValue(
                                        cached.get().getAnalysisJson(),
                                        new TypeReference<>() {}
                                );

                        return ResponseEntity.ok(
                                new ApiResponse<>(true, cachedResult, null));
                }

            // 5️⃣ Build prompt
            String prompt =
                    promptService.resumeAnalysisPrompt(resumeText);

            // 6️⃣ Call Gemini
            String geminiRaw =
                    geminiService.generateResponse(prompt);

            if (geminiRaw == null || geminiRaw.isBlank()) {
                throw new RuntimeException("Gemini returned empty response");
            }

            // 7️⃣ Extract JSON safely
            String jsonOnly =
                    extractJson(geminiRaw);

            // 8️⃣ Parse JSON
            Map<String, Object> parsed =
                    objectMapper.readValue(jsonOnly, new TypeReference<>() {});

            // 9️⃣ Save analysis
            ResumeAnalysis analysis =
                    ResumeAnalysis.builder()
                            .contentHash(contentHash)
                            .analysisJson(jsonOnly)
                            .createdAt(Instant.now())
                            .build();

            resumeAnalysisRepository.save(analysis);

            // 🔟 Return result
            return ResponseEntity.ok(
                    new ApiResponse<>(true, parsed, null));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, e.getMessage()));
        }
    }

    // =========================
    // JSON CLEANER (UNCHANGED)
    // =========================
    private String extractJson(String raw) {
        raw = raw.trim();

        if (raw.startsWith("```")) {
            raw = raw.replaceAll("```json", "")
                     .replaceAll("```", "")
                     .trim();
        }

        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');

        if (start < 0 || end < 0 || end <= start) {
            throw new RuntimeException("Invalid JSON from Gemini");
        }

        return raw.substring(start, end + 1);
    }
}