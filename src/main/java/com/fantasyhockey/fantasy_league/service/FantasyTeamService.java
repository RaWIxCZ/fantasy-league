package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.LineupSpot;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.model.User;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.LineupSpotRepository;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import com.fantasyhockey.fantasy_league.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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
    @Transactional
    public void removePlayerFromTeam(Long playerId, String username) {
        // 1. Najdi tým
        FantasyTeam team = getTeamByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tým nenalezen"));

        // 2. Odstraň hráče ze seznamu
        // Česky: "Projdi seznam hráčů a vyhoď toho, jehož ID se rovná playerId"
        boolean removed = team.getPlayers().removeIf(player -> player.getId().equals(playerId));

        if (!removed) {
            throw new RuntimeException("Hráč v týmu nebyl nalezen!");
        }

        // 3. Ulož změnu (JPA si všimne, že se seznam zmenšil, a smaže řádek v propojovací tabulce)
        teamRepository.save(team);
    }

    public List<FantasyTeam> getLeaderboard() {
        return teamRepository.findAllByOrderByTotalFantasyPointsDesc();
    }

    private final LineupSpotRepository lineupRepository; // Přidej do konstruktoru/Lombok

    @Transactional
    public void saveLineupSpot(String username, Long playerId, String slotName) {
        // 1. Najdi tým a hráče
        FantasyTeam team = getTeamByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tým nenalezen"));

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Hráč nenalezen"));

        // 2. Zkontroluj, jestli hráč už nesedí na jiném slotu (ať se nenaklonuje)
        // (Pro jednoduchost to teď vynecháme, ale v realitě bychom měli smazat jeho starou pozici)

        // 3. Podívej se, jestli už na tom slotu někdo není -> pokud ano, přepíšeme ho
        LineupSpot spot = lineupRepository.findByTeamAndSlotName(team, slotName)
                .orElse(new LineupSpot()); // Pokud neexistuje, vytvoříme nový

        // 4. Nastav data
        spot.setTeam(team);
        spot.setPlayer(player);
        spot.setSlotName(slotName);

        lineupRepository.save(spot);
    }

    @Transactional
    public void removePlayerFromSlot(String username, String slotName) {
        FantasyTeam team = getTeamByUsername(username).orElseThrow();
        lineupRepository.deleteByTeamAndSlotName(team, slotName);
    }

    public List<LineupSpot> getTeamLineup(FantasyTeam team) {
        return lineupRepository.findByTeam(team);
    }
}