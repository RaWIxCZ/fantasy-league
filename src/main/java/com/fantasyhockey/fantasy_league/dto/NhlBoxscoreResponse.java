package com.fantasyhockey.fantasy_league.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhlBoxscoreResponse {

    private PlayerByGameStats playerByGameStats;
    private Team awayTeam;
    private Team homeTeam;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerByGameStats {
        private TeamStats awayTeam;
        private TeamStats homeTeam;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamStats {
        private List<PlayerStatDto> forwards = new ArrayList<>();
        private List<PlayerStatDto> defense = new ArrayList<>();
        private List<GoalieStatDto> goalies = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerStatDto {
        private Long playerId;
        private int goals = 0;
        private int assists = 0;
        @JsonProperty("plusMinus")
        private int plusMinus = 0;
        @JsonProperty("sog")
        private int shots = 0;
        @JsonProperty("blockedShots")
        private int blockedShots = 0;
        @JsonProperty("hits")
        private int hits = 0;
        @JsonProperty("pim")
        private int pim = 0;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoalieStatDto {
        private Long playerId;
        private int saves = 0;
        @JsonProperty("shotsAgainst")
        private int shotsAgainst = 0;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private int score;
    }
}