package com.fantasyhockey.fantasy_league.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class EspnScraperService {

    private static final Logger logger = LoggerFactory.getLogger(EspnScraperService.class);
    private static final String ESPN_INJURY_URL = "https://www.espn.com/nhl/injuries";

    public Map<String, String> getInjuredPlayers() {
        Map<String, String> injuredPlayers = new HashMap<>();
        try {
            Document doc = Jsoup.connect(ESPN_INJURY_URL).get();
            Elements injuryTables = doc.select(".Table__TBODY");
            for (Element table : injuryTables) {
                Elements rows = table.select("tr");
                for (Element row : rows) {
                    Elements cells = row.select("td");
                    if (cells.size() > 1) {
                        String playerName = cells.get(0).text();
                        String status = cells.get(1).text();
                        // We only care about players who are actually injured, not day-to-day
                        if (!"Day-To-Day".equalsIgnoreCase(status)) {
                            injuredPlayers.put(playerName, status);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while scraping ESPN for injuries: {}", e.getMessage());
        }
        return injuredPlayers;
    }
}