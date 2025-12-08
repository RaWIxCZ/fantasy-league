package com.fantasyhockey.fantasy_league.repository;

import com.fantasyhockey.fantasy_league.model.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for managing PlayerStats entities.
 * Provides methods for querying game-by-game player statistics.
 */
@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {

        /**
         * Finds all stats records for a specific player.
         * 
         * @param playerId the player's ID
         * @return list of all stats for the player
         */
        List<PlayerStats> findByPlayerId(Long playerId);

        /**
         * Checks if stats already exist for a player in a specific game.
         * Used to prevent duplicate stat entries when processing NHL games.
         * 
         * @param playerId the player's ID
         * @param gameId   the NHL game ID
         * @return true if stats exist, false otherwise
         */
        boolean existsByPlayerIdAndGameId(Long playerId, Long gameId);

        /**
         * Finds all stats for a player within a date range.
         * 
         * @param playerId  the player's ID
         * @param startDate start of the date range (inclusive)
         * @param endDate   end of the date range (inclusive)
         * @return list of stats within the date range
         */
        List<PlayerStats> findByPlayerIdAndDateBetween(Long playerId, java.time.LocalDate startDate,
                        java.time.LocalDate endDate);

        /**
         * Finds top players by total fantasy points within a date range.
         * Returns aggregated statistics for each player, ordered by total points
         * descending.
         * 
         * Result array contains:
         * [0] Player entity
         * [1] Total fantasy points (sum)
         * [2] Total goals (sum)
         * [3] Total assists (sum)
         * [4] Total plus/minus (sum)
         * [5] Total shots (sum)
         * [6] Total blocked shots (sum)
         * [7] Total hits (sum)
         * [8] Total penalty minutes (sum)
         * 
         * @param playerIds list of player IDs to include
         * @param startDate start of the date range
         * @param endDate   end of the date range
         * @return list of Object arrays containing player and aggregated stats
         */
        @org.springframework.data.jpa.repository.Query("SELECT s.player, " +
                        "SUM(s.fantasyPoints) as totalPoints, " +
                        "SUM(s.goals) as totalGoals, " +
                        "SUM(s.assists) as totalAssists, " +
                        "SUM(s.plusMinus) as totalPlusMinus, " +
                        "SUM(s.shots) as totalShots, " +
                        "SUM(s.blockedShots) as totalBlockedShots, " +
                        "SUM(s.hits) as totalHits, " +
                        "SUM(s.pim) as totalPim " +
                        "FROM PlayerStats s " +
                        "WHERE s.player.id IN :playerIds AND s.date BETWEEN :startDate AND :endDate " +
                        "GROUP BY s.player " +
                        "ORDER BY totalPoints DESC")
        List<Object[]> findTopPlayersByPointsInDateRange(List<Long> playerIds, java.time.LocalDate startDate,
                        java.time.LocalDate endDate);
}