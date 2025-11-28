package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.model.FantasyTeam;
import com.fantasyhockey.fantasy_league.model.Player;
import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import com.fantasyhockey.fantasy_league.repository.PlayerRepository;
import com.fantasyhockey.fantasy_league.service.FantasyTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final FantasyTeamRepository fantasyTeamRepository;
    private final FantasyTeamService fantasyTeamService;

    private static final Map<String, String> TEAM_NAMES = new HashMap<>();

    static {
        TEAM_NAMES.put("ANA", "Anaheim Ducks");
        TEAM_NAMES.put("BOS", "Boston Bruins");
        TEAM_NAMES.put("BUF", "Buffalo Sabres");
        TEAM_NAMES.put("CGY", "Calgary Flames");
        TEAM_NAMES.put("CAR", "Carolina Hurricanes");
        TEAM_NAMES.put("CHI", "Chicago Blackhawks");
        TEAM_NAMES.put("COL", "Colorado Avalanche");
        TEAM_NAMES.put("CBJ", "Columbus Blue Jackets");
        TEAM_NAMES.put("DAL", "Dallas Stars");
        TEAM_NAMES.put("DET", "Detroit Red Wings");
        TEAM_NAMES.put("EDM", "Edmonton Oilers");
        TEAM_NAMES.put("FLA", "Florida Panthers");
        TEAM_NAMES.put("LAK", "Los Angeles Kings");
        TEAM_NAMES.put("MIN", "Minnesota Wild");
        TEAM_NAMES.put("MTL", "Montreal Canadiens");
        TEAM_NAMES.put("NSH", "Nashville Predators");
        TEAM_NAMES.put("NJD", "New Jersey Devils");
        TEAM_NAMES.put("NYI", "New York Islanders");
        TEAM_NAMES.put("NYR", "New York Rangers");
        TEAM_NAMES.put("OTT", "Ottawa Senators");
        TEAM_NAMES.put("PHI", "Philadelphia Flyers");
        TEAM_NAMES.put("PIT", "Pittsburgh Penguins");
        TEAM_NAMES.put("SJS", "San Jose Sharks");
        TEAM_NAMES.put("SEA", "Seattle Kraken");
        TEAM_NAMES.put("STL", "St. Louis Blues");
        TEAM_NAMES.put("TBL", "Tampa Bay Lightning");
        TEAM_NAMES.put("TOR", "Toronto Maple Leafs");
        TEAM_NAMES.put("UTA", "Utah Hockey Club");
        TEAM_NAMES.put("VAN", "Vancouver Canucks");
        TEAM_NAMES.put("VGK", "Vegas Golden Knights");
        TEAM_NAMES.put("WSH", "Washington Capitals");
        TEAM_NAMES.put("WPG", "Winnipeg Jets");
    }

    @GetMapping("/players")
    public String listPlayers(
            @RequestParam(name = "team", required = false) String selectedTeam,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            Principal principal,
            Model model) {

        // 1. Načteme seznam týmů pro roletku
        List<String> allTeams = playerRepository.findDistinctTeamNames();
        model.addAttribute("teamList", allTeams);
        model.addAttribute("teamNames", TEAM_NAMES);

        // 2. Načteme draftované hráče
        Map<Long, String> draftedPlayers = new HashMap<>();
        List<FantasyTeam> fantasyTeams = fantasyTeamRepository.findAll();
        for (FantasyTeam ft : fantasyTeams) {
            for (Player p : ft.getPlayers()) {
                draftedPlayers.put(p.getId(), ft.getTeamName());
            }
        }
        model.addAttribute("draftedPlayers", draftedPlayers);

        // 3. Zkontrolujeme limity týmu
        boolean forwardsFull = false;
        boolean defenseFull = false;
        boolean goaliesFull = false;

        if (principal != null) {
            Optional<FantasyTeam> userTeamOpt = fantasyTeamService.getTeamByUsername(principal.getName());
            if (userTeamOpt.isPresent()) {
                FantasyTeam team = userTeamOpt.get();
                long forwardsCount = team.getPlayers().stream()
                        .filter(p -> "C".equals(p.getPosition()) || "LW".equals(p.getPosition())
                                || "RW".equals(p.getPosition()))
                        .count();
                long defenseCount = team.getPlayers().stream()
                        .filter(p -> "D".equals(p.getPosition()))
                        .count();
                long goaliesCount = team.getPlayers().stream()
                        .filter(p -> "G".equals(p.getPosition()))
                        .count();

                forwardsFull = forwardsCount >= FantasyTeamService.MAX_FORWARDS;
                defenseFull = defenseCount >= FantasyTeamService.MAX_DEFENSEMEN;
                goaliesFull = goaliesCount >= FantasyTeamService.MAX_GOALIES;
            }
        }
        model.addAttribute("forwardsFull", forwardsFull);
        model.addAttribute("defenseFull", defenseFull);
        model.addAttribute("goaliesFull", goaliesFull);

        // 4. Pokud uživatel vybral tým, načteme hráče a rozdělíme je
        if (selectedTeam != null && !selectedTeam.isEmpty()) {
            List<Player> players = playerRepository.findByTeamNameOrderByLastNameAsc(selectedTeam);

            List<Player> forwards = new ArrayList<>();
            List<Player> defensemen = new ArrayList<>();
            List<Player> goalies = new ArrayList<>();

            for (Player p : players) {
                if ("G".equals(p.getPosition())) {
                    goalies.add(p);
                } else if ("D".equals(p.getPosition())) {
                    defensemen.add(p);
                } else {
                    forwards.add(p);
                }
            }

            model.addAttribute("forwards", forwards);
            model.addAttribute("defensemen", defensemen);
            model.addAttribute("goalies", goalies);
            model.addAttribute("selectedTeam", selectedTeam);
        } else {
            model.addAttribute("forwards", List.of());
            model.addAttribute("defensemen", List.of());
            model.addAttribute("goalies", List.of());
        }

        // Pokud je to AJAX požadavek, vrátíme jen fragment
        if ("XMLHttpRequest".equals(requestedWith)) {
            return "players :: playerListContent";
        }

        return "players";
    }
}