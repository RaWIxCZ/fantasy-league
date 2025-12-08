package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Represents game-by-game statistics for a player.
 * Each record captures a player's performance in a single NHL game,
 * including both traditional stats and calculated fantasy points.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStats {

    // ==================== Primary Identifier ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== References ====================

    /**
     * The player these statistics belong to.
     */
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    /**
     * The date when this game was played.
     */
    private LocalDate date;

    /**
     * NHL game ID to prevent duplicate stat entries.
     * Used to ensure we don't process the same game twice.
     */
    private Long gameId;

    // ==================== Skater Statistics ====================

    private int goals;
    private int assists;

    @Column(columnDefinition = "integer default 0")
    private int plusMinus;

    @Column(columnDefinition = "integer default 0")
    private int shots;

    @Column(columnDefinition = "integer default 0")
    private int blockedShots;

    @Column(columnDefinition = "integer default 0")
    private int hits;

    /**
     * Penalty minutes (PIM).
     */
    @Column(columnDefinition = "integer default 0")
    private int pim;

    // ==================== Goalie Statistics ====================

    /**
     * Number of saves made (goalies only).
     */
    private int saves;

    /**
     * Number of shots faced (goalies only).
     */
    private int shotsAgainst;

    /**
     * Number of goals allowed (goalies only).
     */
    private int goalsAgainst;

    /**
     * Whether the goalie won the game (goalies only).
     */
    private boolean win;

    // ==================== Fantasy Points ====================

    /**
     * Total fantasy points earned in this game.
     * Calculated based on the scoring system:
     * - Goals, assists, shots, hits, blocks, etc. for skaters
     * - Saves, goals against, shutouts, etc. for goalies
     */
    private int fantasyPoints;
}