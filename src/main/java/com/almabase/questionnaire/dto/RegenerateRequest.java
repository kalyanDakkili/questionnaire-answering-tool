package com.almabase.questionnaire.dto;

import java.util.List;

public class RegenerateRequest {
    private List<Long> answerIds;

    public List<Long> getAnswerIds() { return answerIds; }
    public void setAnswerIds(List<Long> answerIds) { this.answerIds = answerIds; }
}
