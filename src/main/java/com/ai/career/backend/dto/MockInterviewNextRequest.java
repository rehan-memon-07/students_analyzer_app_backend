package com.ai.career.backend.dto;

public class MockInterviewNextRequest {

    private String previousQuestion;
    private String answer;

    public String getPreviousQuestion() {
        return previousQuestion;
    }

    public void setPreviousQuestion(String previousQuestion) {
        this.previousQuestion = previousQuestion;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
