package com.almabase.questionnaire.controller;

import com.almabase.questionnaire.entity.*;
import com.almabase.questionnaire.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private ReferenceDocumentService refDocService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        List<QuestionnaireRun> runs = questionnaireService.getRunsForUser(user);
        List<ReferenceDocument> refDocs = refDocService.findByUser(user);
        model.addAttribute("runs", runs);
        model.addAttribute("refDocs", refDocs);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("refDocCount", refDocs.size());
        return "dashboard";
    }
}
