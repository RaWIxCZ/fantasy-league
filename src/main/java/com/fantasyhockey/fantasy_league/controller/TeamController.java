package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.service.FantasyTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TeamController {

    private final FantasyTeamService teamService;

    @GetMapping("/my-team")
    public String showMyTeam(Model model, Principal principal) {
        // Principal = aktuálně přihlášený uživatel (od Spring Security)
        String username = principal.getName();

        Optional<FantasyTeam> team = teamService.getTeamByUsername(username);

        if (team.isPresent()) {
            // Uživatel už má tým -> Zobrazíme ho
            model.addAttribute("team", team.get());
            return "my-team"; // existující stránka
        } else {
            // Uživatel nemá tým -> Zobrazíme formulář pro vytvoření
            return "create-team"; // nová stránka
        }
    }

    @PostMapping("/create-team")
    public String createTeam(@RequestParam("teamName") String teamName, Principal principal) {
        teamService.createTeam(teamName, principal.getName());
        return "redirect:/my-team";
    }
    @PostMapping("/add-player")
    public String addPlayerToTeam(@RequestParam("playerId") Long playerId, Principal principal) {
        try {
            teamService.addPlayerToTeam(playerId, principal.getName());
            return "redirect:/my-team";
        } catch (RuntimeException e) {
            // OPRAVA: Enkódování češtiny do URL formátu
            String encodedError = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/players?error=" + encodedError;
        }
    }
    @PostMapping("/remove-player") // Pozor: Používáme POST, protože měníme data (mazání je změna)
    public String removePlayer(@RequestParam("playerId") Long playerId, Principal principal) {
        teamService.removePlayerFromTeam(playerId, principal.getName());
        return "redirect:/my-team"; // Po smazání zůstáváme na stránce týmu
    }
}