# Morni Wintersport

Der Wintersport-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): **Winterspiele im verschneiten Bergdorf
am Morni-Kogel – Riesenslalom, Biathlon und Bobbahn, um Gold, Silber und Bronze.** 2 bis 12 Leute treten im
**Wettkampf** aus allen drei Disziplinen an; jede Disziplin geht auch einzeln. Ein Paper-Plugin für Paper 26.x
(Java 25). Es läuft im Netzwerk und auch ganz allein.

Englischer Name: **Winter Games**. Netzwerk-Kennung: `wintersport`.

## So wird gespielt

1. Im **Bergdorf** (Dorfplatz mit großem Christbaum, Almhütten mit Schneedächern, Glühweinstand, Siegertreppchen)
   stimmt man mit dem **Kompass** ab – *Wettkampf (alle drei)* oder eine Disziplin allein – und sagt mit dem **grünen
   Farbstoff** „bereit“. Ab zwei Leuten (oder sobald jemand bereit ist) läuft ein Countdown; sind alle bereit, geht es
   nach 5 Sekunden los. Höchstens 12 Leute, Partys bleiben zusammen.
2. Bis dahin: **Eisfläche** zum Herumrutschen, **Sessellift** hinauf zur Bergstation (auf die rote Linie stellen, der
   nächste Sessel nimmt einen mit) und die **Rodelbahn** hinunter (oben aufs Eis treten – schon sitzt man im Schlitten,
   unten gibt's die Rodelzeit).
3. Jede Disziplin wird gewertet: schnellste Zeit vorn, die ersten drei bekommen **Gold, Silber, Bronze** (gleiche Zeit
   = gleicher Platz). Punkte für den Gesamtsieg: 10, 8, 6, 5, 4, 3, 2, danach je 1, ohne Ziel 0. Nach jeder Disziplin
   steht der **Medaillenspiegel** im Chat und in der Leiste rechts.
4. Zum Schluss die **Siegerehrung** am Treppchen im Dorf (mit Feuerwerk) – im Wettkampf nach Punkten (bei Gleichstand
   entscheiden Gold, Silber, Bronze), bei einer Disziplin nach deren Ergebnis. Allein ist es ein Training: keine
   Medaille, aber die Zeit zählt für die Bestenliste.

## Die Disziplinen (eigene Strecken, aus dem Code gebaut)

| Disziplin | So geht's |
|---|---|
| **Riesenslalom** | Im Boot die Eispiste (Packeis, Schneewälle) hinab – vier gleiche Pisten nebeneinander, bis zu vier fahren gleichzeitig, sonst in mehreren Läufen. Zwölf Tore: rote links, blaue rechts, jedes zwischen den Stangen durchfahren. Verpasst = **3 s Strafe**. Der Berg steigt in Terrassen an – auf den Absätzen springt das Boot. |
| **Biathlon** | Massenstart auf der Loipe (Schnelligkeit), drei Runden im Uhrzeigersinn an den Fahnen entlang. Nach Runde 1 und 2 an die eigene Bahn am **Schießstand**: fünf Pfeile auf fünf schwarze Scheiben (die Pfeile fliegen hier schnurgerade), Treffer klappen weiß um. Jeder Fehler = eine **Strafrunde** in der roten Schleife. Wer zuerst nach Runde 3 über die Linie kommt, gewinnt. |
| **Bobbahn** | Im Bob durch einen Eiskanal aus Blaueis mit zwölf Kehren durch den Bergwald – vier Kanäle nebeneinander. **Partys fahren zu zweit im Zweierbob** (der Erste lenkt), wer übrig ist, fährt allein. |

Steuerung im Boot: `W` = Gas, `A`/`D` = lenken (geht auch mit Bedrock). Wer aussteigt oder feststeckt, kommt mit der
**Feder** (oder einfach durch Aussteigen) zum letzten Tor bzw. Kontrollpunkt zurück – die Zeit läuft weiter. Der
**rote Farbstoff** gibt die Disziplin auf (im Wettkampf bleibt man dabei). Menüs sind Truhen und gehen auch mit Bedrock.

## Die Welt

Eine eigene leere Welt (`wintersport`, Schnee-Biom) mit allem aus dem Code: der Skiberg in zwölf Terrassen mit
Gipfelplateau und Starthäusern, die Pisten und Bobkanäle mit Zielbögen, Auslauf, Fangzaun und Zuschauertribünen, der
Sessellift mit Tal- und Bergstation, Stützen und laufenden Sesseln (Anzeige-Objekte), die Rodelbahn, das
Biathlon-Stadion mit Schießstand (zwölf überdachte Matten), Strafrunde und Tribüne, ringsum verschneite Fichten und
hohe Berge. Immer Tag (oder per Einstellung Abend mit Laternen), leise rieselnder Schnee. Tafeln im Dorf: ewiger
Medaillenspiegel und Bestzeiten; an den Zielen Anzeigetafeln mit dem laufenden Lauf.

## Saison

`saison: immer` (Standard) oder `saison: winter` – dann ist nur vom **1. Dezember bis 31. März** offen. Außerhalb
meldet der Modus dem Netzwerk den Zustand `closed`, das Dorf bleibt zum Anschauen offen (Eisfläche, Lift, Rodelbahn)
und erklärt freundlich, wann es wieder losgeht.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/abstimmen [wettkampf\|slalom\|biathlon\|bob]` | abstimmen (ohne Angabe: Menü) – auch `/vote` |
| `/bereit` | im Dorf bereit melden – auch `/ready` |
| `/zuschauen` | dem laufenden Wettkampf von der Tribüne zuschauen – auch `/spectate` |
| `/verlassen` | Wettkampf, Zuschauen, Lift oder Schlitten verlassen – auch `/leave` |
| `/stats [spieler]` · `/top [disziplin]` | Medaillen und Bestzeiten |
| `/wsadmin info\|status\|start [programm]\|stopp\|neubauen\|neuladen\|testdatum <datum\|aus>` | Verwaltung (`moinmorni.wintersport.admin`); `fahren`/`laufen` nur für Tests |

Einstellungen stehen nur in der `config.yml` (Saison, Tageszeit, Schneefall, Countdown, Zeitlimits, Strafe, Zweierbob,
Biathlon-Runden und -Schießen, Tempo) – im Netzwerk später im Panel.

**Zurück ins Spiel:** Verliert jemand mitten im Wettkampf die Verbindung (zum Beispiel, weil das Spiel abstürzt),
bleibt sein Platz `rueckkehr-sekunden` lang frei (ganz oben in der `config.yml`, Vorgabe 90, 0 = aus). Sein Lauf wartet,
die Uhr läuft weiter (das Zeitlimit gilt). Kommt er rechtzeitig wieder – direkt oder über „Zurück ins Spiel“ in der
Lobby –, geht es dort weiter: gleiche Disziplin, gleicher Lauf, seine Zeiten zählen (im Boot sitzt er gleich wieder
drin, im Biathlon steht er dort, wo er war, mit Runde, Treffern und Strafrunden). Zu spät: Er hat den Wettkampf
verlassen wie bisher.

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Wintersport nutzt sie,
wenn sie da ist (`softdepend`): Sprache, Party (zusammen in den Wettkampf, im Zweierbob), Statistik, Zustand,
zurück zur Lobby. Ohne Netzwerk: deutsche Texte, Statistik nur lokal, „zur Lobby“ trennt die Verbindung.
Ab Schnittstelle 2 meldet Wintersport, wer gerade im Wettkampf ist (dann fragt „/lobby“ erst nach), das Bett im Dorf
fragt über das Netzwerk, und ein freigehaltener Platz erscheint in der Lobby als „Zurück ins Spiel“; mit einem
älteren Netzwerk-Plugin läuft alles wie bisher.

Statistik (`stats("wintersport", …)`):

- hochgezählt: `events` (Wettkämpfe), `races` (gewertete Rennen), `gold`, `silver`, `bronze`, `points`, `wins`
  (Gesamtsiege im Wettkampf), `biathlon_hits`, `biathlon_shots`
- Bestzeiten (kleiner ist besser): `time_slalom_ms`, `time_biathlon_ms`, `time_bob_ms`
- Bestwert (größer ist besser): `best_points` (Punkte in einem Wettkampf)

Zustand: `waiting`, `running` oder `closed` (außerhalb der Saison), dazu die Spielerzahl. Lokal:
`spieler/<uuid>.yml` und `bestenliste.yml` (Bestzeiten je Disziplin, ewiger Medaillenspiegel).

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Wintersport.jar` (Version aus `pom.xml`).

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Wintersport.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
```

`test/run.sh` baut das Plugin und ein Test-Netzwerk (`test/fake-network`: Bert spricht Englisch, Anna und Carl sind
eine Party, alle Meldungen landen in `calls.log`), richtet einen Paper-26.1.2-Server ein (offline, Port 25960,
RCON 26060, kurze Countdowns) und schickt Test-Spieler durch Dorf, Abstimmung, Rodelbahn, Sessellift, Saison-Schalter,
einen ganzen Wettkampf (Slalom mit verpasstem Tor und Aussteigen, Biathlon mit echten Bogenschüssen und Strafrunden,
Zweierbob), Siegerehrung, Statistik, einen zweiten Wettkampf mit zwei Läufen und Aufgeben, Zuschauen und den
Lobby-Knopf (`test/wintersport-test.cjs`). Boote fahren in mineflayer nicht selbst – dafür schiebt `/wsadmin fahren`
das Boot Stück für Stück über die Strecke. `FRESH=1` baut die Welt neu, `KEEP=1 SKIP_TEST=1` startet nur den Server.

---

Verfasst mit Claude 🤖
