package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;

    @GetMapping("/players") // Když uživatel zadá localhost:8080/players
    public String listPlayers(Model model) {
        // 1. Získáme seznam všech hráčů z databáze
        var allPlayers = playerRepository.findAll();

        // 2. Vložíme je do "modelu" (balíček dat pro HTML stránku)
        // "playersList" je název, pod kterým to budeme volat v HTML
        model.addAttribute("playersList", allPlayers);

        // 3. Řekneme Springu, ať zobrazí šablonu s názvem "players" (players.html)
        return "players";
    }
}