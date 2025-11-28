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

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final NhlApiService nhlApiService;

    @GetMapping("/")
    public String home(
            @RequestParam(name = "date", required = false) String dateStr,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            Model model) {

        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

        NhlScheduleResponse schedule = nhlApiService.getSchedule(date);
        if (schedule == null) {
            schedule = new NhlScheduleResponse();
            schedule.setGameWeek(java.util.Collections.emptyList());
        }

        model.addAttribute("schedule", schedule);
        model.addAttribute("teamNames", nhlApiService.getTeamNames());
        model.addAttribute("currentDate", date);

        if ("XMLHttpRequest".equals(requestedWith)) {
            return "homepage :: scheduleFragment";
        }

        return "homepage";
    }
}