package com.fantasyhockey.fantasy_league.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the fantasy hockey league application.
 * Defines authentication, authorization, and session management rules.
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${app.security.remember-me.key}")
    private String rememberMeKey;

    /**
     * Configures the security filter chain with access rules and authentication.
     * 
     * Public endpoints (no authentication required):
     * - /register, /save-user: User registration
     * - /css/**, /js/**, /images/**: Static resources
     * 
     * Admin-only endpoints:
     * - /admin/**: Requires ADMIN authority
     * 
     * All other endpoints require authentication.
     * 
     * @param http the HttpSecurity to configure
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Authorization rules
                .authorizeHttpRequests((requests) -> requests
                        // Public access (no login required)
                        .requestMatchers("/register", "/save-user", "/css/**", "/js/**", "/images/**").permitAll()
                        // Admin-only access
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        // All other pages require authentication
                        .anyRequest().authenticated())

                // Login configuration
                .formLogin((form) -> form
                        .loginPage("/login")
                        // Custom success handler for AJAX login support
                        .successHandler((request, response, authentication) -> {
                            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                                response.setStatus(200);
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                        // Custom failure handler for AJAX login support
                        .failureHandler((request, response, exception) -> {
                            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                                response.setStatus(401);
                            } else {
                                response.sendRedirect("/login?error");
                            }
                        })
                        .permitAll())

                // Remember me configuration
                .rememberMe((remember) -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(86400)) // 1 day

                // Logout configuration
                .logout((logout) -> logout.permitAll());

        return http.build();
    }

    /**
     * Password encoder bean for secure password hashing.
     * Uses BCrypt algorithm - never stores passwords as plain text!
     * 
     * Example: "password123" -> "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}