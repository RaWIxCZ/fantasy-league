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
    // Přidán parametr gameId
    public void addStatsForPlayer(Long nhlPlayerId, Long gameId, int goals, int assists, LocalDate date) {

        Player player = playerRepository.findByNhlId(nhlPlayerId)
                .orElseThrow(() -> new RuntimeException("Hráč nenalezen ID: " + nhlPlayerId));

        // 1. KONTROLA DUPLICITY (Ochrana proti Pastrňákově 58 bodům)
        if (statsRepository.existsByPlayerIdAndGameId(player.getId(), gameId)) {
            System.out.println("⚠️ Zápas " + gameId + " už byl pro hráče " + player.getLastName() + " započítán. Přeskakuji.");
            return;
        }

        // 2. Výpočet bodů
        int fantasyPoints = (goals * POINTS_PER_GOAL) + (assists * POINTS_PER_ASSIST);

        // 3. Uložení
        PlayerStats stats = new PlayerStats();
        stats.setPlayer(player);
        stats.setGameId(gameId); // Uložíme ID zápasu
        stats.setDate(date);
        stats.setGoals(goals);
        stats.setAssists(assists);
        stats.setFantasyPoints(fantasyPoints);

        statsRepository.save(stats);

        // 4. Aktualizace týmu
        List<FantasyTeam> allTeams = teamRepository.findAll();
        for (FantasyTeam team : allTeams) {
            if (team.getPlayers().contains(player)) {
                team.setTotalFantasyPoints(team.getTotalFantasyPoints() + fantasyPoints);
                teamRepository.save(team);
            }
        }

        System.out.println("✅ Body započteny: " + player.getLastName() + " (" + fantasyPoints + "b)");
    }
}