package com.fantasyhockey.fantasy_league.service;

import com.fantasyhockey.fantasy_league.dto.UserRegistrationDto;
import com.fantasyhockey.fantasy_league.model.User;
import com.fantasyhockey.fantasy_league.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Náš šifrovač z configu

    public void registerUser(UserRegistrationDto request) {
        // 1. Vytvoříme nového uživatele
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // 2. Heslo zašifrujeme
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encryptedPassword);

        user.setRole("USER");

        // 3. Uložíme do DB
        userRepository.save(user);
    }

    // Metoda pro kontrolu, zda uživatel už neexistuje (použijeme později)
    public boolean userExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
}