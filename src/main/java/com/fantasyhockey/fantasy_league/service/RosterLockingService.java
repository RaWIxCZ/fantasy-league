package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.dto.NhlScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing roster locking based on NHL game schedules.
 * Prevents lineup changes for players whose games have already started.
 * 
 * TODO: Consider implementing caching (1-5 minutes) to reduce NHL API calls.
 * For single-user local development, direct API calls are acceptable.
 */
@Service
@RequiredArgsConstructor
public class RosterLockingService {

    private final NhlApiService nhlApiService;

    /**
     * Returns a set of NHL team abbreviations whose games have already started
     * today.
     * Players on these teams cannot be added/removed from lineups until the next
     * day.
     * 
     * @return set of locked team abbreviations (e.g., "BOS", "NYR", "TOR")
     */
    public Set<String> getLockedTeams() {
        Set<String> lockedTeams = new HashSet<>();
        NhlScheduleResponse schedule = nhlApiService.getSchedule(LocalDate.now());

        if (schedule == null || schedule.getGameWeek() == null) {
            return lockedTeams;
        }

        ZonedDateTime now = ZonedDateTime.now();

        for (NhlScheduleResponse.GameWeekDto day : schedule.getGameWeek()) {
            if (day.getGames() == null)
                continue;

            for (NhlScheduleResponse.GameDto game : day.getGames()) {
                // Check if game has started by comparing start time with current time
                if (game.getStartTimeUTC() != null) {
                    try {
                        ZonedDateTime startTime = ZonedDateTime.parse(game.getStartTimeUTC(),
                                DateTimeFormatter.ISO_DATE_TIME);

                        // If game started (start time is in the past), lock both teams
                        if (startTime.isBefore(now)) {
                            if (game.getHomeTeam() != null)
                                lockedTeams.add(game.getHomeTeam().getAbbrev());
                            if (game.getAwayTeam() != null)
                                lockedTeams.add(game.getAwayTeam().getAbbrev());
                        }
                    } catch (Exception e) {
                        // Ignore parse errors and continue
                    }
                }
            }
        }
        return lockedTeams;
    }

    /**
     * Returns a map of team abbreviations to their current game status.
     * Used for the Live Page to visually indicate which teams are playing.
     * 
     * Game states:
     * - "FUT": Future game (not started)
     * - "PRE": Pre-game (warmups)
     * - "LIVE": Game in progress
     * - "CRIT": Critical moments (overtime, shootout)
     * - "FINAL": Game finished
     * - "OFF": No game today (not in map, caller should handle as default)
     * 
     * @return map of team abbreviation to game state
     */
    public Map<String, String> getTeamGameStatuses() {
        Map<String, String> statuses = new HashMap<>();
        NhlScheduleResponse schedule = nhlApiService.getSchedule(LocalDate.now());

        if (schedule == null || schedule.getGameWeek() == null) {
            return statuses;
        }

        String todayStr = LocalDate.now().toString();

        for (NhlScheduleResponse.GameWeekDto day : schedule.getGameWeek()) {
            // Only process today's games for live status visualization
            if (!todayStr.equals(day.getDate()))
                continue;

            if (day.getGames() != null) {
                for (NhlScheduleResponse.GameDto game : day.getGames()) {
                    String state = game.getGameState();
                    String home = game.getHomeTeam() != null ? game.getHomeTeam().getAbbrev() : null;
                    String away = game.getAwayTeam() != null ? game.getAwayTeam().getAbbrev() : null;

                    if (home != null)
                        statuses.put(home, state);
                    if (away != null)
                        statuses.put(away, state);
                }
            }
        }
        return statuses;
    }
}
