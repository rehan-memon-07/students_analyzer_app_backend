package com.ai.career.backend.service;

import com.ai.career.backend.dto.GeminiRequest;
import com.ai.career.backend.dto.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateResponse(String prompt) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key missing");
        }

        GeminiRequest.Part part = new GeminiRequest.Part(prompt);
        GeminiRequest.Content content = new GeminiRequest.Content(List.of(part));
        GeminiRequest request = new GeminiRequest(List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                apiUrl + "?key=" + apiKey,
                HttpMethod.POST,
                entity,
                GeminiResponse.class
        );

        if (response.getBody() == null ||
                response.getBody().getCandidates() == null ||
                response.getBody().getCandidates().isEmpty()) {
            throw new RuntimeException("Gemini returned no candidates");
        }

        String text = response.getBody()
                .getCandidates()
                .get(0)
                .getContent()
                .getParts()
                .get(0)
                .getText();

        if (text == null || text.isBlank()) {
            throw new RuntimeException("Gemini returned empty text");
        }

        return text;
    }
}
