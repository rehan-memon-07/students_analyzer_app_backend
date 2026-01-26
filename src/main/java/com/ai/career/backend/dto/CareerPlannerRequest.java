package com.ai.career.backend.dto;

import java.util.UUID;

public class CareerPlannerRequest {
    private String sessionToken;
    private UUID resumeId;
    private String goal;   // "SDE", "Data Scientist", etc.

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public UUID getResumeId() {
        return resumeId;
    }

    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }
}
