package com.fantasyhockey.fantasy_league.config;

import com.fantasyhockey.fantasy_league.repository.FantasyTeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Initializer that sets team logo URLs on application startup.
 * Maps team IDs to their corresponding logo image files.
 * Runs once when the application starts.
 */
@Component
public class TeamLogoInitializer implements CommandLineRunner {

    private final FantasyTeamRepository fantasyTeamRepository;

    public TeamLogoInitializer(FantasyTeamRepository fantasyTeamRepository) {
        this.fantasyTeamRepository = fantasyTeamRepository;
    }

    /**
     * Executes on application startup to set team logos.
     * Updates existing teams with their logo file paths.
     * 
     * @param args command line arguments (not used)
     */
    @Override
    public void run(String... args) throws Exception {
        // Map team IDs to logo filenames
        Map<Long, String> logoMapping = Map.of(
                1L, "keg.webp",
                2L, "puk.webp",
                3L, "wb.webp",
                4L, "gg.webp",
                5L, "dds.webp",
                6L, "gob.webp");

        // Update each team's logo URL
        logoMapping.forEach((id, filename) -> {
            fantasyTeamRepository.findById(id).ifPresent(team -> {
                team.setLogoUrl("/images/" + filename);
                fantasyTeamRepository.save(team);
            });
        });
    }
}
