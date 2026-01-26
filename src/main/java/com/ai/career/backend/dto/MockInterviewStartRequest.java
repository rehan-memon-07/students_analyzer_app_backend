package com.ai.career.backend.dto;

import java.util.UUID;

public class MockInterviewStartRequest {

    private UUID resumeId;
    private String role;
    private String sessionToken;

    // getters
    public UUID getResumeId() {
        return resumeId;
    }

    public String getRole() {
        return role;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    // setters (IMPORTANT for @RequestBody)
    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}
