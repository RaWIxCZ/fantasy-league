package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity // Říká Hibernate: "Tohle je tabulka v databázi"
@Table(name = "players") // Jméno tabulky v SQL
@Data // Lombok: Automaticky vytvoří gettery, settery, toString, atd.
@NoArgsConstructor // Lombok: Prázdný konstruktor (nutný pro Hibernate)
@AllArgsConstructor // Lombok: Konstruktor se všemi parametry
public class Player {

    // Nový sloupec, kam si URL uložíme napořád
    private String headshotUrl;

    // --- Vypočítané statistiky (neukládají se do DB, počítají se "za letu") ---

    @Transient // Říkáme DB: "Tohle není sloupec, to si spočítáme"
    public int getSeasonGoals() {
        if (matchHistory == null) return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getGoals).sum();
    }

    @Transient
    public int getSeasonAssists() {
        if (matchHistory == null) return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getAssists).sum();
    }

    @Transient
    public int getSeasonPoints() {
        // V hokeji jsou Body (Productivity) = Góly + Asistence
        return getSeasonGoals() + getSeasonAssists();
    }

    @Transient
    public int getSeasonFantasyPoints() {
        if (matchHistory == null) return 0;
        return matchHistory.stream().mapToInt(PlayerStats::getFantasyPoints).sum();
    }

    // Vazba na tabulku statistik (aby hráč věděl o svých gólech)
    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    private List<PlayerStats> matchHistory;

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