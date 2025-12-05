package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.service.MatchupService;
import com.fantasyhockey.fantasy_league.service.ScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FaceoffController {

    private final MatchupService matchupService;
    private final ScheduleService scheduleService;

    public FaceoffController(MatchupService matchupService, ScheduleService scheduleService) {
        this.matchupService = matchupService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/faceoff")
    public String faceoff(@org.springframework.web.bind.annotation.RequestParam(required = false) Integer week,
            Model model) {
        // Ensure season is initialized (simple check for now)
        scheduleService.initializeSeason();

        // Update scores before displaying (to show live/latest data)
        // We do this for the displayed week to ensure points are calculated
        // even if it's a past week (e.g. after a reset)

        GameWeek displayWeek;
        if (week != null) {
            try {
                displayWeek = scheduleService.getWeekByNumber(week);
            } catch (Exception e) {
                displayWeek = scheduleService.getCurrentWeek();
            }
        } else {
            displayWeek = scheduleService.getCurrentWeek();
        }

        matchupService.updateScoresForWeek(displayWeek);

        List<com.fantasyhockey.fantasy_league.dto.MatchupDetailDto> matchups = matchupService
                .getMatchupDetails(displayWeek);

        model.addAttribute("currentWeek", displayWeek);
        model.addAttribute("actualCurrentWeek", scheduleService.getCurrentWeek());
        model.addAttribute("matchups", matchups);

        // Navigation
        int currentWeekNum = displayWeek.getWeekNumber();
        model.addAttribute("prevWeek", currentWeekNum > 1 ? currentWeekNum - 1 : null);
        // Assuming 20 weeks total for now, or check max
        model.addAttribute("nextWeek", currentWeekNum < 20 ? currentWeekNum + 1 : null);

        return "faceoff";
    }
}
