package com.fantasyhockey.fantasy_league.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NhlPlayerDto {
    private Long id;

    @JsonProperty("firstName")
    private NameDto firstNameObj;

    private String headshot;

    @JsonProperty("lastName")
    private NameDto lastNameObj;

    private String positionCode;

    private boolean isActive;

    @Data
    public static class NameDto {
        @JsonProperty("default")
        private String defaultName;
    }
}