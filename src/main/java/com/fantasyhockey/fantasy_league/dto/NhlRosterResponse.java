package com.fantasyhockey.fantasy_league.dto;

import lombok.Data;
import java.util.List;

@Data
public class NhlRosterResponse {
    private List<NhlPlayerDto> forwards;
    private List<NhlPlayerDto> defensemen;
    private List<NhlPlayerDto> goalies;
}