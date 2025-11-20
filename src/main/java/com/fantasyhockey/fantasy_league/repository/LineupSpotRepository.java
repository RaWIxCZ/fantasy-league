package com.fantasyhockey.fantasy_league.repository;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.LineupSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineupSpotRepository extends JpaRepository<LineupSpot, Long> {

    // Najdi obsazený slot v konkrétním týmu (např. kdo je GK v mém týmu?)
    Optional<LineupSpot> findByTeamAndSlotName(FantasyTeam team, String slotName);

    // Najdi celou sestavu týmu (abychom ji mohli vykreslit)
    List<LineupSpot> findByTeam(FantasyTeam team);

    // Smazání hráče ze slotu (když klikneš na křížek)
    void deleteByTeamAndSlotName(FantasyTeam team, String slotName);
}