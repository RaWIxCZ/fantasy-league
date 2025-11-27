package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kterého hráče se to týká?
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    // Kdy se zápas hrál?
    private LocalDate date;

    // Musíme si pamatovat ID zápasu z NHL, abychom ho nenačítali 2x
    private Long gameId;

    // Statistiky z toho zápasu
    private int goals;
    private int assists;

    // Statistiky pro brankáře
    private int saves;
    private int shotsAgainst;
    private int goalsAgainst;
    private boolean win;


    // Kolik fantasy bodů za to dostal? (např. Gól=5, Asistence=3 -> Celkem 8)
    private int fantasyPoints;
}