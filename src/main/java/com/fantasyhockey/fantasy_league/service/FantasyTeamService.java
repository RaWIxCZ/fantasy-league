package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.User;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FantasyTeamService {

    private final FantasyTeamRepository teamRepository;
    private final UserRepository userRepository;

    // Získání týmu pro přihlášeného uživatele
    public Optional<FantasyTeam> getTeamByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return teamRepository.findByUser(user);
    }

    // Vytvoření nového týmu
    public void createTeam(String teamName, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();

        FantasyTeam team = new FantasyTeam();
        team.setTeamName(teamName);
        team.setUser(user);

        teamRepository.save(team);
    }
}