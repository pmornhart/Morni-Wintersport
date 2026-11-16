package de.moinmorni.wintersport.game;

import de.moinmorni.wintersport.logic.Discipline;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Eine laufende Disziplin im Wettkampf (Riesenslalom, Biathlon oder Bob). */
public interface Stage {

    Discipline discipline();

    void start();

    /** Jeden Tick. */
    void tick(long tick);

    /** Jede Sekunde. */
    void second();

    boolean done();

    /** Ergebnis je Starter (Einzel oder Zweierbob). */
    List<Result> results();

    /** Jemand hat den Server oder den Wettkampf verlassen. */
    void quit(Racer r);

    /** Jemand gibt diese Disziplin auf (bleibt im Wettkampf). */
    void giveUp(Racer r);

    /** Aufräumen (Boote weg, Starttore zu, Scheiben schwarz). */
    void cleanup();

    /** Wo Zuschauer stehen. */
    Location view();

    /** Zeile für die Leiste („Lauf 2 von 3“, „Runde 1/3“ …). */
    String status(Racer r, String lang);

    /** Zurück auf die Strecke (Gegenstand). */
    void reset(Player p);

    /** Für /wsadmin status: eine Zeile je Starter. */
    List<String> debug();
}
