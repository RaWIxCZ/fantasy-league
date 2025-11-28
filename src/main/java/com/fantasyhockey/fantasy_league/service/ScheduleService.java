package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.GameWeek;
import com.fantasyhockey.fantasy_league.model.Matchup;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.GameWeekRepository;
import com.fantasyhockey.fantasy_league.repository.MatchupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
        if (gameWeekRepository.count() > 0) {
            return; // Season already initialized
        }

        // 1. Create Game Weeks (e.g., 20 weeks starting from current Monday)
        LocalDate startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<GameWeek> weeks = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            GameWeek week = new GameWeek();
            week.setWeekNumber(i);
            week.setStartDate(startDate);
            week.setEndDate(startDate.plusDays(6)); // Sunday
            week.setCurrent(i == 1); // First week is current
            week.setCompleted(false);

            weeks.add(gameWeekRepository.save(week));
            startDate = startDate.plusWeeks(1);
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
}
