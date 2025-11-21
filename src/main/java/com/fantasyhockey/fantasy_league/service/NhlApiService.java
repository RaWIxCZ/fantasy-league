package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.dto.NhlBoxscoreResponse;
import com.fantasyhockey.fantasy_league.dto.NhlPlayerDto;
import com.fantasyhockey.fantasy_league.dto.NhlRosterResponse;
import com.fantasyhockey.fantasy_league.dto.NhlScheduleResponse;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor // Lombok vytvoří konstruktor pro repository (Dependency Injection)
public class NhlApiService {

    private final PlayerRepository playerRepository;
    private final PointsService pointsService; // Přidat: Potřebujeme zapisovat body
    private final RestTemplate restTemplate = new RestTemplate(); // Nástroj pro volání URL

    // Seznam všech 32 týmů NHL
    private static final String[] NHL_TEAMS = {
            "ANA", "BOS", "BUF", "CGY", "CAR", "CHI", "COL", "CBJ", "DAL",
            "DET", "EDM", "FLA", "LAK", "MIN", "MTL", "NSH", "NJD", "NYI", "NYR",
            "OTT", "PHI", "PIT", "SJS", "SEA", "STL", "TBL", "TOR", "UTA", "VAN",
            "VGK", "WSH", "WPG"
    };

    // HROMADNÝ IMPORT (Tuto metodu budeš volat z Controlleru)
    public void importAllTeams() {
        System.out.println("🚀 Začínám import všech týmů...");
        for (String teamAbbrev : NHL_TEAMS) {
            importRosterForTeam(teamAbbrev);

            // Malá pauza, abychom nedostali ban od NHL za spamování serveru
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }
        System.out.println("✅ Import všech týmů dokončen.");
    }

    // Původní importRoster, ale s parametrem
    private void importRosterForTeam(String teamAbbrev) {
        System.out.println("Stahuji soupisku pro: " + teamAbbrev);
        String url = "https://api-web.nhle.com/v1/roster/" + teamAbbrev + "/current";

        try {
            NhlRosterResponse response = restTemplate.getForObject(url, NhlRosterResponse.class);
            if (response == null) return;

            List<NhlPlayerDto> allPlayers = new ArrayList<>();
            allPlayers.addAll(response.getForwards());
            allPlayers.addAll(response.getDefensemen());
            allPlayers.addAll(response.getGoalies());

            for (NhlPlayerDto dto : allPlayers) {
                // Posíláme zkratku týmu (teamAbbrev), kterou už máme v parametru této metody
                savePlayerToDb(dto, teamAbbrev);
            }
        } catch (Exception e) {
            System.out.println("Chyba u týmu " + teamAbbrev + ": " + e.getMessage());
        }
    }

