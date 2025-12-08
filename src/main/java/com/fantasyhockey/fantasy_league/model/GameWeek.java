package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Represents a game week in the fantasy league schedule.
 * Game weeks typically run from Monday to Sunday, with Week 1 having
 * a special schedule (Tuesday to Sunday) to align with NHL season start.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sequential week number (1, 2, 3, etc.).
     */
    private int weekNumber;

    /**
     * First day of the game week (inclusive).
     */
    private LocalDate startDate;

    /**
     * Last day of the game week (inclusive).
     */
    private LocalDate endDate;

    /**
     * Indicates if this is the current active game week.
     * Only one week should be current at any time.
     */
    private boolean isCurrent;

    /**
     * Indicates if this game week has finished.
     * Completed weeks have final scores and won't change.
     */
    private boolean isCompleted;
}
