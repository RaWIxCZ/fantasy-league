package com.fantasyhockey.fantasy_league.repository;

import com.fantasyhockey.fantasy_league.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    // Tady se děje magie Spring Data JPA.
    // Tím, že dědíme od JpaRepository, automaticky získáváme metody:
    // .save(Player p)
    // .findAll()
    // .findById(Long id)
    // .delete(Player p)

    // Můžeme si dadefinovat vlastní, např. hledání podle NHL ID:
    Optional<Player> findByNhlId(Long nhlId);

    // 1. Najdi hráče podle týmu (seřazené podle jména)
    List<Player> findByTeamNameOrderByLastNameAsc(String teamName);

    // 2. Najdi unikátní názvy týmů (pro roletku)
    // "SELECT DISTINCT p.teamName FROM Player p"
    @Query("SELECT DISTINCT p.teamName FROM Player p ORDER BY p.teamName")
    List<String> findDistinctTeamNames();
}