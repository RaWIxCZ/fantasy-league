package com.fantasyhockey.fantasy_league.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a user account in the fantasy hockey league system.
 * Each user can own one fantasy team and has authentication credentials.
 */
@Entity
@Table(name = "users") // Using plural form to avoid conflicts with SQL reserved keyword "user"
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username for login. Must be unique across all users.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * User's email address. Required for account creation.
     */
    @Column(nullable = false)
    private String email;

    /**
     * Encrypted password hash. Never stored as plain text.
     * Uses BCrypt hashing for security.
     */
    @Column(nullable = false)
    private String password;

    /**
     * User's role in the system (e.g., "USER", "ADMIN").
     * Defaults to "USER" for regular players.
     */
    private String role = "USER";
}