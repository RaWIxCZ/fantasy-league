package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Matchup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_week_id")
    private GameWeek gameWeek;

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private FantasyTeam homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private FantasyTeam awayTeam;

    private double homeScore;
    private double awayScore;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private FantasyTeam winner; // Null if tie or not finished
}
