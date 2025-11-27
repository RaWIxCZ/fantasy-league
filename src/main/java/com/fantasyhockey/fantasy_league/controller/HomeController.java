package com.fantasyhockey.fantasy_league.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/") // Toto odchytí adresu localhost:8080/ (bez ničeho dalšího)
    public String home() {
        return "homepage";
    }
}