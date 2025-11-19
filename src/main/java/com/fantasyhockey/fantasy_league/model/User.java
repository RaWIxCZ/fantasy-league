package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users") // DŮLEŽITÉ: Množné číslo, aby se to nehádalo s SQL
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) // Login musí být unikátní a vyplněný
    private String username;

    @Column(nullable = false) // Email musí být vyplněný
    private String email;

    @Column(nullable = false)
    private String password; // Zde bude zahashované heslo (nečitelné), nikdy ne čistý text!

    // Role uživatele (např. "USER", "ADMIN")
    // Zatím dáme natvrdo defaultní hodnotu
    private String role = "USER";
}