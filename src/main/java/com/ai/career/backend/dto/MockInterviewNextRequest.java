package com.ai.career.backend.dto;

public class MockInterviewNextRequest {

    private String previousQuestion;
    private String answer;
    private int warningCount; // tracks how many AI warnings have been issued so far

    public String getPreviousQuestion() { return previousQuestion; }
    public String getAnswer() { return answer; }
    public int getWarningCount() { return warningCount; }

    public void setPreviousQuestion(String previousQuestion) { this.previousQuestion = previousQuestion; }
    public void setAnswer(String answer) { this.answer = answer; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
}