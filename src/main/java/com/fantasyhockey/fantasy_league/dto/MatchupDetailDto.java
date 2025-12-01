package com.fantasyhockey.fantasy_league.dto;

import com.fantasyhockey.fantasy_league.model.Matchup;
import com.fantasyhockey.fantasy_league.model.Player;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MatchupDetailDto {
    private Matchup matchup;

    private Player homeTopPlayer;
    private Player awayTopPlayer;

    private List<PlayerWeeklyStatsDto> homeLastWeekTop5;
    private List<PlayerWeeklyStatsDto> awayLastWeekTop5;

    private int homeWinProb;
    private int awayWinProb;

    private java.util.List<String> homeForm; // "W", "L", "OTW", "OTL"
    private java.util.List<String> awayForm;

    private int homeStreak; // Positive = Win Streak, Negative = Loss Streak
    private int awayStreak;
}
