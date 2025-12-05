package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.service.FantasyTeamService;
import com.fantasyhockey.fantasy_league.service.NhlApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
// @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
// // Handled in SecurityConfig
public class AdminController {

    private final NhlApiService nhlApiService;
    private final FantasyTeamService fantasyTeamService;

    @GetMapping
    public String adminHub(Model model) {
        List<FantasyTeam> teams = fantasyTeamService.getLeaderboard();
        model.addAttribute("teams", teams);
        return "admin-hub";
    }

    @PostMapping("/update-stats")
    public String updateStats(@RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        new Thread(() -> nhlApiService.updateStatsForDateRange(startDate, endDate)).start();

        return "redirect:/admin?success=statsUpdateStarted";
    }

    @PostMapping("/remove-player")
    public String removePlayer(@RequestParam("teamId") Long teamId,
            @RequestParam("playerId") Long playerId) {
        fantasyTeamService.removePlayerFromTeam(playerId, teamId);
        return "redirect:/admin?success=playerRemoved";
    }

    // --- Existing Endpoints (kept for backward compatibility or direct usage) ---

    @GetMapping("/fetch-game")
    @ResponseBody
    public String fetchGameStats(@RequestParam("gameId") Long gameId) {
        nhlApiService.processGame(gameId, java.time.LocalDate.now());
        return "Zápas " + gameId + " zpracován!";
    }

    @GetMapping("/reimport-season")
    @ResponseBody
    public String reimportSeason() {
        new Thread(() -> nhlApiService.resetAndImportSeasonData()).start();
        return "🚀 RESET a IMPORT sezóny spuštěn na pozadí!";
    }

    @GetMapping("/import-all-teams")
    @ResponseBody
    public String triggerAllTeamsImport() {
        new Thread(() -> nhlApiService.importAllTeams()).start();
        return "🚀 Import sezóny spuštěn na pozadí! Sleduj konzoli v IntelliJ.";
    }
}