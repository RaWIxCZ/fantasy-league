package com.fantasyhockey.fantasy_league.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final NhlApiService nhlApiService;

    // Spustí se každý den v 8:00 ráno
    // (0 sekund, 0 minut, 8 hodin, * každý den, * každý měsíc, * každý den v týdnu)
    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyUpdate() {
        System.out.println("⏰ Spouštím denní aktualizaci bodů...");
        nhlApiService.updateStatsFromYesterday();
        System.out.println("✅ Denní aktualizace hotova.");
    }
}