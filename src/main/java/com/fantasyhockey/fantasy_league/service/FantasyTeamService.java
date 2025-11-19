package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.model.User;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository; // Import!
import com.fantasyhockey.fantasy_league.repository.UserRepository;
import jakarta.transaction.Transactional; // Import!
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FantasyTeamService {

    private final FantasyTeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository; // Přidáno: Musíme mít přístup k hráčům

    public Optional<FantasyTeam> getTeamByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return teamRepository.findByUser(user);
    }

    public void createTeam(String teamName, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        FantasyTeam team = new FantasyTeam();
        team.setTeamName(teamName);
        team.setUser(user);
        teamRepository.save(team);
    }

    // NOVÁ METODA
    @Transactional // Důležité: Zaručí, že se celá operace provede najednou (nebo vůbec)
    public void addPlayerToTeam(Long playerId, String username) {
        // 1. Najdeme tým uživatele
        FantasyTeam team = getTeamByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nejdřív si musíš vytvořit tým!"));

        // 2. Najdeme hráče, kterého chce přidat
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Hráč neexistuje"));

        // 3. Validace: Je už hráč v týmu?
        if (team.getPlayers().contains(player)) {
            throw new RuntimeException("Tento hráč už ve tvém týmu je.");
        }

        // 4. Validace: Má tým místo? (Max 6 hráčů)
        if (team.getPlayers().size() >= 6) {
            throw new RuntimeException("Tvůj tým je plný! (Max 6 hráčů)");
        }

        // 5. Přidáme hráče a uložíme
        team.getPlayers().add(player);
        teamRepository.save(team);
    }
}