package com.almabase.questionnaire.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    public String getUploadDir() { return uploadDir; }
    public String getGeminiApiKey() { return geminiApiKey; }
}