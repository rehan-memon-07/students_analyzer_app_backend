package com.ai.career.backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class ResumeTextExtractorService {

    /**
     * This method reads a PDF file from disk
     * and extracts plain text from it.
     */
    public String extractTextFromPdf(String filePath) throws IOException {

        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("Resume file not found at path: " + filePath);
        }

        try (PDDocument document = PDDocument.load(file)) {

            PDFTextStripper textStripper = new PDFTextStripper();
            return textStripper.getText(document);

        }
    }
}
