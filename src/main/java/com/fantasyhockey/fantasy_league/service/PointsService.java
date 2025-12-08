package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.model.PlayerStats;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import com.fantasyhockey.fantasy_league.repository.PlayerStatsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for calculating and managing fantasy points.
 * Processes NHL game statistics and converts them into fantasy points
 * based on a predefined scoring system.
 */
@Service
@RequiredArgsConstructor
public class PointsService {

    private static final Logger logger = LoggerFactory.getLogger(PointsService.class);

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository statsRepository;
    private final FantasyTeamRepository teamRepository;

    // ==================== Scoring System Configuration ====================

    /**
     * Fantasy points scoring rules for skaters and goalies.
     * These constants define how NHL statistics are converted to fantasy points.
     */
    public static class ScoringRules {
        // Skater Scoring
        public static final int POINTS_PER_GOAL = 3;
        public static final int POINTS_PER_ASSIST = 3;
        public static final int POINTS_PER_PLUS_MINUS = 1;
        public static final double POINTS_PER_SHOT = 0.5;
        public static final int POINTS_PER_BLOCK = 1;
        public static final double POINTS_PER_HIT = 0.5;
        public static final double POINTS_PER_PIM = -0.1;
        public static final int POINTS_PER_HATTRICK = 3;

        // Goalie Scoring
        public static final double POINTS_PER_SAVE = 0.2;
        public static final int POINTS_PER_SHUTOUT = 3;
        public static final int POINTS_PER_GOAL_AGAINST = -1;

        // Thresholds
        public static final int HATTRICK_THRESHOLD = 3;
    }

    // ==================== Public Methods ====================

    /**
     * Processes and records statistics for a skater (forward or defenseman).
     * Calculates fantasy points based on goals, assists, and other stats,
     * then updates the player's stats history and team totals.
     * 
     * @param nhlPlayerId  NHL API player ID
     * @param gameId       NHL game ID (used to prevent duplicate processing)
     * @param goals        Number of goals scored
     * @param assists      Number of assists
     * @param plusMinus    Plus/minus rating
     * @param shots        Shots on goal
     * @param blockedShots Blocked shots
     * @param hits         Hits delivered
     * @param pim          Penalty minutes
     * @param date         Date of the game
     */
    @Transactional
    public void addStatsForPlayer(Long nhlPlayerId, Long gameId, int goals, int assists, int plusMinus, int shots,
            int blockedShots, int hits, int pim, LocalDate date) {

        Player player = playerRepository.findByNhlId(nhlPlayerId)
                .orElseThrow(() -> new RuntimeException("Player not found with NHL ID: " + nhlPlayerId));

        // Check for duplicate game processing
        if (statsRepository.existsByPlayerIdAndGameId(player.getId(), gameId)) {
            logger.warn("⚠️ Game {} already processed for player {}. Skipping.", gameId, player.getLastName());
            return;
        }

        // Calculate fantasy points
        int fantasyPoints = calculateSkaterPoints(goals, assists, plusMinus, shots, blockedShots, hits, pim);

        // Save player stats
        PlayerStats stats = createSkaterStats(player, gameId, date, goals, assists, plusMinus,
                shots, blockedShots, hits, pim, fantasyPoints);
        statsRepository.save(stats);

        // Update team totals
        updateTeamPoints(player, fantasyPoints);

        logger.info("✅ Points recorded: {} ({}pts)", player.getLastName(), fantasyPoints);
    }

    /**
     * Processes and records statistics for a goalie.
     * Calculates fantasy points based on saves, goals against, and shutouts,
     * then updates the player's stats history and team totals.
     * 
     * @param nhlPlayerId  NHL API player ID
     * @param gameId       NHL game ID (used to prevent duplicate processing)
     * @param saves        Number of saves made
     * @param shotsAgainst Number of shots faced
     * @param isWinner     Whether the goalie won the game
     * @param date         Date of the game
     */
    @Transactional
    public void addGoalieStatsForPlayer(Long nhlPlayerId, Long gameId, int saves, int shotsAgainst, boolean isWinner,
            LocalDate date) {
        Player player = playerRepository.findByNhlId(nhlPlayerId)
                .orElseThrow(() -> new RuntimeException("Goalie not found with NHL ID: " + nhlPlayerId));

        // Check for duplicate game processing
        if (statsRepository.existsByPlayerIdAndGameId(player.getId(), gameId)) {
            logger.warn("⚠️ Game {} already processed for goalie {}. Skipping.", gameId, player.getLastName());
            return;
        }

        int goalsAgainst = shotsAgainst - saves;
        boolean isShutout = (goalsAgainst == 0 && shotsAgainst > 0);

        // Calculate fantasy points
        int fantasyPoints = calculateGoaliePoints(saves, goalsAgainst, isShutout);

        // Save goalie stats
        PlayerStats stats = createGoalieStats(player, gameId, date, saves, shotsAgainst,
                goalsAgainst, isWinner, fantasyPoints);
        statsRepository.save(stats);

        // Update team totals
        updateTeamPoints(player, fantasyPoints);

        logger.info("✅ Points recorded for goalie: {} ({}pts)", player.getLastName(), fantasyPoints);
    }

