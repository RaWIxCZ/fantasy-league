package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineupSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kterému týmu tato pozice patří?
    @ManyToOne
    @JoinColumn(name = "team_id")
    private FantasyTeam team;

    // Který hráč tam stojí?
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    // Název slotu (např. "LW", "C", "GK", "D1", "D2")
    // Později pro více lajn: "L1_LW", "L2_C"
    private String slotName;
}