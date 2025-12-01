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
        updateScoresForWeek(scheduleService.getCurrentWeek());
    }

    public void updateScoresForWeek(GameWeek week) {
        List<Matchup> matchups = matchupRepository.findByGameWeek(week);

        for (Matchup matchup : matchups) {
            double homeScore = calculateTeamScoreForWeek(matchup.getHomeTeam(), week);
            double awayScore = calculateTeamScoreForWeek(matchup.getAwayTeam(), week);

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

    public List<com.fantasyhockey.fantasy_league.dto.MatchupDetailDto> getMatchupDetails(GameWeek week) {
        List<Matchup> matchups = matchupRepository.findByGameWeek(week);

        return matchups.stream().map(matchup -> {
            FantasyTeam home = matchup.getHomeTeam();
            FantasyTeam away = matchup.getAwayTeam();

            // 1. Top Season Player
            Player homeTop = getTopSeasonPlayer(home);
            Player awayTop = getTopSeasonPlayer(away);

            // 2. Top 5 Last Week
            GameWeek lastWeek = findPreviousWeek(week);
            List<com.fantasyhockey.fantasy_league.dto.PlayerWeeklyStatsDto> homeLastWeekTop5;
            List<com.fantasyhockey.fantasy_league.dto.PlayerWeeklyStatsDto> awayLastWeekTop5;

            // Only fetch stats if the previous week is completed
            if (lastWeek != null && lastWeek.isCompleted()) {
                homeLastWeekTop5 = getTopPlayersForWeek(home, lastWeek, 5);
                awayLastWeekTop5 = getTopPlayersForWeek(away, lastWeek, 5);
            } else {
                homeLastWeekTop5 = java.util.Collections.emptyList();
                awayLastWeekTop5 = java.util.Collections.emptyList();
            }

            // 3. Win Probability
            // Simple model: Share of total season points
            double homeTotal = home.getTotalFantasyPoints();
            double awayTotal = away.getTotalFantasyPoints();
            double total = homeTotal + awayTotal;

            int homeProb = 50;
            int awayProb = 50;

            if (total > 0) {
                homeProb = (int) Math.round((homeTotal / total) * 100);
                awayProb = 100 - homeProb;
            }

            // 4. Form & Streak
            List<String> homeForm = getTeamForm(home, week);
            List<String> awayForm = getTeamForm(away, week);
            int homeStreak = getTeamStreak(home, week);
            int awayStreak = getTeamStreak(away, week);

            return com.fantasyhockey.fantasy_league.dto.MatchupDetailDto.builder()
                    .matchup(matchup)
                    .homeTopPlayer(homeTop)
                    .awayTopPlayer(awayTop)
                    .homeLastWeekTop5(homeLastWeekTop5)
                    .awayLastWeekTop5(awayLastWeekTop5)
                    .homeWinProb(homeProb)
                    .awayWinProb(awayProb)
                    .homeForm(homeForm)
                    .awayForm(awayForm)
                    .homeStreak(homeStreak)
                    .awayStreak(awayStreak)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<String> getTeamForm(FantasyTeam team, GameWeek currentWeek) {
        // Fetch all completed matchups for the team before current week
        List<Matchup> pastMatchups = matchupRepository.findAllByTeamAndWeekBefore(team, currentWeek);

        // Sort by week number descending (most recent first)
        pastMatchups
                .sort((m1, m2) -> Integer.compare(m2.getGameWeek().getWeekNumber(), m1.getGameWeek().getWeekNumber()));

        return pastMatchups.stream()
                .limit(5)
                .map(m -> getResultForTeam(m, team))
                .collect(Collectors.toList());
    }

    private int getTeamStreak(FantasyTeam team, GameWeek currentWeek) {
        List<Matchup> pastMatchups = matchupRepository.findAllByTeamAndWeekBefore(team, currentWeek);
        pastMatchups
                .sort((m1, m2) -> Integer.compare(m2.getGameWeek().getWeekNumber(), m1.getGameWeek().getWeekNumber()));

        int streak = 0;
        for (Matchup m : pastMatchups) {
            String result = getResultForTeam(m, team);

            if ("TBD".equals(result))
                continue; // Skip uncompleted weeks

            if (result.equals("V")) {
                if (streak >= 0)
                    streak++;
                else
                    return streak; // Streak broken
            } else {
                if (streak <= 0)
                    streak--;
                else
                    return streak; // Streak broken
            }
        }
        return streak;
    }

    private String getResultForTeam(Matchup m, FantasyTeam team) {
        if (!m.getGameWeek().isCompleted()) {
            return "TBD";
        }

        boolean isHome = m.getHomeTeam().getId().equals(team.getId());
        double myScore = isHome ? m.getHomeScore() : m.getAwayScore();
        double oppScore = isHome ? m.getAwayScore() : m.getHomeScore();

        if (myScore > oppScore)
            return "V";
        if (myScore < oppScore)
            return "P";
        // Tie logic (if any, currently we have OT logic but result is stored as score)
        // Assuming no draws for now based on previous OT logic implementation
        return "P";
    }

    public List<com.fantasyhockey.fantasy_league.dto.MatchupDetailDto> getMatchupDetails() {
        return getMatchupDetails(scheduleService.getCurrentWeek());
    }

    private Player getTopSeasonPlayer(FantasyTeam team) {
        if (team == null || team.getPlayers().isEmpty())
            return null;
        return team.getPlayers().stream()
                .max(Comparator.comparingDouble(Player::getSeasonFantasyPoints))
                .orElse(null);
    }

    private GameWeek findPreviousWeek(GameWeek current) {
        if (current.getWeekNumber() <= 1)
            return current; // Fallback to current if first week

        try {
            return scheduleService.getWeekByNumber(current.getWeekNumber() - 1);
        } catch (Exception e) {
            return current;
        }
    }

    private List<com.fantasyhockey.fantasy_league.dto.PlayerWeeklyStatsDto> getTopPlayersForWeek(FantasyTeam team,
            GameWeek week, int limit) {
        if (team == null || team.getPlayers().isEmpty() || week == null)
            return List.of();

        List<Long> playerIds = team.getPlayers().stream().map(Player::getId).collect(Collectors.toList());

        // Use the custom repository method
        List<Object[]> results = playerStatsRepository.findTopPlayersByPointsInDateRange(
                playerIds, week.getStartDate(), week.getEndDate());

        return results.stream()
                .limit(limit)
                .map(obj -> {
                    Player player = (Player) obj[0];
                    // We need to fetch G/A/P specifically for this week
                    // We might need to query stats details or aggregate them here.
                    // For now, let's assume we can get them.
                    // Ideally, findTopPlayersByPointsInDateRange should return aggregated stats.
                    // Since we can't easily change the repo query return type without checking it,
                    // let's do a separate calculation or update the query.
                    // Fix for ClassCastException: sum() might return Long or Double depending on DB
                    Number pointsNum = (Number) obj[1];
                    double points = pointsNum != null ? pointsNum.doubleValue() : 0.0;

                    int goals = calculateStatForPlayer(player, week, "goals");
                    int assists = calculateStatForPlayer(player, week, "assists");
                    int plusMinus = calculateStatForPlayer(player, week, "plusMinus");
                    int shots = calculateStatForPlayer(player, week, "shots");
                    int blockedShots = calculateStatForPlayer(player, week, "blockedShots");
                    int hits = calculateStatForPlayer(player, week, "hits");
                    int pim = calculateStatForPlayer(player, week, "pim");

                    return new com.fantasyhockey.fantasy_league.dto.PlayerWeeklyStatsDto(player, goals, assists,
                            plusMinus, shots, blockedShots, hits, pim, points);
                })
                .collect(Collectors.toList());
    }

    private int calculateStatForPlayer(Player player, GameWeek week, String statType) {
        return playerStatsRepository
                .findByPlayerIdAndDateBetween(player.getId(), week.getStartDate(), week.getEndDate())
                .stream()
                .mapToInt(stats -> {
                    if ("goals".equals(statType))
                        return stats.getGoals();
                    if ("assists".equals(statType))
                        return stats.getAssists();
                    if ("plusMinus".equals(statType))
                        return stats.getPlusMinus();
                    if ("shots".equals(statType))
                        return stats.getShots();
                    if ("blockedShots".equals(statType))
                        return stats.getBlockedShots();
                    if ("hits".equals(statType))
                        return stats.getHits();
                    if ("pim".equals(statType))
                        return stats.getPim();
                    return 0;
                })
                .sum();
    }
}
