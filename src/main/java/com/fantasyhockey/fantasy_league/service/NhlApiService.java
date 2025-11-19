package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.dto.NhlPlayerDto;
import com.fantasyhockey.fantasy_league.dto.NhlRosterResponse;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor // Lombok vytvoří konstruktor pro repository (Dependency Injection)
public class NhlApiService {

    private final PlayerRepository playerRepository;
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

        // Uložení
        playerRepository.save(player);
    }
}