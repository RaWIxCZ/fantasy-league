package com.fantasyhockey.fantasy_league.dto;

import com.fantasyhockey.fantasy_league.model.Matchup;
import com.fantasyhockey.fantasy_league.model.Player;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object containing detailed matchup information for display.
 * Aggregates matchup data, player statistics, team form, and win probabilities
 * for the faceoff/matchup preview page.
 */
@Data
@Builder
public class MatchupDetailDto {
    /**
     * The core matchup entity with teams and scores.
     */
    private Matchup matchup;

    /**
     * Home team's top performing player this season (by fantasy points).
     */
    private Player homeTopPlayer;

    /**
     * Away team's top performing player this season (by fantasy points).
     */
    private Player awayTopPlayer;

    /**
     * Home team's top 5 players from last week (by fantasy points).
     * Empty list if previous week is not completed.
     */
    private List<PlayerWeeklyStatsDto> homeLastWeekTop5;

    /**
     * Away team's top 5 players from last week (by fantasy points).
     * Empty list if previous week is not completed.
     */
    private List<PlayerWeeklyStatsDto> awayLastWeekTop5;

    /**
     * Home team's win probability (0-100).
     * Calculated based on season performance.
     */
    private int homeWinProb;

    /**
     * Away team's win probability (0-100).
     * Calculated based on season performance.
     */
    private int awayWinProb;

    /**
     * Home team's recent form (last 5 games).
     * Values: "V" (victory), "P" (loss), "TBD" (not yet played)
     */
    private java.util.List<String> homeForm;

    /**
     * Away team's recent form (last 5 games).
     * Values: "V" (victory), "P" (loss), "TBD" (not yet played)
     */
    private java.util.List<String> awayForm;

    /**
     * Home team's current streak.
     * Positive = win streak, Negative = loss streak, 0 = no streak
     */
    private int homeStreak;

    /**
     * Away team's current streak.
     * Positive = win streak, Negative = loss streak, 0 = no streak
     */
    private int awayStreak;
}
