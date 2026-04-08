package com.ai.career.backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@Service
public class ResumeTextExtractorService {

    // ── Extract from raw bytes (used by upload endpoint) ─────────────────────
    public String extractTextFromBytes(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    // ── Extract from file path (kept for legacy compatibility) ────────────────
    public String extractTextFromPdf(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }
}