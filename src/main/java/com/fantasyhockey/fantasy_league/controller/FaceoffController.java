package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.model.Matchup;
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
    public String faceoff(Model model) {
        // Ensure season is initialized (simple check for now)
        scheduleService.initializeSeason();

        // Update scores before displaying (to show live/latest data)
        matchupService.updateScoresForCurrentWeek();

        GameWeek currentWeek = scheduleService.getCurrentWeek();
        List<Matchup> matchups = matchupService.getCurrentMatchups();

        model.addAttribute("currentWeek", currentWeek);
        model.addAttribute("matchups", matchups);

        return "faceoff";
    }
}
