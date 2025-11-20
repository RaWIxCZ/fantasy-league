package com.fantasyhockey.fantasy_league.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NhlPlayerDto {
    private Long id; // NHL ID

    // V JSONu je to "firstName": { "default": "David" }, musíme to vytáhnout
    // Pro zjednodušení použijeme jen objekt, mapování vyřešíme ve službě,
    // nebo pokud je JSON jednoduchý:

    @JsonProperty("firstName")
    private NameDto firstNameObj;

    // NHL API vrací přímo odkaz na fotku v poli "headshot"
    private String headshot;

    @JsonProperty("lastName")
    private NameDto lastNameObj;

    private String positionCode; // "C", "R", "L"

    // Pomocná třída pro jméno (protože NHL to má jako objekt s jazyky)
    @Data
    public static class NameDto {
        @JsonProperty("default")
        private String defaultName;
    }
}