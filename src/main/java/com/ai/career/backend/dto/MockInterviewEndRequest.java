package com.ai.career.backend.dto;

import java.util.List;

public class MockInterviewEndRequest {

    private String sessionToken;
    private String role;
    private List<QA> conversation;

    public static class QA {
        private String question;
        private String answer;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<QA> getConversation() {
        return conversation;
    }

    public void setConversation(List<QA> conversation) {
        this.conversation = conversation;
    }
}