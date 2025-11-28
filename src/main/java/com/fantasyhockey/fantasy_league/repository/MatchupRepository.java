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
}
