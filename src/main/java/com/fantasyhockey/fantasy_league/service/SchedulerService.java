package com.fantasyhockey.fantasy_league.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    private final NhlApiService nhlApiService;

    // Spustí se každý den v 8:00 ráno
    // (0 sekund, 0 minut, 8 hodin, * každý den, * každý měsíc, * každý den v týdnu)
    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyUpdate() {
        logger.info("⏰ Spouštím denní aktualizaci bodů...");
        nhlApiService.updatePlayerInjuries();
        nhlApiService.updateStatsFromYesterday();
        logger.info("✅ Denní aktualizace hotova.");
    }
}