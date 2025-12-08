package com.fantasyhockey.fantasy_league.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service that runs scheduled tasks for daily data updates.
 * Automatically updates player injury statuses, game statistics,
 * and game week statuses every day at 8:00 AM.
 */
@Service
@RequiredArgsConstructor
public class DailyUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(DailyUpdateService.class);
    private final NhlApiService nhlApiService;
    private final ScheduleService scheduleService;

    /**
     * Runs daily at 8:00 AM to update player data and game week statuses.
     * Updates:
     * 1. Game week statuses (current/completed flags)
     * 2. Player injury statuses from NHL API
     * 3. Game statistics from yesterday's games
     * 
     * Cron expression: "0 0 8 * * *"
     * - Second: 0
     * - Minute: 0
     * - Hour: 8
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day of week)
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyUpdate() {
        logger.info("⏰ Starting daily update...");

        // Update game week statuses first
        scheduleService.updateGameWeekStatuses();

        // Then update player data
        nhlApiService.updatePlayerInjuries();
        nhlApiService.updateStatsFromYesterday();

        logger.info("✅ Daily update completed.");
    }
}