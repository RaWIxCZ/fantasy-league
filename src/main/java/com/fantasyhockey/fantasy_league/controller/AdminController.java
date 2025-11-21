package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.service.NhlApiService;
import com.fantasyhockey.fantasy_league.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final PointsService pointsService;
    private final NhlApiService nhlApiService;

    // ENDPOINT: Simulace bodů (ruční)
    @PostMapping("/admin/add-points")
    public String simulatePoints(
            @RequestParam("playerId") Long playerId,
            @RequestParam("goals") int goals,
            @RequestParam("assists") int assists) {

        // Vygenerujeme unikátní falešné ID (např. aktuální čas),
        // aby to prošlo kontrolou duplicity v PointsService
        Long fakeGameId = System.currentTimeMillis();

        // Teď posíláme 5 parametrů:
        pointsService.addStatsForPlayer(
                playerId,
                fakeGameId, // <--- Nový parametr (falešné ID)
                goals,
                assists,
                LocalDate.now()
        );

        return "redirect:/my-team";
    }

    // ENDPOINT: Stažení jednoho zápasu
    @GetMapping("/admin/fetch-game")
    @ResponseBody
    public String fetchGameStats(@RequestParam("gameId") Long gameId) {
        nhlApiService.processGame(gameId);
        return "Zápas " + gameId + " zpracován!";
    }

    // ENDPOINT: Import celé sezóny
    @GetMapping("/admin/import-season")
    @ResponseBody
    public String triggerSeasonImport() {
        // Spustíme to ve vedlejším vlákně, aby nezamrzla stránka
        new Thread(() -> nhlApiService.importSeasonData()).start();
        return "🚀 Import sezóny spuštěn na pozadí! Sleduj konzoli v IntelliJ.";
    }

    // NOVÝ ENDPOINT: Import celé sezóny
    @GetMapping("/admin/import-all-teams")
    @ResponseBody
    public String triggerAllTeamsImport() {
        // Spustíme to ve vedlejším vlákně, aby nezamrzla stránka
        new Thread(() -> nhlApiService.importAllTeams()).start();
        return "🚀 Import sezóny spuštěn na pozadí! Sleduj konzoli v IntelliJ.";
    }

}