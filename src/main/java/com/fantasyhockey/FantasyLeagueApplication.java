package com.fantasyhockey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // Import

@SpringBootApplication
@EnableScheduling // <--- TOTO PŘIDEJ (Zapíná hodiny v aplikaci)
public class FantasyLeagueApplication {

	public static void main(String[] args) {
		SpringApplication.run(FantasyLeagueApplication.class, args);
	}

}