package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // Říká Hibernate: "Tohle je tabulka v databázi"
@Table(name = "players") // Jméno tabulky v SQL
@Data // Lombok: Automaticky vytvoří gettery, settery, toString, atd.
@NoArgsConstructor // Lombok: Prázdný konstruktor (nutný pro Hibernate)
@AllArgsConstructor // Lombok: Konstruktor se všemi parametry
public class Player {

    @Id // Primární klíč
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment (1, 2, 3...)
    private Long id;

    @Column(unique = true) // Každý hráč z NHL API má své unikátní ID, chceme ho zachovat
    private Long nhlId;

    private String firstName;
    private String lastName;

    private String teamName; // Např. "Boston Bruins"
    private String position; // Např. "C", "LW", "D", "G"

    // Zatím neřešíme statistiky, to přidáme později
}