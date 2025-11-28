package com.fantasyhockey.fantasy_league.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhlScheduleResponse {

    private List<GameWeekDto> gameWeek;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameWeekDto {
        private String date;
        private List<GameDto> games;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameDto {
        private Long id;
        private String startTimeUTC;
        private String gameState; // "FUT", "LIVE", "OFF", "FINAL"
        private TeamDto awayTeam;
        private TeamDto homeTeam;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamDto {
        private int id;
        private String abbrev;
        private Integer score;
        private String logo;
    }
}