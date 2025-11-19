package com.fantasyhockey.fantasy_league.repository;

import com.fantasyhockey.fantasy_league.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA je chytrý. Stačí napsat název metody a on vygeneruje SQL:
    // SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // Pro kontrolu při registraci (zda už email neexistuje)
    Optional<User> findByEmail(String email);
}