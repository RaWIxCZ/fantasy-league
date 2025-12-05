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
import java.util.Comparator;
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
        private final com.fantasyhockey.fantasy_league.service.RosterLockingService rosterLockingService;

        @GetMapping("/my-team")
        public String showMyTeam(Model model, Principal principal) {
                String username = principal.getName();
                Optional<FantasyTeam> teamOpt = teamService.getTeamByUsername(username);

                if (teamOpt.isPresent()) {
                        FantasyTeam team = teamOpt.get();
                        model.addAttribute("team", team);

                        if (logger.isDebugEnabled()) {
                                logger.debug("🔍 DEBUG: Tým '{}' má v seznamu {} hráčů.", team.getTeamName(),
                                                team.getPlayers().size());
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

                        // --- TŘÍDĚNÍ HRÁČŮ PODLE PRŮMĚRNÝCH BODŮ ---
                        List<com.fantasyhockey.fantasy_league.model.Player> sortedForwards = team.getPlayers().stream()
                                        .filter(p -> List.of("LW", "C", "RW").contains(p.getPosition()))
                                        .sorted(Comparator
                                                        .comparingDouble(
                                                                        com.fantasyhockey.fantasy_league.model.Player::getAverageFantasyPoints)
                                                        .reversed())
                                        .collect(Collectors.toList());

                        List<com.fantasyhockey.fantasy_league.model.Player> sortedDefensemen = team.getPlayers()
                                        .stream()
                                        .filter(p -> "D".equals(p.getPosition()))
                                        .sorted(Comparator
                                                        .comparingDouble(
                                                                        com.fantasyhockey.fantasy_league.model.Player::getAverageFantasyPoints)
                                                        .reversed())
                                        .collect(Collectors.toList());

                        List<com.fantasyhockey.fantasy_league.model.Player> sortedGoalies = team.getPlayers().stream()
                                        .filter(p -> "G".equals(p.getPosition()))
                                        .sorted(Comparator
                                                        .comparingDouble(
                                                                        com.fantasyhockey.fantasy_league.model.Player::getAverageFantasyPoints)
                                                        .reversed())
                                        .collect(Collectors.toList());

                        model.addAttribute("sortedForwards", sortedForwards);
                        model.addAttribute("sortedDefensemen", sortedDefensemen);
                        model.addAttribute("sortedGoalies", sortedGoalies);

                        model.addAttribute("maxForwards", 11);
                        model.addAttribute("maxDefensemen", 7);
                        model.addAttribute("maxGoalies", 3);

                        boolean isTeamFull = forwardsCount >= 11 && defensemenCount >= 7 && goaliesCount >= 3;
                        model.addAttribute("isTeamFull", isTeamFull);

                        model.addAttribute("lockedTeams", rosterLockingService.getLockedTeams());

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
        public org.springframework.http.ResponseEntity<?> addPlayerToTeam(
                        @RequestParam("playerId") Long playerId,
                        @org.springframework.web.bind.annotation.RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                        Principal principal) {
                try {
                        teamService.addPlayerToTeam(playerId, principal.getName());

                        if ("XMLHttpRequest".equals(requestedWith)) {
                                // Pro AJAX vrátíme JSON s novými počty
                                Map<String, Object> response = new java.util.HashMap<>();
                                response.put("success", true);
                                response.put("message", "Hráč byl úspěšně draftován.");

                                // Získání aktuálních počtů pro aktualizaci UI
                                Optional<FantasyTeam> teamOpt = teamService.getTeamByUsername(principal.getName());
                                if (teamOpt.isPresent()) {
                                        FantasyTeam team = teamOpt.get();
                                        response.put("forwardsCount", team.getPlayers().stream()
                                                        .filter(p -> List.of("LW", "C", "RW").contains(p.getPosition()))
                                                        .count());
                                        response.put("defenseCount",
                                                        team.getPlayers().stream()
                                                                        .filter(p -> "D".equals(p.getPosition()))
                                                                        .count());
                                        response.put("goaliesCount",
                                                        team.getPlayers().stream()
                                                                        .filter(p -> "G".equals(p.getPosition()))
                                                                        .count());

                                        response.put("forwardsFull", team.getPlayers().stream()
                                                        .filter(p -> List.of("LW", "C", "RW").contains(p.getPosition()))
                                                        .count() >= 11);
                                        response.put("defenseFull",
                                                        team.getPlayers().stream()
                                                                        .filter(p -> "D".equals(p.getPosition()))
                                                                        .count() >= 7);
                                        response.put("goaliesFull",
                                                        team.getPlayers().stream()
                                                                        .filter(p -> "G".equals(p.getPosition()))
                                                                        .count() >= 3);

                                        response.put("teamName", team.getTeamName());
                                }

                                return org.springframework.http.ResponseEntity.ok(response);
                        }

                        return org.springframework.http.ResponseEntity.status(302).header("Location", "/my-team")
                                        .build();
                } catch (RuntimeException e) {
                        if ("XMLHttpRequest".equals(requestedWith)) {
                                return org.springframework.http.ResponseEntity.badRequest()
                                                .body(Map.of("success", false, "message", e.getMessage()));
                        }
                        String encodedError = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
                        return org.springframework.http.ResponseEntity.status(302)
                                        .header("Location", "/players?error=" + encodedError).build();
                }
        }

        @PostMapping("/remove-player") // Pozor: Používáme POST, protože měníme data (mazání je změna)
        public String removePlayer(@RequestParam("playerId") Long playerId, Principal principal) {
                teamService.removePlayerFromTeam(playerId, principal.getName());
                return "redirect:/my-team"; // Po smazání zůstáváme na stránce týmu
        }
}