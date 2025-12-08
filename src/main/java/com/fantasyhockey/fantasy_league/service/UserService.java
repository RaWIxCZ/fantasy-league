package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.dto.UserRegistrationDto;
import com.fantasyhockey.fantasy_league.model.User;
import com.fantasyhockey.fantasy_league.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for managing user accounts and registration.
 * Handles user creation with secure password encryption.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user in the system.
     * Creates a user account with encrypted password and default USER role.
     * 
     * @param request registration data containing username, email, and password
     */
    public void registerUser(UserRegistrationDto request) {
        // Create new user entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // Encrypt password using BCrypt
        // Never store passwords as plain text!
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encryptedPassword);

        // Set default role
        user.setRole("USER");

        // Save to database
        userRepository.save(user);
    }

    /**
     * Checks if a user with the given username already exists.
     * 
     * @param username the username to check
     * @return true if user exists, false otherwise
     */
    public boolean userExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
}