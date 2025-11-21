package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;

    @GetMapping("/players")
    public String listPlayers(
            @RequestParam(name = "team", required = false) String selectedTeam,
            Model model) {

        // 1. Načteme seznam týmů pro roletku
        List<String> allTeams = playerRepository.findDistinctTeamNames();
        model.addAttribute("teamList", allTeams);

        // 2. Pokud uživatel vybral tým, načteme hráče
        if (selectedTeam != null && !selectedTeam.isEmpty()) {
            List<Player> players = playerRepository.findByTeamNameOrderByLastNameAsc(selectedTeam);
            model.addAttribute("playersList", players);
            model.addAttribute("selectedTeam", selectedTeam); // Aby roletka zůstala vybraná
        } else {
            // Pokud nic nevybral, pošleme prázdný seznam (nebo null)
            model.addAttribute("playersList", List.of());
        }

        return "players";
    }
}