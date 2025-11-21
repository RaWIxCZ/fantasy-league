package com.fantasyhockey.fantasy_league.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
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

        @JsonProperty("forwards")
        private List<PlayerStatDto> forwards = new ArrayList<>();

        @JsonProperty("goalies")
        private List<PlayerStatDto> goalies = new ArrayList<>();

        @JsonProperty("defense")
        @JsonAlias({"defensemen", "defencemen"})
        private List<PlayerStatDto> defensemen = new ArrayList<>();

        @JsonAnySetter
        public void handleUnknown(String key, Object value) {
            // Pokud klíč obsahuje "defen", tak je to ono! (defensemen/defencemen)
            if (key.toLowerCase().contains("defen")) {
                // Musíme to ručně přetypovat, protože Jackson vrací obecný Object (LinkedHashMap)
                // Toto je trochu "dirty", ale funkční.
                // Lepší je použít ObjectMapper.convertValue, ale pro jednoduchost:

                // Vraťme se raději k @JsonAlias, ale PŘIDEJME VÍCE MOŽNOSTÍ
            }
        }
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