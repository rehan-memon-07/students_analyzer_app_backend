package com.ai.career.backend.dto;

import java.util.UUID;

public class ResumeCompareRequest {

    private UUID resumeIdA;
    private UUID resumeIdB;
    private String sessionToken;

    public UUID getResumeIdA() { return resumeIdA; }
    public UUID getResumeIdB() { return resumeIdB; }
    public String getSessionToken() { return sessionToken; }

    public void setResumeIdA(UUID resumeIdA) { this.resumeIdA = resumeIdA; }
    public void setResumeIdB(UUID resumeIdB) { this.resumeIdB = resumeIdB; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}