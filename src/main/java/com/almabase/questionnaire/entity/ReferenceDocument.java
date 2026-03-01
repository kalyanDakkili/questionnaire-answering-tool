package com.almabase.questionnaire.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reference_documents")
public class ReferenceDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String filename;
    private String originalFilename;
    private String contentType;
    @Column(columnDefinition = "TEXT")
    private String content;
    private LocalDateTime uploadedAt = LocalDateTime.now();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String f) { this.filename = f; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String f) { this.originalFilename = f; }
    public String getContentType() { return contentType; }
    public void setContentType(String c) { this.contentType = c; }
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime u) { this.uploadedAt = u; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
}
