package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user's fantasy hockey team.
 * Each team belongs to one user and contains a roster of NHL players.
 * Teams compete against each other in weekly matchups to earn league points.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FantasyTeam {

    // ==================== Primary Identifier ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Team Identity ====================

    /**
     * Custom name chosen by the team owner.
     */
    private String teamName;

    /**
     * URL to the team's logo image.
     */
    private String logoUrl;

    // ==================== Season Statistics ====================

    /**
     * Total fantasy points accumulated by all players on the team.
     * Updated whenever player stats are processed.
     */
    private int totalFantasyPoints = 0;

    /**
     * Number of matchup wins in regulation time.
     */
    @Column(columnDefinition = "integer default 0")
    private int wins = 0;

    /**
     * Number of matchup losses in regulation time.
     */
    @Column(columnDefinition = "integer default 0")
    private int losses = 0;

    /**
     * Number of matchup wins in overtime/shootout.
     */
    @Column(columnDefinition = "integer default 0")
    private int otWins = 0;

    /**
     * Number of matchup losses in overtime/shootout.
     */
    @Column(columnDefinition = "integer default 0")
    private int otLosses = 0;

    /**
     * Total league points earned (used for standings).
     * Calculated based on wins, losses, and OT results.
     */
    @Column(columnDefinition = "integer default 0")
    private int leaguePoints = 0;

    // ==================== Relationships ====================

    /**
     * The user who owns this fantasy team.
     * One-to-one relationship: each team has exactly one owner.
     */
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * The roster of NHL players on this fantasy team.
     * Many-to-many relationship: players can be on multiple teams,
     * and teams have multiple players.
     * Uses a join table "team_players" to manage the relationship.
     */
    @ManyToMany
    @JoinTable(name = "team_players", joinColumns = @JoinColumn(name = "team_id"), inverseJoinColumns = @JoinColumn(name = "player_id"))
    private List<Player> players = new ArrayList<>();

    // ==================== Helper Methods ====================

    /**
     * Adds a player to the team's roster.
     * 
     * @param player the player to add
     */
    public void addPlayer(Player player) {
        this.players.add(player);
    }
}