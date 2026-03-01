package com.almabase.questionnaire.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questionnaire_runs")
public class QuestionnaireRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String status;
    @Column(columnDefinition = "TEXT")
    private String originalQuestionnaire;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<QuestionAnswer> answers = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getOriginalQuestionnaire() { return originalQuestionnaire; }
    public void setOriginalQuestionnaire(String o) { this.originalQuestionnaire = o; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime c) { this.completedAt = c; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public List<QuestionAnswer> getAnswers() { return answers; }
    public void setAnswers(List<QuestionAnswer> a) { this.answers = a; }
}
