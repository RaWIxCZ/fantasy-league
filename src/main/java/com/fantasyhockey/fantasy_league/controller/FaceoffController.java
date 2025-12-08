package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.service.MatchupService;
import com.fantasyhockey.fantasy_league.service.ScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller for the Faceoff/Matchup page.
 * Displays weekly head-to-head matchups between fantasy teams with
 * detailed statistics, predictions, and team form.
 */
@Controller
public class FaceoffController {

    private static final int MAX_WEEKS = 20;

    private final MatchupService matchupService;
    private final ScheduleService scheduleService;

    public FaceoffController(MatchupService matchupService, ScheduleService scheduleService) {
        this.matchupService = matchupService;
        this.scheduleService = scheduleService;
    }

    /**
     * Displays the faceoff page with matchups for a specific week.
     * If no week is specified, shows the current week.
     * Automatically initializes the season schedule if needed.
     * 
     * @param week  optional week number (1-20), defaults to current week
     * @param model Spring MVC model
     * @return faceoff template name
     */
    @GetMapping("/faceoff")
    public String faceoff(@org.springframework.web.bind.annotation.RequestParam(required = false) Integer week,
            Model model) {
        // Ensure season schedule is initialized
        scheduleService.initializeSeason();

        // Determine which week to display
        GameWeek displayWeek;
        if (week != null) {
            try {
                displayWeek = scheduleService.getWeekByNumber(week);
            } catch (Exception e) {
                // Fall back to current week if requested week doesn't exist
                displayWeek = scheduleService.getCurrentWeek();
            }
        } else {
            displayWeek = scheduleService.getCurrentWeek();
        }

        // Update scores for the displayed week
        matchupService.updateScoresForWeek(displayWeek);

        // Get matchup details with statistics
        List<com.fantasyhockey.fantasy_league.dto.MatchupDetailDto> matchups = matchupService
                .getMatchupDetails(displayWeek);

        // Add data to model
        model.addAttribute("currentWeek", displayWeek);
        model.addAttribute("actualCurrentWeek", scheduleService.getCurrentWeek());
        model.addAttribute("matchups", matchups);

        // Add week navigation links
        int currentWeekNum = displayWeek.getWeekNumber();
        model.addAttribute("prevWeek", currentWeekNum > 1 ? currentWeekNum - 1 : null);
        model.addAttribute("nextWeek", currentWeekNum < MAX_WEEKS ? currentWeekNum + 1 : null);

        return "faceoff";
    }
}
