package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.dto.NhlBoxscoreResponse;
import com.fantasyhockey.fantasy_league.dto.NhlPlayerDto;
import com.fantasyhockey.fantasy_league.dto.NhlRosterResponse;
import com.fantasyhockey.fantasy_league.dto.NhlScheduleResponse;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NhlApiService {

    private static final Logger logger = LoggerFactory.getLogger(NhlApiService.class);
    private final PlayerRepository playerRepository;
    private final PointsService pointsService;
    private final EspnScraperService espnScraperService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String[] NHL_TEAMS = {
            "ANA", "BOS", "BUF", "CGY", "CAR", "CHI", "COL", "CBJ", "DAL",
            "DET", "EDM", "FLA", "LAK", "MIN", "MTL", "NSH", "NJD", "NYI", "NYR",
            "OTT", "PHI", "PIT", "SJS", "SEA", "STL", "TBL", "TOR", "UTA", "VAN",
            "VGK", "WSH", "WPG"
    };

    public void importAllTeams() {
        logger.info("🚀 Začínám import všech týmů...");
        updatePlayerInjuries();

        for (String teamAbbrev : NHL_TEAMS) {
            importRosterForTeam(teamAbbrev);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("✅ Import všech týmů dokončen.");
    }

    private void importRosterForTeam(String teamAbbrev) {
        logger.info("Stahuji soupisku pro: {}", teamAbbrev);
        String url = "https://api-web.nhle.com/v1/roster/" + teamAbbrev + "/current";

        try {
            NhlRosterResponse response = restTemplate.getForObject(url, NhlRosterResponse.class);
            if (response == null) return;

            List<NhlPlayerDto> allPlayers = new ArrayList<>();
            allPlayers.addAll(response.getForwards());
            allPlayers.addAll(response.getDefensemen());
            allPlayers.addAll(response.getGoalies());

            for (NhlPlayerDto dto : allPlayers) {
                savePlayerToDb(dto, teamAbbrev);
            }
        } catch (Exception e) {
            logger.error("Chyba u týmu {}: {}", teamAbbrev, e.getMessage());
        }
    }

    public void updatePlayerInjuries() {
        logger.info("Aktualizuji status zranění hráčů z ESPN...");
        Map<String, String> injuredPlayers = espnScraperService.getInjuredPlayers();
        logger.info("Načteno {} zraněných hráčů z ESPN.", injuredPlayers.size());

        List<Player> allPlayers = playerRepository.findAll();
        for (Player player : allPlayers) {
            String playerName = player.getFirstName() + " " + player.getLastName();
            player.setInjured(injuredPlayers.containsKey(playerName));
            playerRepository.save(player);
        }
        logger.info("✅ Status zranění hráčů aktualizován.");
    }

    public void updateStatsFromYesterday() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        String url = "https://api-web.nhle.com/v1/schedule/" + yesterday;
        logger.info("🔍 Hledám zápasy pro datum: {}", yesterday);

        try {
            NhlScheduleResponse response = restTemplate.getForObject(url, NhlScheduleResponse.class);

            if (response != null && response.getGameWeek() != null) {
                for (NhlScheduleResponse.GameWeekDto day : response.getGameWeek()) {
                    if (day.getDate().equals(yesterday)) {
                        for (NhlScheduleResponse.GameDto game : day.getGames()) {
                            logger.info("🚀 Nalezen zápas ID: {}. Zpracovávám...", game.getId());
                            processGame(game.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Chyba při stahování rozvrhu: {}", e.getMessage());
        }
    }

    private void savePlayerToDb(NhlPlayerDto dto, String teamCode) {
        Player player = playerRepository.findByNhlId(dto.getId())
                .orElse(new Player());

        player.setNhlId(dto.getId());
        player.setFirstName(dto.getFirstNameObj().getDefaultName());
        player.setLastName(dto.getLastNameObj().getDefaultName());
        player.setPosition(dto.getPositionCode());
        player.setTeamName(teamCode);
        player.setHeadshotUrl(dto.getHeadshot());

        playerRepository.save(player);
    }

    public void processGame(Long gameId) {
        String url = "https://api-web.nhle.com/v1/gamecenter/" + gameId + "/boxscore";

        try {
            NhlBoxscoreResponse response = restTemplate.getForObject(url, NhlBoxscoreResponse.class);

            if (response == null || response.getPlayerByGameStats() == null) {
                return;
            }

            processTeamStats(response.getPlayerByGameStats().getAwayTeam(), gameId, response.getAwayTeam().getScore() > response.getHomeTeam().getScore());
            processTeamStats(response.getPlayerByGameStats().getHomeTeam(), gameId, response.getHomeTeam().getScore() > response.getAwayTeam().getScore());

        } catch (Exception e) {
            logger.error("Chyba při stahování zápasu {}: {}", gameId, e.getMessage());
        }
    }

    private void processTeamStats(NhlBoxscoreResponse.TeamStats teamStats, Long gameId, boolean isWinner) {
        if (teamStats == null) return;

        List<NhlBoxscoreResponse.PlayerStatDto> skaters = new ArrayList<>();
        if (teamStats.getForwards() != null) {
            skaters.addAll(teamStats.getForwards());
        }
        if (teamStats.getDefense() != null) {
            skaters.addAll(teamStats.getDefense());
        }

        for (NhlBoxscoreResponse.PlayerStatDto p : skaters) {
            if (p.getGoals() > 0 || p.getAssists() > 0) {
                try {
                    pointsService.addStatsForPlayer(
                            p.getPlayerId(),
                            gameId,
                            p.getGoals(),
                            p.getAssists(),
                            LocalDate.now()
                    );
                } catch (Exception e) {
                    logger.warn("⚠️ CHYBA u hráče ID {}: {}", p.getPlayerId(), e.getMessage());
                }
            }
        }

        if (teamStats.getGoalies() != null) {
            for (NhlBoxscoreResponse.GoalieStatDto g : teamStats.getGoalies()) {
                try {
                    pointsService.addGoalieStatsForPlayer(
                            g.getPlayerId(),
                            gameId,
                            g.getSaves(),
                            g.getShotsAgainst(),
                            isWinner,
                            LocalDate.now()
                    );
                } catch (Exception e) {
                    logger.warn("⚠️ CHYBA u brankáře ID {}: {}", g.getPlayerId(), e.getMessage());
                }
            }
        }
    }

    public void importSeasonData() {
        LocalDate startDate = LocalDate.of(2025, 10, 7);
        LocalDate today = LocalDate.now();

        logger.info("🚀 START: Bezpečný hromadný import sezóny od {} do {}", startDate, today);

        LocalDate currentDate = startDate;

        while (currentDate.isBefore(today) || currentDate.equals(today)) {
            String dateStr = currentDate.toString();
            logger.info("📅 Zpracovávám den: {}", dateStr);

            processScheduleForDate(dateStr);

            currentDate = currentDate.plusDays(1);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("🏁 KONEC: Import sezóny dokončen.");
    }

    private void processScheduleForDate(String dateStr) {
        String url = "https://api-web.nhle.com/v1/schedule/" + dateStr;
        try {
            NhlScheduleResponse response = restTemplate.getForObject(url, NhlScheduleResponse.class);

            if (response != null && response.getGameWeek() != null) {
                for (NhlScheduleResponse.GameWeekDto day : response.getGameWeek()) {
                    if (day.getDate().equals(dateStr)) {
                        for (NhlScheduleResponse.GameDto game : day.getGames()) {
                            processGame(game.getId());
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("⚠️ Chyba importu pro {}: {}", dateStr, e.getMessage());
        }
    }
}