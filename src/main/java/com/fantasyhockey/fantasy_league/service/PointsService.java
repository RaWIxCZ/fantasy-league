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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PointsService {

    private static final Logger logger = LoggerFactory.getLogger(PointsService.class);

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository statsRepository;
    private final FantasyTeamRepository teamRepository;

    // Nastavení bodování
    private static final int POINTS_PER_GOAL = 5;
    private static final int POINTS_PER_ASSIST = 3;

    // Brankáři (Rebalance)
    private static final double POINTS_PER_SAVE = 0.2;
    private static final int POINTS_PER_WIN = 4;
    private static final int POINTS_PER_SHUTOUT = 3;
    private static final int POINTS_PER_GOAL_AGAINST = -1;

    @Transactional
    // Přidán parametr gameId
    public void addStatsForPlayer(Long nhlPlayerId, Long gameId, int goals, int assists, LocalDate date) {

        Player player = playerRepository.findByNhlId(nhlPlayerId)
                .orElseThrow(() -> new RuntimeException("Hráč nenalezen ID: " + nhlPlayerId));

        // 1. KONTROLA DUPLICITY (Ochrana proti Pastrňákově 58 bodům)
        if (statsRepository.existsByPlayerIdAndGameId(player.getId(), gameId)) {
            logger.warn("⚠️ Zápas {} už byl pro hráče {} započítán. Přeskakuji.", gameId, player.getLastName());
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

        logger.info("✅ Body započteny: {} ({}b)", player.getLastName(), fantasyPoints);
    }

    @Transactional
    public void addGoalieStatsForPlayer(Long nhlPlayerId, Long gameId, int saves, int shotsAgainst, boolean isWinner,
            LocalDate date) {
        Player player = playerRepository.findByNhlId(nhlPlayerId)
                .orElseThrow(() -> new RuntimeException("Brankář nenalezen ID: " + nhlPlayerId));

        if (statsRepository.existsByPlayerIdAndGameId(player.getId(), gameId)) {
            logger.warn("⚠️ Zápas {} už byl pro brankáře {} započítán. Přeskakuji.", gameId, player.getLastName());
            return;
        }

        int goalsAgainst = shotsAgainst - saves;
        boolean isShutout = (goalsAgainst == 0 && shotsAgainst > 0);

        // Výpočet bodů pro brankáře
        // Zákroky (např. 30 * 0.2 = 6 bodů)
        double points = (saves * POINTS_PER_SAVE);

        // Inkasované góly (např. 2 * -1 = -2 body)
        points += (goalsAgainst * POINTS_PER_GOAL_AGAINST);

        if (isWinner) {
            points += POINTS_PER_WIN;
        }
        if (isShutout) {
            points += POINTS_PER_SHUTOUT;
        }

        // Zaokrouhlení na celé číslo (int)
        int fantasyPoints = (int) Math.round(points);

        PlayerStats stats = new PlayerStats();
        stats.setPlayer(player);
        stats.setGameId(gameId);
        stats.setDate(date);
        stats.setSaves(saves);
        stats.setShotsAgainst(shotsAgainst);
        stats.setGoalsAgainst(goalsAgainst);
        stats.setWin(isWinner);
        stats.setFantasyPoints(fantasyPoints);

        statsRepository.save(stats);

        List<FantasyTeam> allTeams = teamRepository.findAll();
        for (FantasyTeam team : allTeams) {
            if (team.getPlayers().contains(player)) {
                team.setTotalFantasyPoints(team.getTotalFantasyPoints() + fantasyPoints);
                teamRepository.save(team);
            }
        }

        logger.info("✅ Body započteny pro brankáře: {} ({}b)", player.getLastName(), fantasyPoints);
    }
}