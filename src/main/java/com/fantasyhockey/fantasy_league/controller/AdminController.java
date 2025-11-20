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

    // Endpoint pro simulaci bodů (ruční)
    @PostMapping("/admin/add-points")
    public String simulatePoints(
            @RequestParam("playerId") Long playerId,
            @RequestParam("goals") int goals,
            @RequestParam("assists") int assists) {

        pointsService.addStatsForPlayer(playerId, goals, assists, LocalDate.now());
        return "redirect:/my-team";
    }

    // Endpoint pro stažení jednoho zápasu
    @PostMapping("/admin/fetch-game")
    @ResponseBody
    public String fetchGameStats(@RequestParam("gameId") Long gameId) {
        nhlApiService.processGame(gameId);
        return "Zápas " + gameId + " zpracován!";
    }

    // NOVÝ ENDPOINT: Import celé sezóny
    @GetMapping("/admin/import-season")
    @ResponseBody
    public String triggerSeasonImport() {
        // Spustíme to ve vedlejším vlákně, aby nezamrzla stránka
        new Thread(() -> nhlApiService.importSeasonData()).start();
        return "🚀 Import sezóny spuštěn na pozadí! Sleduj konzoli v IntelliJ.";
    }
}