    public void updateStatsFromYesterday() {
        // 1. Zjistíme včerejší datum
        String yesterday = LocalDate.now().minusDays(1).toString(); // např. "2025-11-18"

        String url = "https://api-web.nhle.com/v1/schedule/" + yesterday;
        System.out.println("🔍 Hledám zápasy pro datum: " + yesterday);

        try {
            NhlScheduleResponse response = restTemplate.getForObject(url, NhlScheduleResponse.class);

            if (response != null && response.getGameWeek() != null) {
                // NHL API vrací "týden", musíme najít ten správný den v seznamu
                for (NhlScheduleResponse.GameWeekDto day : response.getGameWeek()) {
                    if (day.getDate().equals(yesterday)) {
                        // Našli jsme včerejší den, projdeme zápasy
                        for (NhlScheduleResponse.GameDto game : day.getGames()) {
                            System.out.println("🚀 Nalezen zápas ID: " + game.getId() + ". Zpracovávám...");
                            processGame(game.getId()); // Tady voláme tu tvoji metodu!
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Chyba při stahování rozvrhu: " + e.getMessage());
        }
    }

    private void savePlayerToDb(NhlPlayerDto dto, String teamCode) {
        // Zkontrolujeme, jestli už hráč v DB není (podle NHL ID)
        Player player = playerRepository.findByNhlId(dto.getId())
                .orElse(new Player()); // Pokud není, vytvoříme nového. Pokud je, aktualizujeme ho.

        // Mapování DTO -> Entity
        player.setNhlId(dto.getId());
        // Pozor: JSON vrací jméno v objektu, musíme se zanořit
        player.setFirstName(dto.getFirstNameObj().getDefaultName());
        player.setLastName(dto.getLastNameObj().getDefaultName());
        player.setPosition(dto.getPositionCode());
        player.setTeamName(teamCode);
        player.setHeadshotUrl(dto.getHeadshot());

        // Uložení
        playerRepository.save(player);
    }
    public void processGame(Long gameId) {
        String url = "https://api-web.nhle.com/v1/gamecenter/" + gameId + "/boxscore";

        try {
            NhlBoxscoreResponse response = restTemplate.getForObject(url, NhlBoxscoreResponse.class);

            if (response == null || response.getPlayerByGameStats() == null) {
                return;
            }

            processTeamStats(response.getPlayerByGameStats().getAwayTeam(), gameId);
            processTeamStats(response.getPlayerByGameStats().getHomeTeam(), gameId);

        } catch (Exception e) {
            System.out.println("Chyba při stahování zápasu " + gameId + ": " + e.getMessage());
        }
    }

    // Pomocná metoda, která projde seznamy útočníků, obránců a brankářů
    private void processTeamStats(NhlBoxscoreResponse.TeamStats teamStats, Long gameId) {
        if (teamStats == null) return;

        List<NhlBoxscoreResponse.PlayerStatDto> allPlayers = new ArrayList<>();

        if (teamStats.getForwards() != null) {
            allPlayers.addAll(teamStats.getForwards());
        }
        if (teamStats.getDefensemen() != null) {
            allPlayers.addAll(teamStats.getDefensemen());
        }
        if (teamStats.getGoalies() != null) {
            allPlayers.addAll(teamStats.getGoalies());
        }

        for (NhlBoxscoreResponse.PlayerStatDto p : allPlayers) {
            // Pokud hráč bodoval (má gól nebo asistenci)
            if (p.getGoals() > 0 || p.getAssists() > 0) {
                // Pošleme to do PointsService
                // Poznámka: Try-catch, protože hráč nemusí být v naší DB (může to být nováček)
                try {
                    pointsService.addStatsForPlayer(
                            p.getPlayerId(),
                            gameId,
                            p.getGoals(),
                            p.getAssists(),
                            LocalDate.now() // Tady ideálně parsovat datum ze zápasu, ale now() pro opravu stačí
                    );
                } catch (Exception e) {
                    System.out.println("⚠️ CHYBA u hráče ID " + p.getPlayerId() + ": " + e.getMessage());
                }
            }
        }
    }

    public void importSeasonData() {
        // Začátek sezóny NHL 25/26 (7. října 2025)
        LocalDate startDate = LocalDate.of(2025, 10, 7);
        LocalDate today = LocalDate.now(); // Dnešek

        System.out.println("🚀 START: Bezpečný hromadný import sezóny od " + startDate + " do " + today);

        LocalDate currentDate = startDate;

        // 1. SMYČKA PŘES DNY
        while (currentDate.isBefore(today) || currentDate.equals(today)) {
            String dateStr = currentDate.toString(); // yyyy-MM-dd
            System.out.println("📅 Zpracovávám den: " + dateStr);

            processScheduleForDate(dateStr);

            currentDate = currentDate.plusDays(1);

            // PAUZA MEZI DNY (1 sekunda)
            // Dáváme serveru čas na vydechnutí
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        }

        System.out.println("🏁 KONEC: Import sezóny dokončen.");
    }

    private void processScheduleForDate(String dateStr) {
        String url = "https://api-web.nhle.com/v1/schedule/" + dateStr;
        try {
            NhlScheduleResponse response = restTemplate.getForObject(url, NhlScheduleResponse.class);

            if (response != null && response.getGameWeek() != null) {
                for (NhlScheduleResponse.GameWeekDto day : response.getGameWeek()) {
                    if (day.getDate().equals(dateStr)) {

                        // 2. SMYČKA PŘES ZÁPASY V TOM DNI
                        for (NhlScheduleResponse.GameDto game : day.getGames()) {
                            // Zavoláme logiku pro stažení Boxscore a uložení bodů
                            processGame(game.getId());

                            // PAUZA MEZI ZÁPASY (300 ms)
                            // Abychom neposlali 10 requestů v jedné milisekundě
                            try { Thread.sleep(300); } catch (InterruptedException e) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Chyba importu pro " + dateStr + ": " + e.getMessage());
        }
    }
}