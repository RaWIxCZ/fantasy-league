package com.fantasyhockey.fantasy_league.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhlPlayerLandingDto {
    private boolean isActive;
}