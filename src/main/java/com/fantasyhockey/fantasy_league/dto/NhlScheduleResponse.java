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
        private Long id; // To je to číslo, co potřebujeme (např. 2025020285)
    }
}