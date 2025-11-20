package com.fantasyhockey.fantasy_league.controller;

import com.fantasyhockey.fantasy_league.service.FantasyTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController // Důležité: Vrací data, ne HTML
@RequestMapping("/api/lineup")
@RequiredArgsConstructor
public class LineupApiController {

    private final FantasyTeamService teamService;

    // Endpoint pro uložení pozice (volá se při Drop)
    @PostMapping("/save")
    public ResponseEntity<?> saveSpot(@RequestBody SaveSpotRequest request, Principal principal) {
        try {
            teamService.saveLineupSpot(principal.getName(), request.getPlayerId(), request.getSlotName());
            return ResponseEntity.ok("Uloženo");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint pro smazání pozice (volá se při kliknutí na X)
    @PostMapping("/remove")
    public ResponseEntity<?> removeSpot(@RequestBody RemoveSpotRequest request, Principal principal) {
        teamService.removePlayerFromSlot(principal.getName(), request.getSlotName());
        return ResponseEntity.ok("Odstraněno");
    }

    // DTO třídy pro příjem dat (Static inner classes)
    @lombok.Data
    static class SaveSpotRequest {
        private Long playerId;
        private String slotName;
    }

    @lombok.Data
    static class RemoveSpotRequest {
        private String slotName;
    }
}