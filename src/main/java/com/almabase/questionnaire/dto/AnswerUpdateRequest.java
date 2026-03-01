package com.almabase.questionnaire.dto;

public class AnswerUpdateRequest {
    private Long answerId;
    private String newAnswer;

    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }
    public String getNewAnswer() { return newAnswer; }
    public void setNewAnswer(String newAnswer) { this.newAnswer = newAnswer; }
}
