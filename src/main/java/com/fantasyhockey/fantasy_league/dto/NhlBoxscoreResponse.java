package com.fantasyhockey.fantasy_league.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Důležité: Ignoruj všechno smetí okolo, co nepotřebujeme
public class NhlBoxscoreResponse {

    private PlayerByGameStats playerByGameStats;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerByGameStats {
        private TeamStats awayTeam;
        private TeamStats homeTeam;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamStats {
        // Hráči jsou rozděleni do skupin, musíme je pak spojit
        private List<PlayerStatDto> forwards = new ArrayList<>();
        private List<PlayerStatDto> defensemen = new ArrayList<>();
        private List<PlayerStatDto> goalies = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerStatDto {
        private Long playerId;

        // Někdy tam ty hodnoty nejsou (když hráč nic neudělal), proto default 0
        private int goals = 0;
        private int assists = 0;

        // Jméno pro kontrolu v logu
        private NameDto name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NameDto {
        @JsonProperty("default")
        private String defaultName;
    }
}