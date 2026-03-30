package com.ai.career.backend.dto;

import java.util.List;

public class MockInterviewEndRequest {

    private String sessionToken;
    private List<QA> conversation;
    private String role;
    private int totalAiWarnings; // total AI detection warnings across session

    public String getSessionToken() { return sessionToken; }
    public List<QA> getConversation() { return conversation; }
    public String getRole() { return role; }
    public int getTotalAiWarnings() { return totalAiWarnings; }

    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public void setConversation(List<QA> conversation) { this.conversation = conversation; }
    public void setRole(String role) { this.role = role; }
    public void setTotalAiWarnings(int totalAiWarnings) { this.totalAiWarnings = totalAiWarnings; }

    public static class QA {
        private String question;
        private String answer;

        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public void setQuestion(String question) { this.question = question; }
        public void setAnswer(String answer) { this.answer = answer; }
    }
}