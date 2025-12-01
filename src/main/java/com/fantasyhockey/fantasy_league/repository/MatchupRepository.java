package com.fantasyhockey.fantasy_league.repository;

import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.model.Matchup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchupRepository extends JpaRepository<Matchup, Long> {
    List<Matchup> findByGameWeek(GameWeek gameWeek);

    List<Matchup> findByGameWeekOrderByIdAsc(GameWeek gameWeek);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM Matchup m WHERE (m.homeTeam = :team OR m.awayTeam = :team) AND m.gameWeek.weekNumber < :#{#week.weekNumber}")
    List<Matchup> findAllByTeamAndWeekBefore(
            @org.springframework.data.repository.query.Param("team") com.fantasyhockey.fantasy_league.model.FantasyTeam team,
            @org.springframework.data.repository.query.Param("week") GameWeek week);
}
