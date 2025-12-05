package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.model.LineupSpot;
import com.fantasyhockey.fantasy_league.model.Matchup;
import com.fantasyhockey.fantasy_league.service.FantasyTeamService;
import com.fantasyhockey.fantasy_league.service.MatchupService;
import com.fantasyhockey.fantasy_league.service.RosterLockingService;
import com.fantasyhockey.fantasy_league.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class LiveController {

    private final MatchupService matchupService;
    private final ScheduleService scheduleService;
    private final FantasyTeamService fantasyTeamService;
    private final RosterLockingService rosterLockingService;

    @GetMapping("/live")
    public String showLivePage(Model model) {
        GameWeek currentWeek = scheduleService.getCurrentWeek();
        List<Matchup> matchups = matchupService.getMatchupsForWeek(currentWeek);

        // Prepare View Models for Matchups
        // We need easy access to Lineups for both teams in the view

        List<Map<String, Object>> liveMatchups = new ArrayList<>();

        for (Matchup m : matchups) {
            Map<String, Object> matchData = new HashMap<>();
            matchData.put("matchup", m);

            // Home Team Lineup
            Map<String, LineupSpot> homeLineup = fantasyTeamService.getTeamLineup(m.getHomeTeam())
                    .stream()
                    .collect(Collectors.toMap(LineupSpot::getSlotName, spot -> spot));
            matchData.put("homeLineup", homeLineup);

            // Away Team Lineup
            Map<String, LineupSpot> awayLineup = fantasyTeamService.getTeamLineup(m.getAwayTeam())
                    .stream()
                    .collect(Collectors.toMap(LineupSpot::getSlotName, spot -> spot));
            matchData.put("awayLineup", awayLineup);

            liveMatchups.add(matchData);
        }

        model.addAttribute("liveMatchups", liveMatchups);
        model.addAttribute("currentWeek", currentWeek);

        // Pass Game Statuses for Gray/Color Logic
        // Map<TeamAbbrev, GameStateString>
        model.addAttribute("gameStatuses", rosterLockingService.getTeamGameStatuses());

        return "live";
    }
}
