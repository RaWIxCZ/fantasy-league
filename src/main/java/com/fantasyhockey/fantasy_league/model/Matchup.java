package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a head-to-head matchup between two fantasy teams in a specific
 * game week.
 * Teams accumulate points based on their players' performances during the week,
 * and the team with more points wins the matchup.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Matchup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The game week when this matchup takes place.
     */
    @ManyToOne
    @JoinColumn(name = "game_week_id")
    private GameWeek gameWeek;

    /**
     * The home team in this matchup.
     */
    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private FantasyTeam homeTeam;

    /**
     * The away team in this matchup.
     */
    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private FantasyTeam awayTeam;

    /**
     * Total fantasy points scored by the home team during this week.
     */
    private double homeScore;

    /**
     * Total fantasy points scored by the away team during this week.
     */
    private double awayScore;

    /**
     * The winning team of this matchup.
     * Null if the matchup is not yet completed or ended in a tie.
     */
    @ManyToOne
    @JoinColumn(name = "winner_id")
    private FantasyTeam winner;
}
