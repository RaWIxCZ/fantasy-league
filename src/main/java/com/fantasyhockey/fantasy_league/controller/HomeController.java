package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.dto.NhlScheduleResponse;
import com.fantasyhockey.fantasy_league.service.NhlApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Controller for the homepage/match center.
 * Displays NHL game schedule and currently playing games.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final NhlApiService nhlApiService;

    /**
     * Displays the homepage with NHL game schedule.
     * Supports AJAX requests for dynamic date changes.
     * 
     * @param dateStr       optional date parameter (ISO format: yyyy-MM-dd),
     *                      defaults to today
     * @param requestedWith header to detect AJAX requests
     * @param model         Spring MVC model
     * @return full homepage template or schedule fragment for AJAX
     */
    @GetMapping("/")
    public String home(
            @RequestParam(name = "date", required = false) String dateStr,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            Model model) {

        // Parse date or use today
        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

        // Fetch NHL schedule for the date
        NhlScheduleResponse schedule = nhlApiService.getSchedule(date);
        if (schedule == null) {
            schedule = new NhlScheduleResponse();
            schedule.setGameWeek(java.util.Collections.emptyList());
        }

        // Add data to model
        model.addAttribute("schedule", schedule);
        model.addAttribute("teamNames", nhlApiService.getTeamNames());
        model.addAttribute("currentDate", date);

        // Return fragment for AJAX requests, full page otherwise
        if ("XMLHttpRequest".equals(requestedWith)) {
            return "homepage :: scheduleFragment";
        }

        return "homepage";
    }
}