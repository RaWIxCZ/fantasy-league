package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents an NHL player in the fantasy league system.
 * Stores basic player information and provides calculated season statistics
 * based on game-by-game performance data.
 */
@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    // ==================== Primary Identifiers ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Official NHL player ID for API integration.
     */
    @Column(unique = true)
    private Long nhlId;

    // ==================== Basic Player Information ====================

    private String firstName;
    private String lastName;
    private String position;
    private String teamName;

    // ==================== Visual & UI Fields ====================

    /**
     * URL to the player's headshot image.
     */
    private String headshotUrl;

    // ==================== Status Fields ====================

    /**
     * Indicates if the player is currently injured.
     * Injured players may have limited or no playing time.
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean injured = false;

    // ==================== Relationships ====================

    /**
     * Historical game-by-game statistics for this player.
     * Loaded lazily to improve performance.
     */
    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    private List<PlayerStats> matchHistory;

    // ==================== Season Statistics (Skaters) ====================

    /**
     * Calculates total goals scored this season.
     * 
     * @return sum of all goals from match history
     */
    @Transient
    public int getSeasonGoals() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getGoals).sum();
    }

    /**
     * Calculates total assists this season.
     * 
     * @return sum of all assists from match history
     */
    @Transient
    public int getSeasonAssists() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getAssists).sum();
    }

    /**
     * Calculates total NHL points (goals + assists) this season.
     * Note: This returns fantasy points, not NHL points.
     * 
     * @return total fantasy points earned
     */
    @Transient
    public int getSeasonPoints() {
        return getSeasonFantasyPoints();
    }

    /**
     * Calculates total fantasy points earned this season.
     * 
     * @return sum of all fantasy points from match history
     */
    @Transient
    public int getSeasonFantasyPoints() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getFantasyPoints).sum();
    }

    /**
     * Calculates average fantasy points per game.
     * 
     * @return average points, or 0.0 if no games played
     */
    @Transient
    public double getAverageFantasyPoints() {
        if (matchHistory == null || matchHistory.isEmpty()) {
            return 0.0;
        }
        return (double) getSeasonFantasyPoints() / matchHistory.size();
    }

    /**
     * Calculates total plus/minus rating this season.
     * 
     * @return sum of all plus/minus values from match history
     */
    @Transient
    public int getSeasonPlusMinus() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getPlusMinus).sum();
    }

    /**
     * Calculates total shots on goal this season.
     * 
     * @return sum of all shots from match history
     */
    @Transient
    public int getSeasonShots() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getShots).sum();
    }

    /**
     * Calculates total blocked shots this season.
     * 
     * @return sum of all blocked shots from match history
     */
    @Transient
    public int getSeasonBlockedShots() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getBlockedShots).sum();
    }

    /**
     * Calculates total hits this season.
     * 
     * @return sum of all hits from match history
     */
    @Transient
    public int getSeasonHits() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getHits).sum();
    }

    /**
     * Calculates total penalty minutes this season.
     * 
     * @return sum of all penalty minutes from match history
     */
    @Transient
    public int getSeasonPim() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getPim).sum();
    }

    // ==================== Season Statistics (Goalies) ====================

    /**
     * Calculates goals against average (GAA) for goalies.
     * GAA = (total goals against * 60) / total minutes played
     * Assumes 60 minutes per game for simplification.
     * 
     * @return GAA, or 0.0 if no games played
     */
    @Transient
    public double getSeasonGaa() {
        if (matchHistory == null || matchHistory.isEmpty())
            return 0.0;
        int totalGoalsAgainst = matchHistory.stream().mapToInt(PlayerStats::getGoalsAgainst).sum();
        long totalMinutes = matchHistory.size() * 60;
        if (totalMinutes == 0)
            return 0.0;
        return (double) totalGoalsAgainst * 60 / totalMinutes;
    }

    /**
     * Calculates save percentage for goalies.
     * Save% = total saves / total shots against
     * 
     * @return save percentage (0.0 to 1.0), or 0.0 if no shots faced
     */
    @Transient
    public double getSeasonSavePctg() {
        if (matchHistory == null || matchHistory.isEmpty())
            return 0.0;
        int totalSaves = matchHistory.stream().mapToInt(PlayerStats::getSaves).sum();
        int totalShotsAgainst = matchHistory.stream().mapToInt(PlayerStats::getShotsAgainst).sum();
        if (totalShotsAgainst == 0)
            return 0.0;
        return (double) totalSaves / totalShotsAgainst;
    }
}