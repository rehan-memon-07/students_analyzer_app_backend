package com.ai.career.backend.controller;

import com.ai.career.backend.dto.ApiResponse;
import com.ai.career.backend.model.Resume;
import com.ai.career.backend.model.ResumeAnalysis;
import com.ai.career.backend.repository.ResumeAnalysisRepository;
import com.ai.career.backend.repository.ResumeRepository;
import com.ai.career.backend.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ResumeTextExtractorService textExtractorService;
    private final HashService hashService;
    private final TextNormalizerService normalizerService;
    private final GeminiService geminiService;
    private final PromptService promptService;
    private final SessionService sessionService;

    public ResumeController(
            ResumeRepository resumeRepository,
            ResumeAnalysisRepository resumeAnalysisRepository,
            ResumeTextExtractorService textExtractorService,
            HashService hashService,
            TextNormalizerService normalizerService,
            GeminiService geminiService,
            PromptService promptService,
            SessionService sessionService
    ) {
        this.resumeRepository = resumeRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.textExtractorService = textExtractorService;
        this.hashService = hashService;
        this.normalizerService = normalizerService;
        this.geminiService = geminiService;
        this.promptService = promptService;
        this.sessionService = sessionService;
    }

    // ============================================================
    // UPLOAD — Extract text in memory, no file saved to disk
    // ============================================================
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionToken") String sessionToken
    ) {
        try {
            UUID userId = sessionService.getUserFromSession(sessionToken);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, null, "INVALID_SESSION"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, null, "EMPTY_FILE"));
            }

            String originalFileName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "resume.pdf";

            // Extract text directly from bytes — no disk write
            byte[] fileBytes = file.getBytes();
            String rawText = textExtractorService.extractTextFromBytes(fileBytes);

            if (rawText == null || rawText.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, null, "EMPTY_RESUME_TEXT"));
            }

            // Hash for caching
            String normalizedText = normalizerService.normalize(rawText);
            String contentHash = hashService.sha256(normalizedText);

            // Check for existing resume with same hash for this user
            Resume existing = resumeRepository
                    .findByUserIdAndContentHash(userId, contentHash)
                    .orElse(null);

            if (existing != null) {
                // Duplicate resume — return existing record
                return ResponseEntity.ok(new ApiResponse<>(true, Map.of(
                        "resumeId", existing.getId().toString(),
                        "fileName", existing.getFileName() != null
                                ? existing.getFileName() : originalFileName,
                        "isDuplicate", true
                ), null));
            }

            // Save resume record
            Resume resume = Resume.builder()
                    .userId(userId)
                    .fileName(originalFileName)
                    .filePath("")           // no longer used
                    .uploadedAt(Instant.now())
                    .contentHash(contentHash)
                    .extractedText(rawText) // saved immediately
                    .build();

            resumeRepository.save(resume);

            return ResponseEntity.ok(new ApiResponse<>(true, Map.of(
                    "resumeId", resume.getId().toString(),
                    "fileName", originalFileName,
                    "isDuplicate", false
            ), null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "UPLOAD_FAILED: " + e.getMessage()));
        }
    }

    // ============================================================
    // EXTRACT TEXT — now just reads from DB (already saved on upload)
    // ============================================================
    @PostMapping("/extract-text")
    public ResponseEntity<ApiResponse<String>> extractResumeText(
            @RequestParam UUID resumeId,
            @RequestParam String sessionToken
    ) {
        UUID userId = sessionService.getUserFromSession(sessionToken);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, null, "INVALID_SESSION"));
        }

        Resume resume = resumeRepository.findById(resumeId)
                .orElse(null);
        if (resume == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, null, "RESUME_NOT_FOUND"));
        }

        String extractedText = resume.getExtractedText();

        // Fallback: if somehow text is missing, return error clearly
        if (extractedText == null || extractedText.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ApiResponse<>(false, null, "EXTRACTED_TEXT_MISSING"));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, extractedText, null));
    }

    // ============================================================
    // ANALYZE
    // ============================================================
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<String>> analyzeResume(
            @RequestParam UUID resumeId,
            @RequestParam String sessionToken
    ) {
        try {
            UUID userId = sessionService.getUserFromSession(sessionToken);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, null, "INVALID_SESSION"));
            }

            Resume resume = resumeRepository.findById(resumeId)
                    .orElse(null);
            if (resume == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, null, "RESUME_NOT_FOUND"));
            }

            String extractedText = resume.getExtractedText();
            if (extractedText == null || extractedText.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(new ApiResponse<>(false, null, "EXTRACTED_TEXT_MISSING"));
            }

            String contentHash = resume.getContentHash();

            // Check cache
            ResumeAnalysis cached = resumeAnalysisRepository
                    .findByContentHash(contentHash)
                    .orElse(null);

            if (cached != null) {
                return ResponseEntity.ok(
                        new ApiResponse<>(true, cached.getAnalysisJson(), null));
            }

            // Run Gemini analysis
            String prompt = promptService.resumeAnalysisPrompt(extractedText);
            String analysisJson = geminiService.generateResponse(prompt);

            // Cache result
            ResumeAnalysis analysis = ResumeAnalysis.builder()
                    .contentHash(contentHash)
                    .analysisJson(analysisJson)
                    .createdAt(Instant.now())
                    .build();
            resumeAnalysisRepository.save(analysis);

            return ResponseEntity.ok(new ApiResponse<>(true, analysisJson, null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "ANALYSIS_FAILED: " + e.getMessage()));
        }
    }

    // ============================================================
    // COMPARE
    // ============================================================
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<String>> compareResumes(
            @RequestParam UUID resumeIdA,
            @RequestParam UUID resumeIdB,
            @RequestParam String sessionToken
    ) {
        try {
            UUID userId = sessionService.getUserFromSession(sessionToken);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, null, "INVALID_SESSION"));
            }

            Resume resumeA = resumeRepository.findById(resumeIdA).orElse(null);
            Resume resumeB = resumeRepository.findById(resumeIdB).orElse(null);

            if (resumeA == null || resumeB == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, null, "RESUME_NOT_FOUND"));
            }

            String textA = resumeA.getExtractedText();
            String textB = resumeB.getExtractedText();

            if (textA == null || textA.isBlank() || textB == null || textB.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(new ApiResponse<>(false, null, "EXTRACTED_TEXT_MISSING"));
            }

            String prompt = promptService.resumeComparisonPrompt(textA, textB);
            String comparisonJson = geminiService.generateResponse(prompt);

            return ResponseEntity.ok(new ApiResponse<>(true, comparisonJson, null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "COMPARISON_FAILED: " + e.getMessage()));
        }
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