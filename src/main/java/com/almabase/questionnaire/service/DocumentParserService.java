package com.almabase.questionnaire.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentParserService {

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".pdf")) {
            return extractFromPdf(file.getInputStream());
        } else if (filename.endsWith(".docx")) {
            return extractFromDocx(file.getInputStream());
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return extractFromExcel(file.getInputStream());
        } else if (filename.endsWith(".txt")) {
            return new String(file.getBytes());
        }
        return new String(file.getBytes());
    }

    private String extractFromPdf(InputStream is) throws IOException {
        try (PDDocument doc = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractFromDocx(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        sb.append(cell.getText()).append("\t");
                    }
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
    }

    private String extractFromExcel(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(cell.toString()).append("\t");
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Parse questionnaire text into individual questions.
     * Detects numbered lists (1., 2., Q1., Q1:) and line-based questions.
     */
    public List<String> parseQuestions(String text) {
        List<String> questions = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder currentQuestion = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                if (currentQuestion.length() > 0) {
                    String q = currentQuestion.toString().trim();
                    if (looksLikeQuestion(q)) {
                        questions.add(q);
                    }
                    currentQuestion = new StringBuilder();
                }
                continue;
            }
            // Numbered pattern: "1.", "Q1.", "Q1:", "1)"
            if (line.matches("^(Q?\\d+[.):]|\\*|-)\\s+.*") || isStandaloneQuestion(line)) {
                if (currentQuestion.length() > 0) {
                    String q = currentQuestion.toString().trim();
                    if (looksLikeQuestion(q)) {
                        questions.add(q);
                    }
                    currentQuestion = new StringBuilder();
                }
                currentQuestion.append(line);
            } else {
                if (currentQuestion.length() > 0) {
                    currentQuestion.append(" ").append(line);
                } else if (looksLikeQuestion(line)) {
                    currentQuestion.append(line);
                }
            }
        }
        if (currentQuestion.length() > 0) {
            String q = currentQuestion.toString().trim();
            if (looksLikeQuestion(q)) {
                questions.add(q);
            }
        }

        // Fallback: split by question marks if no questions found
        if (questions.isEmpty()) {
            for (String sentence : text.split("[?]")) {
                String s = sentence.trim();
                if (s.length() > 10) {
                    questions.add(s + "?");
                }
            }
        }
        return questions;
    }

    private boolean looksLikeQuestion(String line) {
        return line.length() > 5 && (
            line.contains("?") ||
            line.matches("^(Q?\\d+[.):]|\\*|-)\\s+.*") ||
            line.toLowerCase().startsWith("what") ||
            line.toLowerCase().startsWith("how") ||
            line.toLowerCase().startsWith("do ") ||
            line.toLowerCase().startsWith("does") ||
            line.toLowerCase().startsWith("is ") ||
            line.toLowerCase().startsWith("are") ||
            line.toLowerCase().startsWith("describe") ||
            line.toLowerCase().startsWith("explain") ||
            line.toLowerCase().startsWith("please")
        );
    }

    private boolean isStandaloneQuestion(String line) {
        return line.endsWith("?");
    }
}
