package com.almabase.questionnaire.controller;

import com.almabase.questionnaire.dto.*;
import com.almabase.questionnaire.entity.*;
import com.almabase.questionnaire.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/questionnaire")
public class QuestionnaireController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private ExportService exportService;

    @GetMapping("/upload")
    public String uploadPage(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        var refDocs = questionnaireService.getRunsForUser(user);
        model.addAttribute("hasRefDocs", !refDocs.isEmpty());
        return "questionnaire/upload";
    }

    @PostMapping("/process")
    public String process(@RequestParam("file") MultipartFile file,
                          Principal principal,
                          RedirectAttributes attrs) {
        User user = userService.findByUsername(principal.getName());
        try {
            QuestionnaireRun run = questionnaireService.processQuestionnaire(file, user);
            attrs.addFlashAttribute("success", "Questionnaire processed! " + run.getAnswers().size() + " questions answered.");
            return "redirect:/questionnaire/" + run.getId() + "/review";
        } catch (Exception e) {
            attrs.addFlashAttribute("error", "Error processing questionnaire: " + e.getMessage());
            return "redirect:/questionnaire/upload";
        }
    }

    @GetMapping("/{id}/review")
    public String reviewPage(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        Optional<QuestionnaireRun> optRun = questionnaireService.findByIdAndUser(id, user);
        if (optRun.isEmpty()) return "redirect:/dashboard";
        QuestionnaireRun run = optRun.get();
        Map<String, Object> summary = questionnaireService.getCoverageSummary(run);
        model.addAttribute("run", run);
        model.addAttribute("answers", run.getAnswers());
        model.addAttribute("summary", summary);
        return "questionnaire/review";
    }

    @PostMapping("/{runId}/answer/{answerId}/edit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editAnswer(@PathVariable Long runId,
                                                           @PathVariable Long answerId,
                                                           @RequestBody AnswerUpdateRequest req,
                                                           Principal principal) {
        try {
            userService.findByUsername(principal.getName()); // auth check
            QuestionAnswer updated = questionnaireService.updateAnswer(answerId, req.getNewAnswer());
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("answer", updated.getAnswer());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{runId}/regenerate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> regenerate(@PathVariable Long runId,
                                                           @RequestBody RegenerateRequest req,
                                                           Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            questionnaireService.regenerateAnswers(req.getAnswerIds(), user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteRun(@PathVariable Long id, Principal principal, RedirectAttributes attrs) {
        User user = userService.findByUsername(principal.getName());
        Optional<QuestionnaireRun> optRun = questionnaireService.findByIdAndUser(id, user);
        if (optRun.isPresent()) {
            questionnaireService.deleteRun(id);
            attrs.addFlashAttribute("success", "Run deleted");
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Optional<QuestionnaireRun> optRun = questionnaireService.findByIdAndUser(id, user);
        if (optRun.isEmpty()) return ResponseEntity.notFound().build();
        try {
            byte[] docxBytes = exportService.exportToDocx(optRun.get());
            String filename = optRun.get().getName().replaceAll("[^a-zA-Z0-9._-]", "_") + "_answered.docx";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docxBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}