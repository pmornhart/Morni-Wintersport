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

- Version 0.1.1 (Branch `claude/zurueck-ins-spiel`, wartet auf Philips Go): **Zurück ins Spiel** (Philip: „mitten im
  Spiel nochmal fragen, ob man wirklich zur Lobby will; nach einem Absturz zurück ins Spiel“). Netzwerk-Schnittstelle 2
  (byte-gleiche Kopie; `askToLobby`, `playing`, `holdSlot`; älteres Netzwerk-Plugin → `LinkageError` einmal im Log,
  sonst wie bisher). `playing` vom Start bis zur Siegerehrung bzw. Abbruch, das Bett im Dorf ruft `askToLobby`.
  Verbindung mitten im Wettkampf weg → Platz `rueckkehr-sekunden` frei (neu oben in der config.yml, Vorgabe 90, 0 =
  aus; Regeln in `logic/Comeback`), `holdSlot`; der Lauf wartet, die Uhr läuft weiter (Zeitlimit gilt), wer weg ist,
  bekommt in späteren Disziplinen seinen Platz in den Läufen. Rechtzeitig zurück (`Stage.rejoin`): Slalom/Bob gleich
  wieder im Boot (allein am letzten Tor bzw. Kontrollpunkt, zu zweit beim Partner), Biathlon an derselben Stelle mit
  Runde, Treffern und Strafrunden (am Schießstand zurück auf die Matte), sonst auf die Tribüne; zu spät: wie bisher
  verlassen. Test-Netzwerk schreibt `playing`, `ask-lobby`, `hold-slot`; `test/run.sh` mit `MEMORY` (Xms 256M).
  JUnit 6.1.3. Geprüft: 38 Unit-Tests (neu `ComebackTest`), Paper 26.1 übersetzt, echt (`test/run.sh`,
  `rueckkehr-sekunden: 20`): vor dem Neustart des Rechners **116/116**; mit den neuen Slalom-Prüfungen auf dem stark
  ausgelasteten Rechner (Last ~10–40 auf 4 Kernen, zwei Läufe vom Speicherwächter beendet) **117/119** – alle neuen
  Prüfungen grün (alle spielen; Slalom: Dora geht mitten im Lauf, Lauf wartet, zurück → wieder im Boot, gleiches Tor,
  holdSlot 20 → 0; Biathlon: Dora geht in der Strafrunde, Stand bleibt, zurück an derselben Stelle; Bob: Bert kommt zu
  spät → verlässt den Wettkampf, holdSlot 0, ins Dorf; bei der Siegerehrung spielt niemand; Bett → `askToLobby`), die
  zwei roten sind die echten Bogenschüsse im Biathlon (Schießrunde der Test-Spieler wurde unter der Last nicht
  rechtzeitig fertig), Log ohne Fehler. Gesehen: Wird der Server per SIGTERM beendet, wirft `onDisable` „Cannot remove
  entity off-main“ (Paper ruft das Abschalten dann aus dem Shutdown-Thread) – unverändert, nicht neu.

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

- Philip: Go für `main` mit 0.1.1 (Zurück ins Spiel).
- Netzwerk-Repo: Einstellungs-Katalog um `rueckkehr-sekunden` ergänzen (0–600, Vorgabe 90).
- Nicht echt geprüft: Rückkehr über das echte Netzwerk (Lobby-Angebot „Zurück ins Spiel“), Rückkehr im laufenden
  Zweierbob mit Partner (nur Code), Bedrock.
- Netzwerk-Repo: Lobby-Figur + `MODE_REPOS` (`wintersport: 'pmornhart/morni-wintersport'`), Zustand `closed` anzeigen,
  Webseite (Modus-Seite, Statistik-Schlüssel), Einstellungs-Katalog für die config.yml.
