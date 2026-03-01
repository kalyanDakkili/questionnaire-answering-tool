package com.almabase.questionnaire.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question_answers")
public class QuestionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String question;
    @Column(columnDefinition = "TEXT")
    private String answer;
    @Column(columnDefinition = "TEXT")
    private String citation;
    @Column(columnDefinition = "TEXT")
    private String evidenceSnippet;
    private Double confidenceScore;
    private Integer questionOrder;
    private Boolean edited = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private QuestionnaireRun run;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String q) { this.question = q; }
    public String getAnswer() { return answer; }
    public void setAnswer(String a) { this.answer = a; }
    public String getCitation() { return citation; }
    public void setCitation(String c) { this.citation = c; }
    public String getEvidenceSnippet() { return evidenceSnippet; }
    public void setEvidenceSnippet(String e) { this.evidenceSnippet = e; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double c) { this.confidenceScore = c; }
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer o) { this.questionOrder = o; }
    public Boolean getEdited() { return edited; }
    public void setEdited(Boolean e) { this.edited = e; }
    public QuestionnaireRun getRun() { return run; }
    public void setRun(QuestionnaireRun r) { this.run = r; }
}