    /**
     * Resets all player statistics and team point totals.
     * Used for season resets or data cleanup.
     */
    @Transactional
    public void resetAllStats() {
        statsRepository.deleteAll();
        List<FantasyTeam> teams = teamRepository.findAll();
        for (FantasyTeam team : teams) {
            team.setTotalFantasyPoints(0);
            teamRepository.save(team);
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Calculates fantasy points for a skater based on game statistics.
     */
    private int calculateSkaterPoints(int goals, int assists, int plusMinus, int shots,
            int blockedShots, int hits, int pim) {
        double points = 0;

        // Core scoring stats
        points += goals * ScoringRules.POINTS_PER_GOAL;
        points += assists * ScoringRules.POINTS_PER_ASSIST;

        // Defensive and physical stats
        points += plusMinus * ScoringRules.POINTS_PER_PLUS_MINUS;
        points += shots * ScoringRules.POINTS_PER_SHOT;
        points += blockedShots * ScoringRules.POINTS_PER_BLOCK;
        points += hits * ScoringRules.POINTS_PER_HIT;
        points += pim * ScoringRules.POINTS_PER_PIM;

        // Bonus for hat trick
        if (goals >= ScoringRules.HATTRICK_THRESHOLD) {
            points += ScoringRules.POINTS_PER_HATTRICK;
        }

        return (int) Math.round(points);
    }

    /**
     * Calculates fantasy points for a goalie based on game statistics.
     */
    private int calculateGoaliePoints(int saves, int goalsAgainst, boolean isShutout) {
        double points = 0;

        // Saves (e.g., 30 saves * 0.2 = 6 points)
        points += saves * ScoringRules.POINTS_PER_SAVE;

        // Goals against (e.g., 2 goals * -1 = -2 points)
        points += goalsAgainst * ScoringRules.POINTS_PER_GOAL_AGAINST;

        // Shutout bonus
        if (isShutout) {
            points += ScoringRules.POINTS_PER_SHUTOUT;
        }

        return (int) Math.round(points);
    }

    /**
     * Creates a PlayerStats entity for a skater.
     */
    private PlayerStats createSkaterStats(Player player, Long gameId, LocalDate date,
            int goals, int assists, int plusMinus, int shots,
            int blockedShots, int hits, int pim, int fantasyPoints) {
        PlayerStats stats = new PlayerStats();
        stats.setPlayer(player);
        stats.setGameId(gameId);
        stats.setDate(date);
        stats.setGoals(goals);
        stats.setAssists(assists);
        stats.setPlusMinus(plusMinus);
        stats.setShots(shots);
        stats.setBlockedShots(blockedShots);
        stats.setHits(hits);
        stats.setPim(pim);
        stats.setFantasyPoints(fantasyPoints);
        return stats;
    }

    /**
     * Creates a PlayerStats entity for a goalie.
     */
    private PlayerStats createGoalieStats(Player player, Long gameId, LocalDate date,
            int saves, int shotsAgainst, int goalsAgainst,
            boolean isWinner, int fantasyPoints) {
        PlayerStats stats = new PlayerStats();
        stats.setPlayer(player);
        stats.setGameId(gameId);
        stats.setDate(date);
        stats.setSaves(saves);
        stats.setShotsAgainst(shotsAgainst);
        stats.setGoalsAgainst(goalsAgainst);
        stats.setWin(isWinner);
        stats.setFantasyPoints(fantasyPoints);
        return stats;
    }

    /**
     * Updates fantasy point totals for all teams that have this player on their
     * roster.
     */
    private void updateTeamPoints(Player player, int fantasyPoints) {
        List<FantasyTeam> allTeams = teamRepository.findAll();
        for (FantasyTeam team : allTeams) {
            if (team.getPlayers().contains(player)) {
                team.setTotalFantasyPoints(team.getTotalFantasyPoints() + fantasyPoints);
                teamRepository.save(team);
            }
        }
    }
}