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

@Service
@RequiredArgsConstructor
public class PointsService {

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository statsRepository;
    private final FantasyTeamRepository teamRepository;

    // Nastavení bodování
    private static final int POINTS_PER_GOAL = 5;
    private static final int POINTS_PER_ASSIST = 3;

    @Transactional
    public void addStatsForPlayer(Long playerId, int goals, int assists, LocalDate date) {
        // 1. Najdeme hráče
        // Hledáme podle sloupce nhlId, který jsme definovali v Repository už na začátku
        Player player = playerRepository.findByNhlId(playerId)
                .orElseThrow(() -> new RuntimeException("Hráč nenalezen"));

        // 2. Spočítáme fantasy body
        int fantasyPoints = (goals * POINTS_PER_GOAL) + (assists * POINTS_PER_ASSIST);

        // 3. Uložíme záznam do historie (PlayerStats)
        PlayerStats stats = new PlayerStats();
        stats.setPlayer(player);
        stats.setDate(date);
        stats.setGoals(goals);
        stats.setAssists(assists);
        stats.setFantasyPoints(fantasyPoints);

        statsRepository.save(stats);

        // 4. Aktualizujeme skóre všem fantasy týmům, které tohoto hráče mají
        // Tohle je trochu pokročilejší SQL logika, ale uděláme to jednoduše v Javě:

        // Najdeme všechny týmy (v reálu bychom dělali efektivnější dotaz, ale pro začátek OK)
        List<FantasyTeam> allTeams = teamRepository.findAll();

        for (FantasyTeam team : allTeams) {
            // Pokud tým obsahuje tohoto hráče
            if (team.getPlayers().contains(player)) {
                // Přičteme body k celkovému skóre
                team.setTotalFantasyPoints(team.getTotalFantasyPoints() + fantasyPoints);
                teamRepository.save(team);
            }
        }

        System.out.println("Body přičteny! Hráč: " + player.getLastName() + ", Body: " + fantasyPoints);
    }
}