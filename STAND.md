# Stand – Morni Wintersport

**Was es ist:** Der Wintersport-Modus des MoinMorni-Netzwerks (Paper-Plugin `MorniWintersport`, Java 25, Paper 26.x):
2–12 Leute treten in Riesenslalom, Biathlon und Bobbahn an – um Medaillen und den Gesamtsieg. Jede Disziplin geht
auch einzeln. Netzwerk-Kennung `wintersport`, englisch „Winter Games“.

## Aufbau

- `src/main/java/de/moinmorni/wintersport/`: `WintersportPlugin` (Start, Welt, Takt, Saison, Zustand), `Settings`,
  `Texts` (de/en), `Ui`, `Items`, `Holo` (Schrift je Sprache), `Sidebar` (kopiert die Rang-Teams des Netzwerks),
  `Boards` (Schilder, ewiger Medaillenspiegel, Bestzeiten, Anzeigetafeln an den Zielen, Leiste im Dorf), `Snow`,
  `Commands` (`/abstimmen`, `/bereit`, `/zuschauen`, `/verlassen`, `/stats`, `/top`, `/wsadmin`).
- `logic/` (ohne Server, mit Tests): `Discipline`, `Program`, `Line`/`Gate` (Linien kreuzen), `Ranking` (Plätze,
  Medaillen, Punkte), `MedalTable`, `Heats`, `Teams` (Zweierbob), `Groups`, `Votes`, `Season`, `BiathlonProgress`,
  `RaceClock`, `TimeFormat`.
- `course/` (ohne Server, mit Tests): `Terrain` (Berg in zwölf Terrassen), `Slalom`, `Bob`, `Path` (Mittellinie mit
  runden Kurven), `Biathlon`, `Lift`, `Places`.
- `world/`: `Plan`, `Decor`, `Zones`, `Ground`, `Forest`, `Pistes` (Slalom, Rodelbahn, Tribünen), `BobTrack`,
  `LiftPlan`, `VillagePlan`, `Stadium`, `Props` (Torstangen, Liftseile), `Construction` (`VERSION` 2), `VoidGenerator`.
- `game/`: `Games`, `Competition` (Ablauf, Wertung, Statistik, Siegerehrung), `Racer`, `Stage`, `Result`, `BoatRace`
  (Slalom und Bob mit Läufen, Starttor, Zeitnahme, Zurücksetzen), `BiathlonRace` (Massenstart, Schießstand, Strafrunden).
- `village/`: `Village` (Warten, Abstimmen, Countdown), `VillageMenus`, `Chairlift`, `Fun` (Rodelbahn).
- `listen/`: `PlayerListener`, `RaceListener`, `GuardListener`. `net/`: `Network`, `Connected`, `Standalone`. `menu/`.
- `test/`: `run.sh`, `wintersport-test.cjs`, `rcon.cjs`, `fake-network/`.

## Stand (16.11.2026)

- Version 0.1.0 (erster Stand, Branch `claude/erster-stand`, noch nicht veröffentlicht – wartet auf Philips Go).
- 34 Unit-Tests grün (Wertung, Medaillenspiegel, Läufe, Zweierbob, Saison, Biathlon-Ablauf, Zeiten, Linien/Tore,
  Gelände, Slalom-Tore auf flachem Eis, Bob nie bergauf, Loipe, Daten, Texte). Übersetzt gegen Paper 26.3 und 26.1.
- Echter Test (`test/run.sh`, Paper 26.1.2 + Test-Netzwerk, fünf Test-Spieler): **102/102**, Server-Log ohne Fehler –
  Dorf, Abstimmung, Rodelbahn, Sessellift, Saison-Schalter (closed/Datum/waiting), Wettkampf: Slalom (4 Bahnen,
  Starttor, verpasstes Tor = Strafe, Aussteigen per Schleichen = zurück zum Tor), Biathlon (eingefroren, eine Runde
  wirklich gelaufen, echte Bogenschüsse, Treffer klappen weiß um, Strafrunden zuerst), Zweierbob (Party im selben Boot,
  Kontrollpunkte, Zurücksetzen), Medaillen, Medaillenspiegel, Siegerehrung auf dem Treppchen, Netzwerk-Statistik,
  Englisch, zweiter Wettkampf mit zwei Läufen und Aufgeben, Zuschauen, Abbruch, Lobby-Knopf.
- Bilder mit echtem Client 26.3 (nur lokal, `/home/user/mm-cache/bilder-wintersport/`): Dorf, Christbaum, Berg,
  Slalompisten mit Toren, Starttor, Slalom-Fahrt aus dem Boot, Bobkanäle, Stadion, Schießstand, Sessellift.
- Nicht echt geprüft: echtes Bootfahren mit einem richtigen Client (Steuerung, Sprünge über die Absätze, Tempo),
  Mitfahren im Sessellift mit echtem Client, Bedrock, Netzwerk mit echtem Hub, Paper 26.3 als Server.

## Offen

- Philip: Bilder ansehen, Go für `main` (dann Release v0.1.0).
- Netzwerk-Repo: Lobby-Figur + `MODE_REPOS` (`wintersport: 'pmornhart/morni-wintersport'`), Zustand `closed` anzeigen,
  Webseite (Modus-Seite, Statistik-Schlüssel), Einstellungs-Katalog für die config.yml.
