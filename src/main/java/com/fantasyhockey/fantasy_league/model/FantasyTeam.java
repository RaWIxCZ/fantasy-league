package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FantasyTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teamName;

    // VAZBA 1:1 (Jeden tým patří jednomu uživateli)
    @OneToOne
    @JoinColumn(name = "user_id") // V tabulce fantasy_team vznikne sloupec user_id
    private User user;

    // VAZBA M:N (Tým má seznam hráčů)
    @ManyToMany
    @JoinTable(
            name = "team_players", // Vznikne pomocná tabulka "team_players"
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private List<Player> players = new ArrayList<>();

    // Metoda pro přidání hráče (pomocná)
    public void addPlayer(Player player) {
        this.players.add(player);
    }
}