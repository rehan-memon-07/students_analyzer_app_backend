package com.ai.career.backend.controller;

import com.ai.career.backend.dto.ApiResponse;
import com.ai.career.backend.model.Resume;
import com.ai.career.backend.model.ResumeAnalysis;
import com.ai.career.backend.repository.ResumeAnalysisRepository;
import com.ai.career.backend.repository.ResumeRepository;
import com.ai.career.backend.service.GeminiService;
import com.ai.career.backend.service.HashService;
import com.ai.career.backend.service.PromptService;
import com.ai.career.backend.service.ResumeTextExtractorService;
import com.ai.career.backend.service.SessionService;
import com.ai.career.backend.service.TextNormalizerService;
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

    // ================================================================
    // UPLOAD — extract text in memory, no file saved to disk
    // ================================================================
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

            // Normalize and hash for deduplication
            String normalizedText = normalizerService.normalize(rawText);
            String contentHash = hashService.generateHash(normalizedText);

            // Return existing record if same resume already uploaded
            Resume existing = resumeRepository
                    .findByUserIdAndContentHash(userId, contentHash)
                    .orElse(null);

            if (existing != null && existing.getExtractedText() != null 
                    && !existing.getExtractedText().isBlank()) {
                return ResponseEntity.ok(new ApiResponse<>(true, Map.of(
                        "resumeId", existing.getId().toString(),
                        "fileName", existing.getFileName() != null
                                ? existing.getFileName() : originalFileName,
                        "isDuplicate", true
                ), null));
            }
// If duplicate exists but has no text, fall through and create fresh record

            // Save new resume record with extracted text immediately
            Resume resume = Resume.builder()
                    .userId(userId)
                    .fileName(originalFileName)
                    .filePath("")
                    .uploadedAt(Instant.now())
                    .contentHash(contentHash)
                    .extractedText(rawText)
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

    // ================================================================
    // EXTRACT TEXT — reads from DB (text already saved on upload)
    // ================================================================
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

        Resume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, null, "RESUME_NOT_FOUND"));
        }

        String extractedText = resume.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ApiResponse<>(false, null, "EXTRACTED_TEXT_MISSING"));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, extractedText, null));
    }

    // ================================================================
    // ANALYZE
    // ================================================================
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

            Resume resume = resumeRepository.findById(resumeId).orElse(null);
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

            // Return cached analysis if available
            ResumeAnalysis cached = resumeAnalysisRepository
                    .findByContentHash(contentHash)
                    .orElse(null);

            if (cached != null) {
                return ResponseEntity.ok(
                        new ApiResponse<>(true, cached.getAnalysisJson(), null));
            }

            // Run Gemini analysis
            String prompt = promptService.resumeAnalysisPrompt(extractedText);
            String analysisJson = extractJson(geminiService.generateResponse(prompt));

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

    // ================================================================
    // COMPARE
    // ================================================================
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
            String comparisonJson = extractJson(geminiService.generateResponse(prompt));

            return ResponseEntity.ok(new ApiResponse<>(true, comparisonJson, null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, null, "COMPARISON_FAILED: " + e.getMessage()));
        }
    }

    // ================================================================
    // JSON CLEANER — strips markdown backticks Gemini sometimes adds
    // ================================================================
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
            throw new RuntimeException(
                "Invalid JSON from Gemini: " +
                raw.substring(0, Math.min(raw.length(), 200))
            );
        }

        return raw.substring(start, end + 1);
    }
}