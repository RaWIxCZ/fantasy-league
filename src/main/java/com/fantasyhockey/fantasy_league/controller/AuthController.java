package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.dto.UserRegistrationDto;
import com.fantasyhockey.fantasy_league.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // Zobrazení registračního formuláře
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    // Zpracování registrace
    @PostMapping("/save-user")
    public String registerUser(@ModelAttribute("user") UserRegistrationDto userDto, Model model) {
        // Tady by měla být validace (jestli user už neexistuje), ale pro začátek to zjednodušíme
        try {
            userService.registerUser(userDto);
            return "redirect:/login?success"; // Po úspěchu přesměrujeme na login
        } catch (Exception e) {
            model.addAttribute("error", "Chyba při registraci: " + e.getMessage());
            return "register";
        }
    }

    // Zobrazení loginu
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}