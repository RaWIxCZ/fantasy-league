package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.model.Matchup;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.GameWeekRepository;
import com.fantasyhockey.fantasy_league.repository.MatchupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleService {

    private final GameWeekRepository gameWeekRepository;
    private final MatchupRepository matchupRepository;
    private final FantasyTeamRepository fantasyTeamRepository;

    public ScheduleService(GameWeekRepository gameWeekRepository, MatchupRepository matchupRepository,
            FantasyTeamRepository fantasyTeamRepository) {
        this.gameWeekRepository = gameWeekRepository;
        this.matchupRepository = matchupRepository;
        this.fantasyTeamRepository = fantasyTeamRepository;
    }

    @Transactional
    public void initializeSeason() {
        // Check if we need to reset the schedule (if Week 1 start date is incorrect)
        gameWeekRepository.findByWeekNumber(1).ifPresent(week1 -> {
            if (!week1.getStartDate().equals(LocalDate.of(2025, 10, 7))) {
                matchupRepository.deleteAll();
                gameWeekRepository.deleteAll();
            }
        });

        if (gameWeekRepository.count() > 0) {
            return; // Season already initialized correctly
        }

        // 1. Create Game Weeks
        // Week 1: Oct 7 (Tue) - Oct 12 (Sun)
        // Week 2+: Mon - Sun
        LocalDate startDate = LocalDate.of(2025, 10, 7);
        List<GameWeek> weeks = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 20; i++) {
            GameWeek week = new GameWeek();
            week.setWeekNumber(i);
            week.setStartDate(startDate);

            if (i == 1) {
                // First week ends on Sunday Oct 12
                week.setEndDate(LocalDate.of(2025, 10, 12));
            } else {
                // Subsequent weeks are 7 days (Mon-Sun)
                // startDate is already set to previous end + 1 (Monday)
                week.setEndDate(startDate.plusDays(6));
            }

            boolean isCompleted = week.getEndDate().isBefore(today);
            boolean isCurrent = !isCompleted && !week.getStartDate().isAfter(today);

            week.setCompleted(isCompleted);
            week.setCurrent(isCurrent);

            weeks.add(gameWeekRepository.save(week));

            // Set start date for next week (Monday)
            startDate = week.getEndDate().plusDays(1);
        }

        // Ensure at least one week is current if season hasn't started or ended?
        if (weeks.stream().noneMatch(GameWeek::isCurrent) && !weeks.isEmpty()) {
            if (today.isBefore(weeks.get(0).getStartDate())) {
                weeks.get(0).setCurrent(true);
                gameWeekRepository.save(weeks.get(0));
            }
        }

        // 2. Generate Schedule
        List<FantasyTeam> teams = fantasyTeamRepository.findAll();
        if (teams.size() < 2) {
            return; // Not enough teams
        }

        generateRoundRobinSchedule(teams, weeks);
    }

    private void generateRoundRobinSchedule(List<FantasyTeam> teams, List<GameWeek> weeks) {
        // Round Robin Algorithm
        // If odd number of teams, add a dummy team (bye) - handled by logic or assuming
        // even for now (User said 6 teams)

        List<FantasyTeam> rotation = new ArrayList<>(teams);
        if (rotation.size() % 2 != 0) {
            // Handle odd number if necessary, but assuming 6 for now
        }

        int numTeams = rotation.size();
        int numRounds = numTeams - 1; // One round-robin cycle
        int halfSize = numTeams / 2;

        // We want to repeat the cycle to fill all weeks
        int currentWeekIndex = 0;

        while (currentWeekIndex < weeks.size()) {
            for (int round = 0; round < numRounds; round++) {
                if (currentWeekIndex >= weeks.size())
                    break;

                GameWeek currentWeek = weeks.get(currentWeekIndex);

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

                // Rotate teams (keep first fixed, rotate others)
                // [0, 1, 2, 3, 4, 5] -> [0, 5, 1, 2, 3, 4]
                FantasyTeam last = rotation.remove(rotation.size() - 1);
                rotation.add(1, last);

                currentWeekIndex++;
            }
        }
    }

    public GameWeek getCurrentWeek() {
        return gameWeekRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("No current game week found"));
    }

    public GameWeek getWeekByNumber(int number) {
        return gameWeekRepository.findByWeekNumber(number)
                .orElseThrow(() -> new RuntimeException("Week " + number + " not found"));
    }
}
