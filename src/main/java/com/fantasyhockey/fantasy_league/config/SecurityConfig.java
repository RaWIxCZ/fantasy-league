package com.fantasyhockey.fantasy_league.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. Definice pravidel přístupu (Filter Chain)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        // Tyto stránky povolíme všem (i nepřihlášeným):
                        .requestMatchers("/register", "/save-user", "/css/**", "/js/**", "/images/**").permitAll()
                        // Všechno ostatní vyžaduje přihlášení:
                        .anyRequest().authenticated())
                .formLogin((form) -> form
                        .loginPage("/login") // Naše vlastní přihlašovací stránka (zatím ji nemáme)
                        .permitAll())
                .logout((logout) -> logout.permitAll());

        return http.build();
    }

    // 2. Nástroj na šifrování hesel (Hashování)
    // Nikdy neukládáme hesla jako prostý text! "12345" -> "$2a$10$..."
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}