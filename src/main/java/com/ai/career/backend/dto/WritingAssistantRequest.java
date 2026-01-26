package com.ai.career.backend.dto;

public class WritingAssistantRequest {

    private String sessionToken;
    private String task;      // improve | email | linkedin | cover_letter
    private String content;   // user input text

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
