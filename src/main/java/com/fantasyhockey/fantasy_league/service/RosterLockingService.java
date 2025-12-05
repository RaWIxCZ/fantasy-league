package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.dto.NhlScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RosterLockingService {

    private final NhlApiService nhlApiService;

    // TODO: Consider caching this for 1-5 minutes to avoid spamming NHL API on
    // every request
    // For local use with one user, direct call is fine.

    /**
     * Returns a set of NHL team abbreviations (e.g. "BOS", "NYR") whose games
     * have already started today or are finished.
     */
    public Set<String> getLockedTeams() {
        Set<String> lockedTeams = new HashSet<>();
        NhlScheduleResponse schedule = nhlApiService.getSchedule(LocalDate.now());

        if (schedule == null || schedule.getGameWeek() == null) {
            return lockedTeams;
        }

        ZonedDateTime now = ZonedDateTime.now();

        for (NhlScheduleResponse.GameWeekDto day : schedule.getGameWeek()) {
            // "today" in API response might cover a rolling window, ensure we check the
            // date if needed
            // But usually getSchedule(now) returns the week focusing on now.
            // We should filter for strictly today's games if the response includes others.
            // The API usually returns ~1 week around the date.

            if (day.getGames() == null)
                continue;

            for (NhlScheduleResponse.GameDto game : day.getGames()) {
                // Check if the game is relevant to "Today" based on start time?
                // Actually, if a game is in the list, we check its start time.
                // If start time is past -> Locked.

                if (game.getStartTimeUTC() != null) {
                    try {
                        ZonedDateTime startTime = ZonedDateTime.parse(game.getStartTimeUTC(),
                                DateTimeFormatter.ISO_DATE_TIME);

                        // If game started (Time is in past) -> Lock both teams
                        if (startTime.isBefore(now)) {
                            if (game.getHomeTeam() != null)
                                lockedTeams.add(game.getHomeTeam().getAbbrev());
                            if (game.getAwayTeam() != null)
                                lockedTeams.add(game.getAwayTeam().getAbbrev());
                        }
                    } catch (Exception e) {
                        // ignore parse error
                    }
                }
            }
        }
        return lockedTeams;
    }

    /**
     * Returns a map of Team Abbreviation -> Game State (PRE, LIVE, FINAL, OFF)
     * Used for the Live Page to color/gray out players.
     */
    public Map<String, String> getTeamGameStatuses() {
        Map<String, String> statuses = new HashMap<>();
        NhlScheduleResponse schedule = nhlApiService.getSchedule(LocalDate.now());

        // Default to "OFF" (No Game) if not found in schedule
        // But since we can't easily iterate all teams to set "OFF", callers should
        // handle missing keys as "OFF"

        if (schedule == null || schedule.getGameWeek() == null) {
            return statuses;
        }

        String todayStr = LocalDate.now().toString();

        for (NhlScheduleResponse.GameWeekDto day : schedule.getGameWeek()) {
            // Only care about TODAY's games for "Live" status visualization?
            // User said: "zašedlé... se zrovna nebude hrát žádný zápas"
            // So strictly Today.
            if (!todayStr.equals(day.getDate()))
                continue;

            if (day.getGames() != null) {
                for (NhlScheduleResponse.GameDto game : day.getGames()) {
                    String state = game.getGameState(); // FUT, PRE, LIVE, CRIT, FINAL, OFF
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
