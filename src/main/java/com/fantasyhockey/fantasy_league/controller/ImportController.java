package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.service.NhlApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController // Říká, že tato třída odpovídá na HTTP požadavky
@RequiredArgsConstructor
public class ImportController {

    private final NhlApiService nhlApiService;

}