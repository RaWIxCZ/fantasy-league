package com.fantasyhockey.fantasy_league.dto;

import com.fantasyhockey.fantasy_league.model.Player;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerWeeklyStatsDto {
    private Player player;
    private int goals;
    private int assists;
    private int plusMinus;
    private int shots;
    private int blockedShots;
    private int hits;
    private int pim;
    private double fantasyPoints;
}
