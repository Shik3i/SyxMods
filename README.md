# Syx Architect

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Game Version](https://img.shields.io/badge/Songs%20of%20Syx-v0.71.44%20(V71)-orange.svg)](https://songsofsyx.com/)
[![Zero Allocation](https://img.shields.io/badge/Performance-Zero--Allocation%20Loop-success.svg)](#performance--zero-allocation-guarantee)

> **The Ultimate Creative, Schönbau & Sandbox Suite for *Songs of Syx*.**  
> *Die umfassende Creative-, Schönbau- & Sandbox-Suite für Songs of Syx.*

---

## 🇬🇧 English Overview

**Syx Architect** transforms *Songs of Syx* into the ultimate model-building sandbox. Whether you want to design grand palatial districts, build sprawling Roman-style stone road networks, experiment with colossal city layouts, or enjoy the visual charm of bustling oddjobbers without resource grinds, Syx Architect gives you precision tools to sculpt your dream empire.

### Key Highlights
- **Semantic Schönbau Tiers (Decoupling Work from Materials):**
  - **Construction:** Keep your workers physically paving roads, building walls, and assembling furniture without deducting a single brick or log from your stockpiles (`Free Materials, Keep Work`), speed them up 10x (`Fast & Free`), or complete blueprints instantly (`Instant Build`).
  - **Maintenance:** Janitors walk the streets and repair aged buildings without consuming repair supplies (`Janitors Work, Free Mats`), or freeze building integrity permanently at 100% (`Zero Decay`).
  - **Knowledge:** Lock unlocked technologies forever (`No Forgetting - High-Watermark`), preventing tech regressions from laboratory shift changes, or gain instant omniscience (`Omniscience +1,000,000`).
  - **Logistics:** Keep your warehouses and raw material piles automatically supplied at ~80% capacity (`Raw Materials` or `All Assigned Crates`).
- **World, Nature & Population Manager:**
  - **Instant Immigrants:** Spawn +10, +50, or +100 citizens of any playable species directly on command.
  - **Cosmopolitan Wave:** Spawn +25 citizens of every playable species at once for immediate multi-cultural bustling streets.
  - **Anti-Aging & Longevity:** Multiplies citizen life expectancy by 1000x, eliminating old age mortality.
  - **Clear Building Weather & Daylight:** Freezes bright neutral daylight, banishing clouds, rain, snow, and night darkness for optimal architectural planning and pristine screenshots.
  - **100% Soil Moisture & Fertility:** Maximizes ground moisture map-wide, allowing lush farms, orchards, and pastures to thrive anywhere.
- **Security & Combat Control:**
  - **Raid Immunity:** Neutralizes incoming barbarian raids and hostile incursions instantly.
  - **Combat Auto-Win:** Breaks enemy army morale on active battlefields for guaranteed tactical victory.
- **1-Click Instant Actions:**
  - `⚡ Instant Build Now`: Completes all pending rooms, roads, walls, and structures settlement-wide.
  - `📦 Fill All Crates to 100%`: Instantly tops off every assigned warehouse crate.
  - `⚔️ Click Here to Win Combat`: Instant battlefield victory.
- **Seamless In-Game Bilingual UI:**
  - Direct **F10** hotkey toggle and top-right HUD button.
  - One-click live language toggle between **English** and **Deutsch**.
  - Built-in interactive tutorial and guide dialog.

---

## 🇩🇪 Deutsche Übersicht

**Syx Architect** macht *Songs of Syx* zum perfekten Modellbaukasten. Egal ob du prachtvolle Palastviertel entwerfen, kilometerlange Pflasterstraßen verlegen, monumentale Großstädte planen oder das Gewusel fleißiger Bauarbeiter ohne mühsamen Ressourcen-Grind genießen möchtest – Syx Architect liefert dir die volle Kontrolle.

### Hauptfunktionen
- **Semantische Schönbau-Stufen (Arbeit & Material entkoppelt):**
  - **Baustellen-Modus:** Arbeiter pflastern Straßen und errichten Mauern ganz normal (das lebendige Gewusel bleibt erhalten!), doch der Materialverbrauch ist 0 (`Gratis Material, Arbeit bleibt`), 10-faches Bautempo (`Schnell & Gratis`) oder augenblickliche Fertigstellung (`Sofortbau`).
  - **Gebäude-Wartung:** Hausmeister patrouillieren durch die Straßen und reparieren Gebäude ohne Materialkosten (`Hausmeister reparieren, Gratis Material`) oder dauerhaft 100% Gebäudezustand (`Kein Verschleiß`).
  - **Wissen & Forschung:** Einmal erforschte Technologien gehen niemals wieder verloren (`Nie wieder vergessen`), selbst wenn Labore unterbesetzt sind, oder sofortige Allwissenheit (`Allwissenheit +1.000.000 Punkte`).
  - **Lager-Logistik:** Hält Baurohstoffe oder alle zugewiesenen Lagerkisten automatisch auf ~80% Füllstand.
- **Welt-, Natur- & Bevölkerungs-Manager:**
  - **Sofort-Einwanderung:** Spawne per Klick +10, +50 oder +100 Bürger jeder gewünschten Rasse, um neue Stadtviertel sofort zu beleben.
  - **Kosmopolitische Welle:** Spawne +25 Bürger aller spielbaren Rassen zugleich.
  - **Alterungsschutz:** Verhindert das Sterben von Bürgern an Altersschwäche.
  - **Schönes Bauwetter & Tageslicht:** Friert helles Tageslicht ein und hält Wolken, Regen und Schnee fern – perfekt für Modellbau und Screenshots.
  - **100% Bodenfruchtbarkeit:** Maximiert die Bodenfeuchte kartenweit für grenzenlose Freiheit beim Platzieren von Farmen und Obstgärten.
- **Sicherheit & Schlachten:**
  - **Raid-Immunität:** Bricht feindliche Invasionen und Raids automatisch ab.
  - **Sofort-Sieg im Kampf:** Zerschlägt die feindliche Moral auf dem Schlachtfeld.
- **1-Klick Sofort-Aktionen:**
  - `⚡ Sofortbau jetzt ausführen`: Vollendet alle Baustellen, Straßen und Mauern sofort.
  - `📦 Alle Lagerkisten auf 100% füllen`: Maximale Lagerfüllung mit einem Klick.
  - `⚔️ Sofort-Sieg im Kampf`: Garantiert den Sieg in aktiven Schlachten.

---

## ⚡ Performance & Zero-Allocation Guarantee

Syx Architect was engineered from the ground up to respect Songs of Syx's high-performance 60 Hz simulation loop:
- **Zero Heap Allocations at 60 Hz:** `update(double ds)` performs **zero** object allocations during active gameplay. No GC pauses or frame stutters.
- **Pre-Allocated & Static Boosters:** Boosters attached to `game.boosting.BOOSTABLES` are allocated once and dynamically adjusted in-place via primitive double values.
- **Safe Reflection Caching:** Internal package-private engine handles (`JobBuild.construct`, `Bitsmap1D`, `JanitorInstance`) are resolved once during initialization and reused.
- **Savegame Safety:** Full serialization support via `FilePutter` and `FileGetter` with backward-compatible versioning (V1, V2, V3) and session-reload safety.

---

## 🛠️ Installation & Building from Source

### Requirements
- *Songs of Syx* v0.71.44+ (V71)
- Java 21 JDK
- Gradle Wrapper (included)

### Build & Deploy
To compile the mod JAR and deploy directly into your local Songs of Syx mod folder:
```bash
./gradlew.bat deployMod
```
The task automatically bundles `SyxArchitect.jar`, `_info.txt`, and the localized `assets/` into:
`%APPDATA%/songsofsyx/mods/SyxArchitect/V71/`

### Activation
1. Launch *Songs of Syx*.
2. In the Mod Manager launcher window, activate **Syx Architect**.
3. Load an existing savegame or create a new world.
4. Press **F10** or click the **Syx Architect [F10]** button in the top bar to open the suite.

---

## 📄 License
This project is licensed under the MIT License.
