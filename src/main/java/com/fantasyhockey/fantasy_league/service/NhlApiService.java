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
    private static final String[] NHL_TEAMS = {
            "ANA", "BOS", "BUF", "CGY", "CAR", "CHI", "COL", "CBJ", "DAL",
            "DET", "EDM", "FLA", "LAK", "MIN", "MTL", "NSH", "NJD", "NYI", "NYR",
            "OTT", "PHI", "PIT", "SJS", "SEA", "STL", "TBL", "TOR", "UTA", "VAN",
            "VGK", "WSH", "WPG"
    };

    private static final int DELAY_BETWEEN_TEAMS_MS = 200;
    private static final int DELAY_BETWEEN_DAYS_MS = 1000;
    private static final int DELAY_BETWEEN_GAMES_MS = 300;
    private static final LocalDate SEASON_START_DATE = LocalDate.of(2025, 10, 7);

    private final PlayerRepository playerRepository;
    private final PointsService pointsService;
    private final EspnScraperService espnScraperService;
    private final RestTemplate restTemplate;

    public void importAllTeams() {
        logger.info("🚀 Začínám import všech týmů...");
        updatePlayerInjuries();

        for (String teamAbbrev : NHL_TEAMS) {
            importRosterForTeam(teamAbbrev);
            try {
                Thread.sleep(DELAY_BETWEEN_TEAMS_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Import interrupted while waiting between teams", e);
            }
        }
        logger.info("✅ Import všech týmů dokončen.");
    }

    private void importRosterForTeam(String teamAbbrev) {
        logger.info("Stahuji soupisku pro: {}", teamAbbrev);
        String url = "https://api-web.nhle.com/v1/roster/" + teamAbbrev + "/current";

        try {
            NhlRosterResponse response = restTemplate.getForObject(url, NhlRosterResponse.class);
            if (response == null)
                return;

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
                            processGame(game.getId(), LocalDate.parse(yesterday));
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
        String pos = dto.getPositionCode();
        if ("L".equals(pos))
            pos = "LW";
        if ("R".equals(pos))
            pos = "RW";
        player.setPosition(pos);
        player.setTeamName(teamCode);
        player.setHeadshotUrl(dto.getHeadshot());

        playerRepository.save(player);
    }

    public void processGame(Long gameId, LocalDate gameDate) {
        String url = "https://api-web.nhle.com/v1/gamecenter/" + gameId + "/boxscore";

        try {
            NhlBoxscoreResponse response = restTemplate.getForObject(url, NhlBoxscoreResponse.class);

            if (response == null || response.getPlayerByGameStats() == null) {
                return;
            }

            processTeamStats(response.getPlayerByGameStats().getAwayTeam(), gameId,
                    response.getAwayTeam().getScore() > response.getHomeTeam().getScore(), gameDate);
            processTeamStats(response.getPlayerByGameStats().getHomeTeam(), gameId,
                    response.getHomeTeam().getScore() > response.getAwayTeam().getScore(), gameDate);

        } catch (Exception e) {
            logger.error("Chyba při stahování zápasu {}: {}", gameId, e.getMessage());
        }
    }

    private void processTeamStats(NhlBoxscoreResponse.TeamStats teamStats, Long gameId, boolean isWinner,
            LocalDate gameDate) {
        if (teamStats == null)
            return;

        List<NhlBoxscoreResponse.PlayerStatDto> skaters = new ArrayList<>();
        if (teamStats.getForwards() != null) {
            skaters.addAll(teamStats.getForwards());
        }
        if (teamStats.getDefense() != null) {
            skaters.addAll(teamStats.getDefense());
        }

        for (NhlBoxscoreResponse.PlayerStatDto p : skaters) {
            // Započítáme, pokud má hráč alespoň nějakou statistiku (nejen góly/asistence)
            if (p.getGoals() > 0 || p.getAssists() > 0 || p.getShots() > 0 || p.getBlockedShots() > 0
                    || p.getHits() > 0 || p.getPim() > 0 || p.getPlusMinus() != 0) {
                try {
                    pointsService.addStatsForPlayer(
                            p.getPlayerId(),
                            gameId,
                            p.getGoals(),
                            p.getAssists(),
                            p.getPlusMinus(),
                            p.getShots(),
                            p.getBlockedShots(),
                            p.getHits(),
                            p.getPim(),
                            gameDate);
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
                            gameDate);
                } catch (Exception e) {
                    logger.warn("⚠️ CHYBA u brankáře ID {}: {}", g.getPlayerId(), e.getMessage());
                }
            }
        }
    }

    public void resetAndImportSeasonData() {
        logger.info("🧹 RESET: Mazání všech statistik a bodů týmů...");
        pointsService.resetAllStats();
        logger.info("✅ RESET: Hotovo. Spouštím import sezóny...");
        importSeasonData();
    }

    public void updateStatsForDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("🚀 START: Aktualizace statistik od {} do {}", startDate, endDate);

        LocalDate currentDate = startDate;

        while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
            String dateStr = currentDate.toString();
            logger.info("📅 Zpracovávám den: {}", dateStr);

            processScheduleForDate(dateStr);

            currentDate = currentDate.plusDays(1);

            try {
                Thread.sleep(DELAY_BETWEEN_DAYS_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Import interrupted while waiting between days", e);
            }
        }

        logger.info("🏁 KONEC: Aktualizace statistik dokončena.");
    }

    public void importSeasonData() {
        LocalDate startDate = SEASON_START_DATE;
        LocalDate today = LocalDate.now();
        updateStatsForDateRange(startDate, today);
    }

    private static final Map<String, String> TEAM_NAMES = Map.ofEntries(
            Map.entry("ANA", "Anaheim Ducks"),
            Map.entry("BOS", "Boston Bruins"),
            Map.entry("BUF", "Buffalo Sabres"),
            Map.entry("CGY", "Calgary Flames"),
            Map.entry("CAR", "Carolina Hurricanes"),
            Map.entry("CHI", "Chicago Blackhawks"),
            Map.entry("COL", "Colorado Avalanche"),
            Map.entry("CBJ", "Columbus Blue Jackets"),
            Map.entry("DAL", "Dallas Stars"),
            Map.entry("DET", "Detroit Red Wings"),
            Map.entry("EDM", "Edmonton Oilers"),
            Map.entry("FLA", "Florida Panthers"),
            Map.entry("LAK", "Los Angeles Kings"),
            Map.entry("MIN", "Minnesota Wild"),
            Map.entry("MTL", "Montreal Canadiens"),
            Map.entry("NSH", "Nashville Predators"),
            Map.entry("NJD", "New Jersey Devils"),
            Map.entry("NYI", "New York Islanders"),
            Map.entry("NYR", "New York Rangers"),
            Map.entry("OTT", "Ottawa Senators"),
            Map.entry("PHI", "Philadelphia Flyers"),
            Map.entry("PIT", "Pittsburgh Penguins"),
            Map.entry("SJS", "San Jose Sharks"),
            Map.entry("SEA", "Seattle Kraken"),
            Map.entry("STL", "St. Louis Blues"),
            Map.entry("TBL", "Tampa Bay Lightning"),
            Map.entry("TOR", "Toronto Maple Leafs"),
            Map.entry("UTA", "Utah Hockey Club"),
            Map.entry("VAN", "Vancouver Canucks"),
            Map.entry("VGK", "Vegas Golden Knights"),
            Map.entry("WSH", "Washington Capitals"),
            Map.entry("WPG", "Winnipeg Jets"));

    public Map<String, String> getTeamNames() {
        return TEAM_NAMES;
    }

    public NhlScheduleResponse getSchedule(LocalDate date) {
        String url = "https://api-web.nhle.com/v1/schedule/" + date.toString();
        try {
            return restTemplate.getForObject(url, NhlScheduleResponse.class);
        } catch (Exception e) {
            logger.error("Chyba při stahování rozvrhu pro {}: {}", date, e.getMessage());
            return null;
        }
    }

    private void processScheduleForDate(String dateStr) {
        String url = "https://api-web.nhle.com/v1/schedule/" + dateStr;
        try {
            NhlScheduleResponse response = restTemplate.getForObject(url, NhlScheduleResponse.class);

            if (response != null && response.getGameWeek() != null) {
                for (NhlScheduleResponse.GameWeekDto day : response.getGameWeek()) {
                    if (day.getDate().equals(dateStr)) {
                        for (NhlScheduleResponse.GameDto game : day.getGames()) {
                            processGame(game.getId(), LocalDate.parse(dateStr));
                            try {
                                Thread.sleep(DELAY_BETWEEN_GAMES_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                logger.warn("Import interrupted while waiting between games", e);
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