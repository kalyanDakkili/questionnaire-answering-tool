package com.almabase.questionnaire.controller;

import com.almabase.questionnaire.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "Invalid username or password");
        if (logout != null) model.addAttribute("message", "Logged out successfully");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           RedirectAttributes attrs) {
        if (!password.equals(confirmPassword)) {
            attrs.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }
        if (password.length() < 6) {
            attrs.addFlashAttribute("error", "Password must be at least 6 characters");
            return "redirect:/register";
        }
        try {
            userService.register(username, email, password);
            attrs.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            attrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}
