package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.model.Matchup;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.repository.MatchupRepository;
import com.fantasyhockey.fantasy_league.repository.PlayerStatsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchupService {

    private final MatchupRepository matchupRepository;
    private final ScheduleService scheduleService;
    private final PlayerStatsRepository playerStatsRepository;

    public MatchupService(MatchupRepository matchupRepository, ScheduleService scheduleService,
            PlayerStatsRepository playerStatsRepository) {
        this.matchupRepository = matchupRepository;
        this.scheduleService = scheduleService;
        this.playerStatsRepository = playerStatsRepository;
    }

    public List<Matchup> getCurrentMatchups() {
        GameWeek currentWeek = scheduleService.getCurrentWeek();
        return matchupRepository.findByGameWeek(currentWeek);
    }

    public List<Matchup> getMatchupsForWeek(GameWeek week) {
        return matchupRepository.findByGameWeekOrderByIdAsc(week);
    }

    // Calculate scores for all matchups in the current week
    // This should be called periodically or when viewing the page
    public void updateScoresForCurrentWeek() {
        GameWeek currentWeek = scheduleService.getCurrentWeek();
        List<Matchup> matchups = matchupRepository.findByGameWeek(currentWeek);

        for (Matchup matchup : matchups) {
            double homeScore = calculateTeamScoreForWeek(matchup.getHomeTeam(), currentWeek);
            double awayScore = calculateTeamScoreForWeek(matchup.getAwayTeam(), currentWeek);

            matchup.setHomeScore(homeScore);
            matchup.setAwayScore(awayScore);

            matchupRepository.save(matchup);
        }
    }

    private double calculateTeamScoreForWeek(FantasyTeam team, GameWeek week) {
        if (team == null)
            return 0.0;

        // Sum points of all players in the team for games played within the week's date
        // range
        // Note: This assumes players stay in the team. If transfers are allowed
        // mid-week, logic needs to be more complex (tracking roster history).
        // For now, we sum points of current roster for the period.

        return team.getPlayers().stream()
                .mapToDouble(player -> getPlayerPointsForPeriod(player, week.getStartDate(), week.getEndDate()))
                .sum();
    }

    private double getPlayerPointsForPeriod(Player player, LocalDate start, LocalDate end) {
        // We need a method in PlayerStatsRepository or logic here to sum points
        // Assuming Player has a list of stats or we query the repo
        // Using repository is better for performance if optimized, but here we might
        // need to iterate
        // Let's assume we use the repository to find stats by player and date range

        return playerStatsRepository.findByPlayerIdAndDateBetween(player.getId(), start, end)
                .stream()
                .mapToDouble(stats -> stats.getFantasyPoints())
                .sum();
    }

    public List<Player> getTopPlayers(FantasyTeam team, int limit) {
        if (team == null)
            return List.of();

        return team.getPlayers().stream()
                .sorted(Comparator.comparingDouble(Player::getSeasonFantasyPoints).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
