package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.model.Matchup;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.GameWeekRepository;
import com.fantasyhockey.fantasy_league.repository.MatchupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing the fantasy league schedule and matchups.
 * Handles game week creation and round-robin matchup generation.
 */
@Service
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    // ==================== Season Configuration ====================

    /**
     * Season start date (first day of Week 1).
     * Week 1 starts on Tuesday to align with NHL season opener.
     */
    private static final LocalDate SEASON_START_DATE = LocalDate.of(2025, 10, 7);

    /**
     * End date of Week 1 (Sunday).
     * Week 1 is shorter than other weeks (Tue-Sun instead of Mon-Sun).
     */
    private static final LocalDate WEEK_1_END_DATE = LocalDate.of(2025, 10, 12);

    /**
     * Total number of game weeks in the season.
     */
    private static final int TOTAL_WEEKS = 20;

    /**
     * Number of days in a standard game week (Monday to Sunday).
     */
    private static final int DAYS_PER_WEEK = 7;

    // ==================== Dependencies ====================

    private final GameWeekRepository gameWeekRepository;
    private final MatchupRepository matchupRepository;
    private final FantasyTeamRepository fantasyTeamRepository;

    public ScheduleService(GameWeekRepository gameWeekRepository, MatchupRepository matchupRepository,
            FantasyTeamRepository fantasyTeamRepository) {
        this.gameWeekRepository = gameWeekRepository;
        this.matchupRepository = matchupRepository;
        this.fantasyTeamRepository = fantasyTeamRepository;
    }

    // ==================== Public Methods ====================

    /**
     * Initializes the season schedule if not already created.
     * Creates all game weeks and generates round-robin matchups for all teams.
     * If Week 1 exists with incorrect start date, resets the entire schedule.
     */
    @Transactional
    public void initializeSeason() {
        // Validate existing schedule - reset if Week 1 start date is incorrect
        gameWeekRepository.findByWeekNumber(1).ifPresent(week1 -> {
            if (!week1.getStartDate().equals(SEASON_START_DATE)) {
                matchupRepository.deleteAll();
                gameWeekRepository.deleteAll();
            }
        });

        // Skip if season already initialized
        if (gameWeekRepository.count() > 0) {
            return;
        }

        // Create game weeks
        List<GameWeek> weeks = createGameWeeks();

        // Generate matchups for all teams
        List<FantasyTeam> teams = fantasyTeamRepository.findAll();
        if (teams.size() >= 2) {
            generateRoundRobinSchedule(teams, weeks);
        }
    }

    /**
     * Gets the current active game week.
     * 
     * @return the current game week
     * @throws RuntimeException if no current week is found
     */
    public GameWeek getCurrentWeek() {
        return gameWeekRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("No current game week found"));
    }

    /**
     * Gets a game week by its week number.
     * 
     * @param number the week number (1-based)
     * @return the game week
     * @throws RuntimeException if week not found
     */
    public GameWeek getWeekByNumber(int number) {
        return gameWeekRepository.findByWeekNumber(number)
                .orElseThrow(() -> new RuntimeException("Week " + number + " not found"));
    }

    /**
     * Updates the current and completed status of all game weeks based on today's
     * date.
     * This should be called daily to ensure the correct week is marked as current.
     * 
     * Algorithm:
     * - Completed: end date is before today
     * - Current: not completed AND start date is not after today
     * - Future: start date is after today
     */
    @Transactional
    public void updateGameWeekStatuses() {
        LocalDate today = LocalDate.now();
        List<GameWeek> allWeeks = gameWeekRepository.findAll();

        for (GameWeek week : allWeeks) {
            boolean wasCompleted = week.isCompleted();
            boolean wasCurrent = week.isCurrent();

            // Determine new status
            boolean isCompleted = week.getEndDate().isBefore(today);
            boolean isCurrent = !isCompleted && !week.getStartDate().isAfter(today);

            // Update if changed
            if (wasCompleted != isCompleted || wasCurrent != isCurrent) {
                week.setCompleted(isCompleted);
                week.setCurrent(isCurrent);
                gameWeekRepository.save(week);

                if (isCurrent && !wasCurrent) {
                    logger.info("📅 Week {} is now the current game week ({} to {})",
                            week.getWeekNumber(), week.getStartDate(), week.getEndDate());
                }
                if (isCompleted && !wasCompleted) {
                    logger.info("✅ Week {} has been marked as completed", week.getWeekNumber());
                }
            }
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Creates all game weeks for the season.
     * Week 1 runs Tuesday-Sunday, all other weeks run Monday-Sunday.
     * 
     * @return list of created game weeks
     */
    private List<GameWeek> createGameWeeks() {
        List<GameWeek> weeks = new ArrayList<>();
        LocalDate startDate = SEASON_START_DATE;
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= TOTAL_WEEKS; i++) {
            GameWeek week = new GameWeek();
            week.setWeekNumber(i);
            week.setStartDate(startDate);

            // Week 1 has special end date, others are standard 7-day weeks
            if (i == 1) {
                week.setEndDate(WEEK_1_END_DATE);
            } else {
                week.setEndDate(startDate.plusDays(DAYS_PER_WEEK - 1));
            }

            // Determine week status
            boolean isCompleted = week.getEndDate().isBefore(today);
            boolean isCurrent = !isCompleted && !week.getStartDate().isAfter(today);

            week.setCompleted(isCompleted);
            week.setCurrent(isCurrent);

            weeks.add(gameWeekRepository.save(week));

            // Next week starts the day after this week ends
            startDate = week.getEndDate().plusDays(1);
        }

        // Ensure at least one week is marked as current if season hasn't started
        if (weeks.stream().noneMatch(GameWeek::isCurrent) && !weeks.isEmpty()) {
            if (today.isBefore(weeks.get(0).getStartDate())) {
                weeks.get(0).setCurrent(true);
                gameWeekRepository.save(weeks.get(0));
            }
        }

        return weeks;
    }

    /**
     * Generates a round-robin schedule for all teams.
     * Uses the circle method algorithm to ensure fair matchups.
     * 
     * Algorithm:
     * - Fix the first team in position
     * - Rotate all other teams clockwise each round
     * - Pair teams from opposite ends (first with last, second with second-to-last,
     * etc.)
     * - Repeat the cycle to fill all weeks
     * 
     * Example for 6 teams (A,B,C,D,E,F):
     * Round 1: A-F, B-E, C-D
     * Round 2: A-E, F-D, B-C
     * Round 3: A-D, E-C, F-B
     * etc.
     * 
     * @param teams list of fantasy teams
     * @param weeks list of game weeks to fill
     */
    private void generateRoundRobinSchedule(List<FantasyTeam> teams, List<GameWeek> weeks) {
        List<FantasyTeam> rotation = new ArrayList<>(teams);
        int numTeams = rotation.size();
        int numRounds = numTeams - 1; // Number of rounds in one complete cycle
        int halfSize = numTeams / 2;

        int currentWeekIndex = 0;

        // Continue generating matchups until all weeks are filled
        while (currentWeekIndex < weeks.size()) {
            for (int round = 0; round < numRounds; round++) {
                if (currentWeekIndex >= weeks.size())
                    break;

                GameWeek currentWeek = weeks.get(currentWeekIndex);

                // Create matchups for this round
                for (int i = 0; i < halfSize; i++) {
                    FantasyTeam home = rotation.get(i);
                    FantasyTeam away = rotation.get(numTeams - 1 - i);

                    Matchup matchup = new Matchup();
                    matchup.setGameWeek(currentWeek);
                    matchup.setHomeTeam(home);
                    matchup.setAwayTeam(away);
                    matchup.setHomeScore(0);
                    matchup.setAwayScore(0);

                    matchupRepository.save(matchup);
                }

                // Rotate teams for next round
                // Keep first team fixed, rotate others clockwise
                // [A, B, C, D, E, F] -> [A, F, B, C, D, E]
                FantasyTeam last = rotation.remove(rotation.size() - 1);
                rotation.add(1, last);

                currentWeekIndex++;
            }
        }
    }
}
