package com.almabase.questionnaire.service;

import com.almabase.questionnaire.entity.*;
import com.almabase.questionnaire.repository.*;
import com.almabase.questionnaire.service.AnthropicService.AnswerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class QuestionnaireService {

    @Autowired
    private QuestionnaireRunRepository runRepository;

    @Autowired
    private QuestionAnswerRepository answerRepository;

    @Autowired
    private ReferenceDocumentRepository refDocRepository;

    @Autowired
    private DocumentParserService parserService;

    @Autowired
    private AnthropicService anthropicService;

    /**
     * Upload questionnaire, parse it, generate answers from reference docs.
     */
    @Transactional
    public QuestionnaireRun processQuestionnaire(MultipartFile file, User user) throws IOException {
        String rawText = parserService.extractText(file);
        List<String> questions = parserService.parseQuestions(rawText);

        QuestionnaireRun run = new QuestionnaireRun();
        run.setName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "Questionnaire");
        run.setStatus("PROCESSING");
        run.setOriginalQuestionnaire(rawText);
        run.setUser(user);
        run = runRepository.save(run);

        // Get all reference documents for this user
        List<ReferenceDocument> refDocs = refDocRepository.findByUserId(user.getId());
        List<String> docContents = new ArrayList<>();
        List<String> docNames = new ArrayList<>();
        for (ReferenceDocument rd : refDocs) {
            docContents.add(rd.getContent());
            docNames.add(rd.getOriginalFilename() != null ? rd.getOriginalFilename() : rd.getFilename());
        }

        // Generate answers with delay to avoid API rate limits
        List<QuestionAnswer> answers = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            // Wait 5 seconds between questions to respect Gemini free tier rate limits
            if (i > 0) {
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
            AnswerResult result;
            if (docContents.isEmpty()) {
                result = new AnswerResult("Not found in references.", "", "", 0.0);
            } else {
                result = anthropicService.generateAnswer(question, docContents, docNames);
            }

            QuestionAnswer qa = new QuestionAnswer();
            qa.setQuestion(question);
            qa.setAnswer(result.answer());
            qa.setCitation(result.citation());
            qa.setEvidenceSnippet(result.evidenceSnippet());
            qa.setConfidenceScore(result.confidenceScore());
            qa.setQuestionOrder(i + 1);
            qa.setRun(run);
            answers.add(qa);
        }
        answerRepository.saveAll(answers);

        run.setStatus("COMPLETED");
        run.setCompletedAt(LocalDateTime.now());
        return runRepository.save(run);
    }

    public List<QuestionnaireRun> getRunsForUser(User user) {
        return runRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<QuestionnaireRun> findByIdAndUser(Long id, User user) {
        return runRepository.findByIdAndUser(id, user);
    }

    @Transactional
    public QuestionAnswer updateAnswer(Long answerId, String newAnswer) {
        QuestionAnswer qa = answerRepository.findById(answerId)
            .orElseThrow(() -> new RuntimeException("Answer not found: " + answerId));
        qa.setAnswer(newAnswer);
        qa.setEdited(true);
        return answerRepository.save(qa);
    }

    @Transactional
    public void regenerateAnswers(List<Long> answerIds, User user) {
        List<ReferenceDocument> refDocs = refDocRepository.findByUserId(user.getId());
        List<String> docContents = new ArrayList<>();
        List<String> docNames = new ArrayList<>();
        for (ReferenceDocument rd : refDocs) {
            docContents.add(rd.getContent());
            docNames.add(rd.getOriginalFilename() != null ? rd.getOriginalFilename() : rd.getFilename());
        }

        for (Long aid : answerIds) {
            answerRepository.findById(aid).ifPresent(qa -> {
                AnswerResult result = docContents.isEmpty()
                    ? new AnswerResult("Not found in references.", "", "", 0.0)
                    : anthropicService.generateAnswer(qa.getQuestion(), docContents, docNames);
                qa.setAnswer(result.answer());
                qa.setCitation(result.citation());
                qa.setEvidenceSnippet(result.evidenceSnippet());
                qa.setConfidenceScore(result.confidenceScore());
                qa.setEdited(false);
                answerRepository.save(qa);
            });
        }
    }

    @Transactional
    public void deleteRun(Long runId) {
        runRepository.deleteById(runId);
    }

    public Map<String, Object> getCoverageSummary(QuestionnaireRun run) {
        List<QuestionAnswer> answers = run.getAnswers();
        long total = answers.size();
        long cited = answers.stream().filter(a -> a.getCitation() != null && !a.getCitation().isBlank() && !a.getAnswer().contains("Not found")).count();
        long notFound = answers.stream().filter(a -> a.getAnswer() != null && a.getAnswer().contains("Not found")).count();
        long answered = total - notFound;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("answered", answered);
        summary.put("cited", cited);
        summary.put("notFound", notFound);
        summary.put("completionPercent", total > 0 ? (int)(answered * 100 / total) : 0);
        return summary;
    }
}