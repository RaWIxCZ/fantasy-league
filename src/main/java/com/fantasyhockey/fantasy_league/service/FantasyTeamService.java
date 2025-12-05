package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.LineupSpot;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.model.User;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.LineupSpotRepository;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import com.fantasyhockey.fantasy_league.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FantasyTeamService {

    private final FantasyTeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final LineupSpotRepository lineupRepository;
    private final com.fantasyhockey.fantasy_league.repository.MatchupRepository matchupRepository;

    public static final int MAX_FORWARDS = 11;
    public static final int MAX_DEFENSEMEN = 7;
    public static final int MAX_GOALIES = 3;

    public Optional<FantasyTeam> getTeamByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return teamRepository.findByUser(user);
    }

    public void createTeam(String teamName, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        FantasyTeam team = new FantasyTeam();
        team.setTeamName(teamName);
        team.setUser(user);
        teamRepository.save(team);
    }

    @Transactional
    public void addPlayerToTeam(Long playerId, String username) {
        FantasyTeam team = getTeamByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nejdřív si musíš vytvořit tým!"));

        Player player = playerRepository.findById(Objects.requireNonNull(playerId))
                .orElseThrow(() -> new RuntimeException("Hráč neexistuje"));

        if (team.getPlayers().contains(player)) {
            throw new RuntimeException("Tento hráč už ve tvém týmu je.");
        }

        validateTeamRoster(team, player.getPosition());

        team.getPlayers().add(player);
        teamRepository.save(team);
    }

    private void validateTeamRoster(FantasyTeam team, String newPlayerPosition) {
        long forwardsCount = team.getPlayers().stream().filter(
                p -> "C".equals(p.getPosition()) || "LW".equals(p.getPosition()) || "RW".equals(p.getPosition()))
                .count();
        long defensemenCount = team.getPlayers().stream().filter(p -> "D".equals(p.getPosition())).count();
        long goaliesCount = team.getPlayers().stream().filter(p -> "G".equals(p.getPosition())).count();

        if ("C".equals(newPlayerPosition) || "LW".equals(newPlayerPosition) || "RW".equals(newPlayerPosition)) {
            if (forwardsCount >= MAX_FORWARDS) {
                throw new RuntimeException("Tvůj tým je plný útočníků! (Max " + MAX_FORWARDS + ")");
            }
        } else if ("D".equals(newPlayerPosition)) {
            if (defensemenCount >= MAX_DEFENSEMEN) {
                throw new RuntimeException("Tvůj tým je plný obránců! (Max " + MAX_DEFENSEMEN + ")");
            }
        } else if ("G".equals(newPlayerPosition)) {
            if (goaliesCount >= MAX_GOALIES) {
                throw new RuntimeException("Tvůj tým je plný brankářů! (Max " + MAX_GOALIES + ")");
            }
        }
    }

    @Transactional
    public void removePlayerFromTeam(Long playerId, String username) {
        FantasyTeam team = getTeamByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tým nenalezen"));

        boolean removed = team.getPlayers().removeIf(player -> player.getId().equals(playerId));

        if (!removed) {
            throw new RuntimeException("Hráč v týmu nebyl nalezen!");
        }

        teamRepository.save(team);
    }

    @Transactional
    public void removePlayerFromTeam(Long playerId, Long teamId) {
        FantasyTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Tým nenalezen"));

        boolean removed = team.getPlayers().removeIf(player -> player.getId().equals(playerId));

        if (!removed) {
            throw new RuntimeException("Hráč v týmu nebyl nalezen!");
        }

        teamRepository.save(team);
    }

    public List<FantasyTeam> getLeaderboard() {
        return teamRepository.findAllByOrderByLeaguePointsDesc();
    }

    private final RosterLockingService rosterLockingService;

    @Transactional
    public void saveLineupSpot(String username, Long playerId, String slotName) {
        FantasyTeam team = getTeamByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tým nenalezen"));

        Player player = playerRepository.findById(Objects.requireNonNull(playerId))
                .orElseThrow(() -> new RuntimeException("Hráč nenalezen"));

        // Check if the player entering the slot is locked
        validatePlayerNotLocked(player);

        LineupSpot spot = lineupRepository.findByTeamAndSlotName(team, slotName)
                .orElse(new LineupSpot());

        // Check if the player currently in this slot (being replaced) is locked
        if (spot.getPlayer() != null) {
            validatePlayerNotLocked(spot.getPlayer());
        }

        spot.setTeam(team);
        spot.setPlayer(player);
        spot.setSlotName(slotName);

        lineupRepository.save(spot);
    }

    @Transactional
    public void removePlayerFromSlot(String username, String slotName) {
        FantasyTeam team = getTeamByUsername(username).orElseThrow();

        Optional<LineupSpot> spotOpt = lineupRepository.findByTeamAndSlotName(team, slotName);
        if (spotOpt.isPresent()) {
            validatePlayerNotLocked(spotOpt.get().getPlayer());
        }

        lineupRepository.deleteByTeamAndSlotName(team, slotName);
    }

    public List<LineupSpot> getTeamLineup(FantasyTeam team) {
        return lineupRepository.findByTeam(team);
    }

    @Transactional
    public void movePlayer(String username, Long playerId, String newSlotName, String oldSlotName) {
        // Find the player being moved to ensure they aren't locked
        Player player = playerRepository.findById(playerId).orElseThrow();
        validatePlayerNotLocked(player);

        FantasyTeam team = getTeamByUsername(username).orElseThrow();

        // Ensure the spot we are leaving doesn't contain a DIFFERENT locked player
        // (should be the same player, but good to check)
        // Actually, logic is: We take player from OldSlot.
        // So validation of 'player' covers OldSlot occupant (unless OldSlot had someone
        // else?? No, movePlayer implies moving THAT player).

        lineupRepository.deleteByTeamAndSlotName(team, oldSlotName);
        saveLineupSpot(username, playerId, newSlotName);
    }

    private void validatePlayerNotLocked(Player player) {
        if (rosterLockingService.getLockedTeams().contains(player.getTeamName())) {
            throw new RuntimeException(
                    "Hráč " + player.getLastName() + " již hraje (nebo dohrál) a nelze s ním hýbat!");
        }
    }

    private final com.fantasyhockey.fantasy_league.repository.PlayerStatsRepository playerStatsRepository;

    @Transactional
    public void updateStandings() {
        List<FantasyTeam> teams = teamRepository.findAll();
        // Reset stats
        for (FantasyTeam team : teams) {
            team.setWins(0);
            team.setLosses(0);
            team.setOtWins(0);
            team.setOtLosses(0);
            team.setLeaguePoints(0);
        }

        List<com.fantasyhockey.fantasy_league.model.Matchup> allMatchups = matchupRepository.findAll();
        java.time.LocalDate today = java.time.LocalDate.now();

        for (com.fantasyhockey.fantasy_league.model.Matchup m : allMatchups) {
            boolean isCompleted = m.getGameWeek().isCompleted();
            boolean isPast = m.getGameWeek().getEndDate().isBefore(today);

            if (isCompleted || isPast) {
                double homeScore;
                double awayScore;

                if (isCompleted) {
                    // TRUST THE DB for completed weeks
                    homeScore = m.getHomeScore();
                    awayScore = m.getAwayScore();
                } else {
                    // If not completed but past, recalculate to be safe.
                    homeScore = calculateTeamScoreForPeriod(m.getHomeTeam(), m.getGameWeek().getStartDate(),
                            m.getGameWeek().getEndDate());
                    awayScore = calculateTeamScoreForPeriod(m.getAwayTeam(), m.getGameWeek().getStartDate(),
                            m.getGameWeek().getEndDate());

                    m.setHomeScore(homeScore);
                    m.setAwayScore(awayScore);
                    matchupRepository.save(m);
                }

                if (homeScore > awayScore) {
                    // Home Win
                    m.getHomeTeam().setWins(m.getHomeTeam().getWins() + 1);
                    m.getHomeTeam().setLeaguePoints(m.getHomeTeam().getLeaguePoints() + 3);

                    m.getAwayTeam().setLosses(m.getAwayTeam().getLosses() + 1);
                    // 0 points for loss
                } else if (awayScore > homeScore) {
                    // Away Win
                    m.getAwayTeam().setWins(m.getAwayTeam().getWins() + 1);
                    m.getAwayTeam().setLeaguePoints(m.getAwayTeam().getLeaguePoints() + 3);

                    m.getHomeTeam().setLosses(m.getHomeTeam().getLosses() + 1);
                    // 0 points for loss
                } else {
                    // TIE -> Overtime Logic
                    // 1. Best player points
                    double homeBest = getBestPlayerPoints(m.getHomeTeam(), m.getGameWeek().getStartDate(),
                            m.getGameWeek().getEndDate());
                    double awayBest = getBestPlayerPoints(m.getAwayTeam(), m.getGameWeek().getStartDate(),
                            m.getGameWeek().getEndDate());

                    if (awayBest > homeBest) {
                        // Away wins OT
                        m.getAwayTeam().setOtWins(m.getAwayTeam().getOtWins() + 1);
                        m.getAwayTeam().setLeaguePoints(m.getAwayTeam().getLeaguePoints() + 2);

                        m.getHomeTeam().setOtLosses(m.getHomeTeam().getOtLosses() + 1);
                        m.getHomeTeam().setLeaguePoints(m.getHomeTeam().getLeaguePoints() + 1);
                    } else {
                        // Home wins OT (Better best player OR Tie in best player -> Home advantage)
                        m.getHomeTeam().setOtWins(m.getHomeTeam().getOtWins() + 1);
                        m.getHomeTeam().setLeaguePoints(m.getHomeTeam().getLeaguePoints() + 2);

                        m.getAwayTeam().setOtLosses(m.getAwayTeam().getOtLosses() + 1);
                        m.getAwayTeam().setLeaguePoints(m.getAwayTeam().getLeaguePoints() + 1);
                    }
                }
            }
        }

        teamRepository.saveAll(teams);
    }

    private double calculateTeamScoreForPeriod(FantasyTeam team, java.time.LocalDate start, java.time.LocalDate end) {
        if (team == null || team.getPlayers().isEmpty())
            return 0.0;

        return team.getPlayers().stream()
                .mapToDouble(player -> {
                    return playerStatsRepository.findByPlayerIdAndDateBetween(player.getId(), start, end)
                            .stream()
                            .mapToDouble(stats -> stats.getFantasyPoints())
                            .sum();
                })
                .sum();
    }

    private double getBestPlayerPoints(FantasyTeam team, java.time.LocalDate start, java.time.LocalDate end) {
        if (team == null || team.getPlayers().isEmpty())
            return 0.0;

        return team.getPlayers().stream()
                .mapToDouble(player -> {
                    return playerStatsRepository.findByPlayerIdAndDateBetween(player.getId(), start, end)
                            .stream()
                            .mapToDouble(stats -> stats.getFantasyPoints())
                            .sum();
                })
                .max()
                .orElse(0.0);
    }
}