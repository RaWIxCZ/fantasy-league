# 🏒 NHL Fantasy League Manager

Komplexní webová aplikace pro správu Fantasy Hokejové Ligy, postavená na **Java Spring Boot** ekosystému.

Aplikace umožňuje uživatelům sestavit si vlastní tým z reálných hráčů NHL, automaticky stahuje výsledky zápasů z oficiálního **NHL API**, zpracovává statistiky a v reálném čase přepočítává body fantasy týmům.

> **Status:** MVP Hotovo (Verze 1.0)

---

## 📸 Ukázky aplikace

**Login page**
<img width="1870" height="1013" alt="LoginPage" src="https://github.com/user-attachments/assets/dcea0a56-80b5-4343-b726-c3ffdc25fda7" />

**My team page**
<img width="1868" height="1019" alt="2" src="https://github.com/user-attachments/assets/4ed7a2fa-6f7a-425b-acce-92a9fd4bcc6e" />

**Player list and draft possibility**
<img width="1854" height="1017" alt="3" src="https://github.com/user-attachments/assets/0efeb3ff-548c-4ca9-8327-5e5666b2af00" />

---

## 🛠 Použité Technologie

Projekt je postaven na moderním Enterprise stacku s důrazem na čistou architekturu a oddělení vrstev (MVC).

### Backend
* **Java 21** (Core logic)
* **Spring Boot v3.5.7** (Framework)
    * **Spring Data JPA** (Hibernate - ORM pro komunikaci s DB)
    * **Spring Security** (Autentizace a autorizace, BCrypt hashing)
    * **Spring Web** (REST API & MVC)
    * **Spring Scheduling** (Automatizace úloh - CRON jobs)
* **Lombok** (Redukce boilerplate kódu)

### Databáze
* **PostgreSQL** (Relační databáze)
* **Transakční řízení** (`@Transactional` pro konzistenci dat při draftování a výpočtech)

### Frontend
* **Thymeleaf** (Server-side rendering šablon)
* **Bootstrap 5** (Responsive UI & Styling)

### Integrace
* **NHL Official API** (Stahování soupisek, schedule a live výsledků zápasů)
* **JSON Processing** (Jackson - mapování JSONu na Java DTO objekty)

---

## ✨ Klíčové Funkce

1.  **Uživatelská správa (Auth)**
    * Registrace a bezpečné přihlášení (Hashování hesel).
    * Ochrana stránek pomocí Spring Security (nepřihlášený uživatel nevidí data).

2.  **Draftovací systém**
    * Prohlížení reálných hráčů NHL (filtrování, pozice).
    * Logika draftu: Přidání hráče do týmu, validace kapacity týmu, kontrola duplicit.
    * Možnost propustit hráče (Drop player).

3.  **Automatický Engine (Scheduler)**
    * Aplikace běží autonomně.
    * Každé ráno (CRON) se aplikace dotáže NHL API na včerejší zápasy.
    * Stáhne "Boxscore" data, naparuje je na hráče v databázi a vypočítá Fantasy body (Gól = 5b, Asistence = 3b).
    * Automatická aktualizace celkového skóre týmu.

---

## 🚀 Jak spustit projekt

### Prerekvizity
* JDK 17 nebo novější
* PostgreSQL
* Maven

### Instalace

1.  **Klonování repozitáře**
    ```bash
    git clone [https://github.com/TvojeJmeno/fantasy-league.git](https://github.com/TvojeJmeno/fantasy-league.git)
    cd fantasy-league
    ```

2.  **Nastavení Databáze**
    * Vytvořte lokální PostgreSQL databázi s názvem `fantasy_hockey`.
    * Upravte soubor `src/main/resources/application.properties`:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/fantasy_hockey
    spring.datasource.username=vase_jmeno
    spring.datasource.password=vase_heslo
    ```

3.  **Spuštění**
    ```bash
    mvn spring-boot:run
    ```
    * Aplikace se spustí na `http://localhost:8080`.
    * Při prvním spuštění navštivte `/import-players` (nebo použijte admin endpoint) pro naplnění databáze hráči.

---

## 🧠 Architektura a Design Patterns

V projektu jsem aplikoval následující principy:
* **Controller-Service-Repository:** Striktní oddělení vrstev.
* **DTO Pattern:** Oddělení interních databázových entit od dat z externího API.
* **Dependency Injection:** Využití Spring IoC kontejneru.
* **Scheduler:** Asynchronní zpracování dat na pozadí.

---

## 🔜 Plánované rozšíření (Roadmap)

* [ ] Leaderboard (Žebříček) uživatelů.
* [ ] Grafy vývoje bodů v čase.
* [ ] Rozšíření statistik (Trestné minuty, Zásahy brankářů).
* [ ] REST API endpointy pro mobilní aplikaci.

---

**Autor:** Rostislav Janko
