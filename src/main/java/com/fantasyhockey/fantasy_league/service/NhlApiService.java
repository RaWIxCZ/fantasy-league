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

    // Zatím natvrdo pro Boston (BOS), později to uděláme pro všechny
    private final String API_URL = "https://api-web.nhle.com/v1/roster/BOS/current";

    public void importRoster() {
        // 1. Stáhneme JSON z internetu a převedeme na Java objekty
        NhlRosterResponse response = restTemplate.getForObject(API_URL, NhlRosterResponse.class);

        if (response == null) {
            System.out.println("Chyba: Nic se nestáhlo!");
            return;
        }

        // 2. Sloučíme všechny seznamy (útočníky, obránce, brankáře) do jednoho
        List<NhlPlayerDto> allPlayers = new ArrayList<>();
        allPlayers.addAll(response.getForwards());
        allPlayers.addAll(response.getDefensemen());
        allPlayers.addAll(response.getGoalies());

        // 3. Uložíme každého hráče do databáze
        for (NhlPlayerDto dto : allPlayers) {
            savePlayerToDb(dto);
        }

        System.out.println("Hotovo! Uloženo " + allPlayers.size() + " hráčů.");
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

    private void savePlayerToDb(NhlPlayerDto dto) {
        // Zkontrolujeme, jestli už hráč v DB není (podle NHL ID)
        Player player = playerRepository.findByNhlId(dto.getId())
                .orElse(new Player()); // Pokud není, vytvoříme nového. Pokud je, aktualizujeme ho.

        // Mapování DTO -> Entity
        player.setNhlId(dto.getId());
        // Pozor: JSON vrací jméno v objektu, musíme se zanořit
        player.setFirstName(dto.getFirstNameObj().getDefaultName());
        player.setLastName(dto.getLastNameObj().getDefaultName());
        player.setPosition(dto.getPositionCode());
        player.setTeamName("Boston Bruins"); // Zatím natvrdo
        // Uložíme URL přímo ze zdroje (NHL nám pošle tu správnou)
        player.setHeadshotUrl(dto.getHeadshot());

        // Uložení
        playerRepository.save(player);
    }
    public void processGame(Long gameId) {
        String url = "https://api-web.nhle.com/v1/gamecenter/" + gameId + "/boxscore";

        try {
            System.out.println("Stahuji zápas ID: " + gameId);
            NhlBoxscoreResponse response = restTemplate.getForObject(url, NhlBoxscoreResponse.class);

            if (response == null || response.getPlayerByGameStats() == null) {
                System.out.println("Žádná data pro zápas " + gameId);
                return;
            }

            // Zpracujeme domácí i hosty
            processTeamStats(response.getPlayerByGameStats().getAwayTeam());
            processTeamStats(response.getPlayerByGameStats().getHomeTeam());

        } catch (Exception e) {
            System.out.println("Chyba při stahování zápasu " + gameId + ": " + e.getMessage());
        }
    }

    // Pomocná metoda, která projde seznamy útočníků, obránců a brankářů
    private void processTeamStats(NhlBoxscoreResponse.TeamStats teamStats) {
        if (teamStats == null) return;

        List<NhlBoxscoreResponse.PlayerStatDto> allPlayers = new ArrayList<>();
        allPlayers.addAll(teamStats.getForwards());
        allPlayers.addAll(teamStats.getDefensemen());
        allPlayers.addAll(teamStats.getGoalies());

        for (NhlBoxscoreResponse.PlayerStatDto p : allPlayers) {
            // Pokud hráč bodoval (má gól nebo asistenci)
            if (p.getGoals() > 0 || p.getAssists() > 0) {
                // Pošleme to do PointsService
                // Poznámka: Try-catch, protože hráč nemusí být v naší DB (může to být nováček)
                try {
                    pointsService.addStatsForPlayer(
                            p.getPlayerId(),
                            p.getGoals(),
                            p.getAssists(),
                            LocalDate.now().minusDays(1) // Dáváme včerejší datum (simulace)
                    );
                } catch (Exception e) {
                    // Hráče nemáme v DB, ignorujeme ho (nebo bychom ho mohli importovat)
                    // System.out.println("Neznámý hráč ID: " + p.getPlayerId());
                }
            }
        }
    }

    public void importSeasonData() {
        // Začátek sezóny NHL 25/26 (přibližně 4. října 2025)
        LocalDate startDate = LocalDate.of(2025, 10, 4);
        LocalDate today = LocalDate.now();

        System.out.println("🚀 START: Hromadný import sezóny od " + startDate + " do " + today);

        // Smyčka přes všechny dny
        LocalDate currentDate = startDate;
        while (currentDate.isBefore(today)) {
            String dateStr = currentDate.toString(); // yyyy-MM-dd

            System.out.println("📅 Zpracovávám den: " + dateStr);

            // Využijeme logiku, kterou už máme pro denní update
            // Ale musíme ji trochu upravit, abychom nekopírovali kód.
            // Ideálně vytvořit pomocnou metodu 'processScheduleForDate(String date)'
            processScheduleForDate(dateStr);

            currentDate = currentDate.plusDays(1);

            // Malá pauza, ať nezahltíme NHL servery (slušnost)
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }

        System.out.println("🏁 KONEC: Import sezóny dokončen.");
    }

    // Tuto metodu vytvoř vyříznutím logiky z updateStatsFromYesterday
    private void processScheduleForDate(String dateStr) {
        String url = "https://api-web.nhle.com/v1/schedule/" + dateStr;
        try {
            NhlScheduleResponse response = restTemplate.getForObject(url, NhlScheduleResponse.class);
            if (response != null && response.getGameWeek() != null) {
                for (NhlScheduleResponse.GameWeekDto day : response.getGameWeek()) {
                    if (day.getDate().equals(dateStr)) {
                        for (NhlScheduleResponse.GameDto game : day.getGames()) {
                            // Abychom nestahovali zápasy, co už máme (volitelné, ale dobré)
                            processGame(game.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Chyba importu pro " + dateStr + ": " + e.getMessage());
        }
    }

}