package com.almabase.questionnaire.controller;

import com.almabase.questionnaire.entity.User;
import com.almabase.questionnaire.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Controller
@RequestMapping("/documents")
public class ReferenceDocumentController {

    @Autowired
    private UserService userService;

    @Autowired
    private ReferenceDocumentService refDocService;

    @PostMapping("/upload")
    public String upload(@RequestParam("files") MultipartFile[] files,
                         Principal principal,
                         RedirectAttributes attrs) {
        User user = userService.findByUsername(principal.getName());
        int uploaded = 0;
        int failed = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                refDocService.upload(file, user);
                uploaded++;
            } catch (Exception e) {
                failed++;
            }
        }
        if (uploaded > 0) attrs.addFlashAttribute("success", uploaded + " document(s) uploaded successfully");
        if (failed > 0) attrs.addFlashAttribute("warning", failed + " document(s) failed to upload");
        return "redirect:/dashboard";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes attrs) {
        User user = userService.findByUsername(principal.getName());
        refDocService.delete(id, user.getId());
        attrs.addFlashAttribute("success", "Document deleted");
        return "redirect:/dashboard";
    }
}
