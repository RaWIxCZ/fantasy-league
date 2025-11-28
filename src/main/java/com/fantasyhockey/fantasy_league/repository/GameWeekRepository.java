package com.fantasyhockey.fantasy_league.repository;

import com.fantasyhockey.fantasy_league.model.GameWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GameWeekRepository extends JpaRepository<GameWeek, Long> {
    Optional<GameWeek> findByWeekNumber(int weekNumber);

    Optional<GameWeek> findByIsCurrentTrue();

    // Find week that contains a specific date
    Optional<GameWeek> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date, LocalDate date2);
}
