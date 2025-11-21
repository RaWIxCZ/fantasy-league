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
}