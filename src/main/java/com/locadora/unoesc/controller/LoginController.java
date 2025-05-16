package com.locadora.unoesc.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private static final String USUARIO = "admin";
    private static final String SENHA_HASH = "$2b$12$YEuHbuk5gDugPZLqoUDaEOkZAibJeiPwgYcRl09SismRxHPAl.laO";

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("error", error != null);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        if (username.equals(USUARIO) && new BCryptPasswordEncoder().matches(password, SENHA_HASH)) {
            session.setAttribute("usuarioLogado", true);
            return "redirect:/home";
        } else {
            return "redirect:/login?erro";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String home(HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/login";
        }
        return "home";
    }
}
