package com.almabase.questionnaire.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service
public class AnthropicService {

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    public record AnswerResult(String answer, String citation, String evidenceSnippet, double confidenceScore) {}

    /**
     * Generate an answer using Google Gemini AI grounded in reference documents.
     * Falls back to keyword matching if API key not set or call fails.
     */
    public AnswerResult generateAnswer(String question, List<String> refDocs, List<String> refNames) {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return callGeminiApi(question, refDocs, refNames);
            } catch (Exception e) {
                System.err.println("Gemini API error, falling back to keyword matching: " + e.getMessage());
            }
        }
        return keywordAnswer(question, refDocs, refNames);
    }

    private AnswerResult callGeminiApi(String question, List<String> refDocs, List<String> refNames) throws Exception {
        // Build context from reference documents
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < refDocs.size(); i++) {
            String name = i < refNames.size() ? refNames.get(i) : ("Document " + (i + 1));
            String content = refDocs.get(i);
            if (content.length() > 2500) content = content.substring(0, 2500) + "...";
            context.append("--- ").append(name).append(" ---\n").append(content).append("\n\n");
        }

        String prompt = """
            You are a precise questionnaire-answering assistant.
            Answer the question ONLY using information from the reference documents below.
            If the answer is not found in the documents, respond with exactly: Not found in references.
            
            Respond in this exact JSON format (no markdown, no extra text):
            {
              "answer": "detailed answer here",
              "citation": "exact document filename",
              "evidence_snippet": "short exact quote from document under 100 chars",
              "confidence": 0.85
            }
            
            Reference Documents:
            """ + context + """
            
            Question: """ + question;

        // Build Gemini API request body
        String requestBody = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": %s
                    }
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.1,
                "maxOutputTokens": 1024
              }
            }
            """.formatted(mapper.writeValueAsString(prompt));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error: " + response.statusCode() + " " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        String text = root.path("candidates").get(0)
            .path("content").path("parts").get(0)
            .path("text").asText();

        // Parse JSON response
        try {
            if (text.contains("{")) {
                text = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);
            }
            JsonNode result = mapper.readTree(text);
            String answer = result.path("answer").asText("Not found in references.");
            String citation = result.path("citation").asText("");
            String snippet = result.path("evidence_snippet").asText("");
            double confidence = result.path("confidence").asDouble(0.8);
            return new AnswerResult(answer, citation, snippet, confidence);
        } catch (Exception e) {
            // Return raw text if JSON parsing fails
            return new AnswerResult(text.trim(), "", "", 0.75);
        }
    }

    /**
     * Keyword-based fallback when no API key is set.
     */
    private AnswerResult keywordAnswer(String question, List<String> refDocs, List<String> refNames) {
        String[] stopWords = {"what", "how", "does", "do", "is", "are", "the", "a", "an",
            "of", "in", "to", "for", "and", "or", "your", "please", "describe", "explain",
            "provide", "details", "type", "use", "you", "give", "tell", "list", "which",
            "when", "where", "who", "why", "have", "has", "with", "that", "this", "its",
            "any", "can", "will", "would", "could", "should", "long", "many", "much"};
        Set<String> stopSet = new HashSet<>(Arrays.asList(stopWords));

        String[] words = question.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", " ").split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String w : words) {
            if (!stopSet.contains(w) && w.length() > 2) keywords.add(w);
        }

        if (keywords.isEmpty()) return new AnswerResult("Not found in references.", "", "", 0.0);

        double bestScore = 0.0;
        String bestDoc = "", bestDocName = "";
        for (int i = 0; i < refDocs.size(); i++) {
            String docLower = refDocs.get(i).toLowerCase();
            int matches = 0;
            for (String kw : keywords) if (docLower.contains(kw)) matches++;
            double score = (double) matches / keywords.size();
            if (score > bestScore) {
                bestScore = score;
                bestDoc = refDocs.get(i);
                bestDocName = i < refNames.size() ? refNames.get(i) : ("Document " + (i + 1));
            }
        }

        if (bestScore < 0.15 || bestDoc.isBlank())
            return new AnswerResult("Not found in references.", "", "", 0.0);

        String answer = extractBestParagraph(bestDoc, keywords);
        if (answer.isBlank()) return new AnswerResult("Not found in references.", "", "", 0.0);

        String snippet = buildSnippet(bestDoc, keywords);
        double confidence = Math.min(0.55 + (bestScore * 0.4), 0.92);
        return new AnswerResult(answer, bestDocName, snippet, confidence);
    }

    private String extractBestParagraph(String doc, List<String> keywords) {
        // Score all paragraphs
        String[] paragraphs = doc.split("\\n\\n+");
        String bestParagraph = "";
        int bestParaScore = 0;

        for (String para : paragraphs) {
            String paraLower = para.toLowerCase().trim();
            if (paraLower.length() < 20) continue;
            int score = 0;
            for (String kw : keywords) if (paraLower.contains(kw)) score++;
            // Prefer paragraphs with bullet points (more content-rich)
            if (paraLower.contains("-") || paraLower.contains("•")) score++;
            if (score > bestParaScore) {
                bestParaScore = score;
                bestParagraph = para.trim();
            }
        }

        if (bestParaScore >= 1 && !bestParagraph.isBlank()) {
            String[] lines = bestParagraph.split("\\n");
            StringBuilder result = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.endsWith(":") && line.length() < 60) continue;
                if (line.equals(line.toUpperCase()) && line.length() < 60) continue;
                result.append(line).append(" ");
            }
            String cleaned = result.toString().trim();
            if (cleaned.length() > 700) cleaned = cleaned.substring(0, 700) + "...";
            if (cleaned.length() > 30) return cleaned;
        }

        // Fallback: top 4 most relevant sentences
        String[] sentences = doc.split("(?<=[.!?])\\s+");
        List<String[]> scored = new ArrayList<>();
        for (String sent : sentences) {
            String lower = sent.toLowerCase().trim();
            if (lower.length() < 15) continue;
            int score = 0;
            for (String kw : keywords) if (lower.contains(kw)) score++;
            if (score > 0) scored.add(new String[]{sent.trim(), String.valueOf(score)});
        }
        scored.sort((a, b) -> Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1])));
        StringBuilder answer = new StringBuilder();
        int taken = 0;
        for (String[] item : scored) {
            if (taken >= 4) break;
            answer.append(item[0]).append(" ");
            taken++;
        }
        String result = answer.toString().trim();
        return result.length() > 700 ? result.substring(0, 700) + "..." : result;
    }

    private String buildSnippet(String doc, List<String> keywords) {
        String docLower = doc.toLowerCase();
        for (String kw : keywords) {
            int idx = docLower.indexOf(kw);
            if (idx >= 0) {
                int start = Math.max(0, idx - 40);
                int end = Math.min(doc.length(), idx + 120);
                return "..." + doc.substring(start, end).trim() + "...";
            }
        }
        return "";
    }
}