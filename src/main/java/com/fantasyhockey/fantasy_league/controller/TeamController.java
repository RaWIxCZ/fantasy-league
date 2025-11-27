package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.LineupSpot;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequiredArgsConstructor
public class TeamController {

    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    private final FantasyTeamService teamService;

    @GetMapping("/my-team")
    public String showMyTeam(Model model, Principal principal) {
        String username = principal.getName();
        Optional<FantasyTeam> teamOpt = teamService.getTeamByUsername(username);

        if (teamOpt.isPresent()) {
            FantasyTeam team = teamOpt.get();
            model.addAttribute("team", team);

            if (logger.isDebugEnabled()) {
                logger.debug("🔍 DEBUG: Tým '{}' má v seznamu {} hráčů.", team.getTeamName(), team.getPlayers().size());
                for (var p : team.getPlayers()) {
                    logger.debug("   - Hráč: {} (ID: {})", p.getLastName(), p.getId());
                }
            }

            model.addAttribute("team", team);

            // 1. Získáme seznam obsazených pozic z DB
            List<LineupSpot> spots = teamService.getTeamLineup(team);

            // 2. Převedeme List na Mapu, kde klíč je Název slotu ("LW", "C"...)
            // To nám umožní v HTML snadno dělat: lineup['LW']
            Map<String, LineupSpot> lineupMap = spots.stream()
                    .collect(Collectors.toMap(LineupSpot::getSlotName, spot -> spot));

            model.addAttribute("lineup", lineupMap);

            // Mapa: ID Hráče -> Název Slotu (např. "L1_LW")
            Map<Long, String> activePlayerSlots = spots.stream()
                    .collect(Collectors.toMap(
                            s -> s.getPlayer().getId(),
                            LineupSpot::getSlotName));
            model.addAttribute("activePlayerSlots", activePlayerSlots);

            // Spočítat hráče podle pozic
            long forwardsCount = team.getPlayers().stream()
                    .filter(p -> List.of("LW", "C", "RW").contains(p.getPosition()))
                    .count();
            long defensemenCount = team.getPlayers().stream()
                    .filter(p -> "D".equals(p.getPosition()))
                    .count();
            long goaliesCount = team.getPlayers().stream()
                    .filter(p -> "G".equals(p.getPosition()))
                    .count();

            model.addAttribute("forwardsCount", forwardsCount);
            model.addAttribute("defensemenCount", defensemenCount);
            model.addAttribute("goaliesCount", goaliesCount);

            model.addAttribute("maxForwards", 11);
            model.addAttribute("maxDefensemen", 7);
            model.addAttribute("maxGoalies", 3);

            boolean isTeamFull = forwardsCount >= 11 && defensemenCount >= 7 && goaliesCount >= 3;
            model.addAttribute("isTeamFull", isTeamFull);

            return "my-team";
        } else {
            return "create-team";
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