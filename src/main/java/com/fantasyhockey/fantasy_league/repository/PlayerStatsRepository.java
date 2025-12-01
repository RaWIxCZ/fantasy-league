package com.fantasyhockey.fantasy_league.repository;

import com.fantasyhockey.fantasy_league.model.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {

    List<PlayerStats> findByPlayerId(Long playerId);

    // Kontrola duplicity
    boolean existsByPlayerIdAndGameId(Long playerId, Long gameId);

    List<PlayerStats> findByPlayerIdAndDateBetween(Long playerId, java.time.LocalDate startDate,
            java.time.LocalDate endDate);

    @org.springframework.data.jpa.repository.Query("SELECT s.player, SUM(s.fantasyPoints) as totalPoints " +
            "FROM PlayerStats s " +
            "WHERE s.player.id IN :playerIds AND s.date BETWEEN :startDate AND :endDate " +
            "GROUP BY s.player " +
            "ORDER BY totalPoints DESC")
    List<Object[]> findTopPlayersByPointsInDateRange(List<Long> playerIds, java.time.LocalDate startDate,
            java.time.LocalDate endDate);
}