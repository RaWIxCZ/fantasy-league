package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.service.FantasyTeamService;
import com.fantasyhockey.fantasy_league.service.NhlApiService;
import com.fantasyhockey.fantasy_league.service.ScheduleService;
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

/**
 * Controller for admin-only functionality.
 * Provides tools for managing player data, updating statistics, and system
 * maintenance.
 * 
 * Access control is handled in SecurityConfig (requires ADMIN authority).
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final NhlApiService nhlApiService;
    private final FantasyTeamService fantasyTeamService;
    private final ScheduleService scheduleService;

    /**
     * Displays the admin hub page with team overview.
     * 
     * @param model Spring MVC model
     * @return admin-hub template name
     */
    @GetMapping
    public String adminHub(Model model) {
        List<FantasyTeam> teams = fantasyTeamService.getLeaderboard();
        model.addAttribute("teams", teams);
        return "admin-hub";
    }

    /**
     * Triggers a background update of player statistics for a date range.
     * Useful for backfilling data or fixing missing stats.
     * 
     * @param startDateStr start date in ISO format (yyyy-MM-dd)
     * @param endDateStr   end date in ISO format (yyyy-MM-dd)
     * @return redirect to admin page with success message
     */
    @PostMapping("/update-stats")
    public String updateStats(@RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // Run in background to avoid blocking the request
        new Thread(() -> nhlApiService.updateStatsForDateRange(startDate, endDate)).start();

        return "redirect:/admin?success=statsUpdateStarted";
    }

    /**
     * Removes a player from a fantasy team.
     * 
     * @param teamId   ID of the fantasy team
     * @param playerId ID of the player to remove
     * @return redirect to admin page with success message
     */
    @PostMapping("/remove-player")
    public String removePlayer(@RequestParam("teamId") Long teamId,
            @RequestParam("playerId") Long playerId) {
        fantasyTeamService.removePlayerFromTeam(playerId, teamId);
        return "redirect:/admin?success=playerRemoved";
    }

    // ==================== Legacy/Direct Access Endpoints ====================
    // Kept for backward compatibility or direct API usage

    /**
     * Manually processes statistics for a specific NHL game.
     * 
     * @param gameId NHL game ID to process
     * @return success message
     */
    @GetMapping("/fetch-game")
    @ResponseBody
    public String fetchGameStats(@RequestParam("gameId") Long gameId) {
        nhlApiService.processGame(gameId, java.time.LocalDate.now());
        return "Game " + gameId + " processed!";
    }

    /**
     * Manually updates game week statuses (current/completed flags).
     * Useful for immediately updating the current week without waiting for daily
     * cron job.
     * 
     * @return redirect to admin hub with success message
     */
    @GetMapping("/update-weeks")
    public String updateGameWeeks() {
        scheduleService.updateGameWeekStatuses();
        return "redirect:/admin?success=weeksUpdated";
    }

    /**
     * Resets all statistics and reimports the entire season data.
     * WARNING: This deletes all existing stats and reimports from scratch.
     * Runs in background.
     * 
     * @return success message
     */
    @GetMapping("/reimport-season")
    @ResponseBody
    public String reimportSeason() {
        new Thread(() -> nhlApiService.resetAndImportSeasonData()).start();
        return "🚀 Season RESET and IMPORT started in background!";
    }

    /**
     * Triggers import of all NHL team rosters.
     * Runs in background. Check console for progress.
     * 
     * @return success message
     */
    @GetMapping("/import-all-teams")
    @ResponseBody
    public String triggerAllTeamsImport() {
        new Thread(() -> nhlApiService.importAllTeams()).start();
        return "🚀 Season import started in background! Check console for progress.";
    }
}