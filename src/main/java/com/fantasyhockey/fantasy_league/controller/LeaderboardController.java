package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.service.FantasyTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LeaderboardController {

    private final FantasyTeamService teamService;

    @GetMapping("/leaderboard")
    public String showLeaderboard(Model model) {
        // Aktualizujeme statistiky (výhry/prohry)
        teamService.updateStandings();

        // Pošleme seznam seřazených týmů do HTML
        model.addAttribute("teams", teamService.getLeaderboard());
        return "leaderboard";
    }
}