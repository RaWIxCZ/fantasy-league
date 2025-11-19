package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.service.NhlApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // Říká, že tato třída odpovídá na HTTP požadavky
@RequiredArgsConstructor
public class ImportController {

    private final NhlApiService nhlApiService;

    @GetMapping("/import-players") // Když zadáš do prohlížeče localhost:8080/import-players
    public String triggerImport() {
        nhlApiService.importRoster();
        return "Import spuštěn! Podívej se do konzole v IntelliJ.";
    }
}