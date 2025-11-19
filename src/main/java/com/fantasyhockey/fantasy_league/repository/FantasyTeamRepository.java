package com.fantasyhockey.fantasy_league.repository;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FantasyTeamRepository extends JpaRepository<FantasyTeam, Long> {
    // Najdi tým podle uživatele (abychom věděli, jestli už nějaký má)
    Optional<FantasyTeam> findByUser(User user);
    // NOVÁ METODA:
    // "Find All" (Najdi vše)
    // "By Order By" (Seřaď podle)
    // "TotalFantasyPoints" (Název sloupce)
    // "Desc" (Descending = Sestupně, tedy od největšího po nejmenší)
    List<FantasyTeam> findAllByOrderByTotalFantasyPointsDesc();
}