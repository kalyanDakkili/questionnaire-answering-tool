package com.almabase.questionnaire.service;

import com.almabase.questionnaire.entity.*;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Service
public class ExportService {

    /**
     * Export questionnaire run as a DOCX document.
     * Preserves structure: Question followed by Answer + Citation.
     */
    public byte[] exportToDocx(QuestionnaireRun run) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            // Title
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Questionnaire: " + run.getName());
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("Calibri");

            // Metadata
            XWPFParagraph meta = doc.createParagraph();
            XWPFRun metaRun = meta.createRun();
            metaRun.setText("Generated: " + (run.getCompletedAt() != null ? run.getCompletedAt().toString() : "N/A"));
            metaRun.setFontSize(10);
            metaRun.setColor("666666");
            metaRun.setFontFamily("Calibri");

            // Coverage Summary
            List<QuestionAnswer> answers = run.getAnswers();
            long total = answers.size();
            long notFound = answers.stream().filter(a -> a.getAnswer() != null && a.getAnswer().contains("Not found")).count();
            long answered = total - notFound;

            XWPFParagraph summaryHeader = doc.createParagraph();
            XWPFRun summaryRun = summaryHeader.createRun();
            summaryRun.setText(String.format("Coverage: %d/%d questions answered | %d not found in references", answered, total, notFound));
            summaryRun.setFontSize(11);
            summaryRun.setFontFamily("Calibri");
            summaryRun.setColor("1F4E79");

            // Divider paragraph
            doc.createParagraph();

            // Questions & Answers
            for (QuestionAnswer qa : answers) {
                // Question number and text
                XWPFParagraph qPara = doc.createParagraph();
                qPara.setSpacingBefore(200);
                XWPFRun qNum = qPara.createRun();
                qNum.setText("Q" + qa.getQuestionOrder() + ". ");
                qNum.setBold(true);
                qNum.setFontSize(12);
                qNum.setFontFamily("Calibri");
                qNum.setColor("1F4E79");

                XWPFRun qText = qPara.createRun();
                qText.setText(qa.getQuestion() != null ? qa.getQuestion() : "");
                qText.setBold(true);
                qText.setFontSize(12);
                qText.setFontFamily("Calibri");
                qText.setColor("1F4E79");

                // Answer
                XWPFParagraph aPara = doc.createParagraph();
                aPara.setIndentationLeft(400);
                XWPFRun aLabel = aPara.createRun();
                aLabel.setText("Answer: ");
                aLabel.setBold(true);
                aLabel.setFontSize(11);
                aLabel.setFontFamily("Calibri");

                XWPFRun aText = aPara.createRun();
                aText.setText(qa.getAnswer() != null ? qa.getAnswer() : "Not found in references.");
                aText.setFontSize(11);
                aText.setFontFamily("Calibri");
                if (qa.getAnswer() != null && qa.getAnswer().contains("Not found")) {
                    aText.setColor("CC0000");
                }

                // Confidence
                if (qa.getConfidenceScore() != null && qa.getConfidenceScore() > 0) {
                    XWPFParagraph confPara = doc.createParagraph();
                    confPara.setIndentationLeft(400);
                    XWPFRun confRun = confPara.createRun();
                    int pct = (int)(qa.getConfidenceScore() * 100);
                    confRun.setText("Confidence: " + pct + "%");
                    confRun.setFontSize(10);
                    confRun.setFontFamily("Calibri");
                    confRun.setColor("888888");
                    confRun.setItalic(true);
                }

                // Citation
                if (qa.getCitation() != null && !qa.getCitation().isBlank()) {
                    XWPFParagraph citePara = doc.createParagraph();
                    citePara.setIndentationLeft(400);
                    XWPFRun citeLabel = citePara.createRun();
                    citeLabel.setText("Citation: ");
                    citeLabel.setBold(true);
                    citeLabel.setFontSize(10);
                    citeLabel.setFontFamily("Calibri");
                    citeLabel.setColor("2E74B5");

                    XWPFRun citeText = citePara.createRun();
                    citeText.setText(qa.getCitation());
                    citeText.setFontSize(10);
                    citeText.setFontFamily("Calibri");
                    citeText.setColor("2E74B5");
                    citeText.setItalic(true);
                }

                // Evidence snippet
                if (qa.getEvidenceSnippet() != null && !qa.getEvidenceSnippet().isBlank()) {
                    XWPFParagraph evPara = doc.createParagraph();
                    evPara.setIndentationLeft(400);
                    XWPFRun evLabel = evPara.createRun();
                    evLabel.setText("Evidence: ");
                    evLabel.setBold(true);
                    evLabel.setFontSize(10);
                    evLabel.setFontFamily("Calibri");
                    evLabel.setColor("595959");

                    XWPFRun evText = evPara.createRun();
                    String snippet = qa.getEvidenceSnippet();
                    if (snippet.length() > 200) snippet = snippet.substring(0, 200) + "...";
                    evText.setText(snippet);
                    evText.setFontSize(10);
                    evText.setFontFamily("Calibri");
                    evText.setColor("595959");
                    evText.setItalic(true);
                }

                // Edited indicator
                if (Boolean.TRUE.equals(qa.getEdited())) {
                    XWPFParagraph editPara = doc.createParagraph();
                    editPara.setIndentationLeft(400);
                    XWPFRun editRun = editPara.createRun();
                    editRun.setText("[Manually edited]");
                    editRun.setFontSize(9);
                    editRun.setColor("AA6600");
                    editRun.setFontFamily("Calibri");
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }
}
