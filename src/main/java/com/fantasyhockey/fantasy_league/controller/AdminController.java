package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.service.NhlApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
// @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final NhlApiService nhlApiService;

    // ENDPOINT: Stažení jednoho zápasu
    @GetMapping("/admin/fetch-game")
    @ResponseBody
    public String fetchGameStats(@RequestParam("gameId") Long gameId) {
        // Note: Manual fetch defaults to today's date. Use with caution for historical
        // games.
        nhlApiService.processGame(gameId, java.time.LocalDate.now());
        return "Zápas " + gameId + " zpracován!";
    }

    @GetMapping("/admin/reimport-season")
    @ResponseBody
    public String reimportSeason() {
        new Thread(() -> nhlApiService.resetAndImportSeasonData()).start();
        return "🚀 RESET a IMPORT sezóny spuštěn na pozadí!";
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