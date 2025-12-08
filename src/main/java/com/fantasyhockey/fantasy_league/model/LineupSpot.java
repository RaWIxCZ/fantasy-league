package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single position in a fantasy team's active lineup.
 * Each lineup spot has a designated position (e.g., "LW", "C", "D1", "GK")
 * and can be filled by one player at a time.
 * 
 * Example slot names:
 * - Forwards: "LW", "C", "RW"
 * - Defense: "D1", "D2"
 * - Goalies: "GK"
 * - Multi-line setups: "L1_LW", "L2_C", etc.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineupSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The fantasy team that owns this lineup spot.
     */
    @ManyToOne
    @JoinColumn(name = "team_id")
    private FantasyTeam team;

    /**
     * The player currently assigned to this lineup spot.
     * Can be null if the spot is empty.
     */
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    /**
     * The position identifier for this spot.
     * Examples: "LW", "C", "RW", "D1", "D2", "GK"
     * For multi-line configurations: "L1_LW", "L2_C", etc.
     */
    private String slotName;
}