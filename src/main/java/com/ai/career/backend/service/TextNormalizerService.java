package com.ai.career.backend.service;

import org.springframework.stereotype.Service;

@Service
public class TextNormalizerService {

    public String normalize(String text) {

        if (text == null) {
            return "";
        }

        String normalized = text.toLowerCase();

        // Remove punctuation
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");

        // Collapse multiple spaces
        normalized = normalized.replaceAll("\\s+", " ");

        // Trim leading and trailing spaces
        normalized = normalized.trim();

        return normalized;
    }
}
