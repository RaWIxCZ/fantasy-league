package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    private String headshotUrl;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean injured = false;

    @Transient
    public int getSeasonGoals() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getGoals).sum();
    }

    @Transient
    public int getSeasonAssists() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getAssists).sum();
    }

    @Transient
    public int getSeasonPoints() {
        return getSeasonFantasyPoints();
    }

    @Transient
    public int getSeasonFantasyPoints() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getFantasyPoints).sum();
    }

    @Transient
    public double getAverageFantasyPoints() {
        if (matchHistory == null || matchHistory.isEmpty()) {
            return 0.0;
        }
        return (double) getSeasonFantasyPoints() / matchHistory.size();
    }

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

    @Transient
    public int getSeasonPlusMinus() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getPlusMinus).sum();
    }

    @Transient
    public int getSeasonShots() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getShots).sum();
    }

    @Transient
    public int getSeasonBlockedShots() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getBlockedShots).sum();
    }

    @Transient
    public int getSeasonHits() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getHits).sum();
    }

    @Transient
    public int getSeasonPim() {
        if (matchHistory == null)
            return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getPim).sum();
    }

    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    private List<PlayerStats> matchHistory;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long nhlId;

    private String firstName;
    private String lastName;

    private String teamName;
    private String position;
}