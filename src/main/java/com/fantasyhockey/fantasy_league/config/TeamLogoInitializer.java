package com.fantasyhockey.fantasy_league.config;

import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TeamLogoInitializer implements CommandLineRunner {

    private final FantasyTeamRepository fantasyTeamRepository;

    public TeamLogoInitializer(FantasyTeamRepository fantasyTeamRepository) {
        this.fantasyTeamRepository = fantasyTeamRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Map<Long, String> logoMapping = Map.of(
                1L, "keg.webp",
                2L, "puk.webp",
                3L, "wb.webp",
                4L, "gg.webp",
                5L, "dds.webp",
                6L, "gob.webp");

        logoMapping.forEach((id, filename) -> {
            fantasyTeamRepository.findById(id).ifPresent(team -> {
                team.setLogoUrl("/images/" + filename);
                fantasyTeamRepository.save(team);
            });
        });
    }
}
