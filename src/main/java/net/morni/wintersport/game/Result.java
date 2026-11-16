package net.morni.wintersport.game;

import java.util.List;

/**
 * Ergebnis eines Starters in einer Disziplin: Zeit samt Strafen, im Ziel? Dazu was die Ergebnisliste zeigt
 * (verpasste Tore bzw. Treffer).
 */
public record Result(List<Racer> members, long timeMs, boolean finished, int missedGates, int hits, int shots) {

    public String id() {
        return members.stream().map(Racer::id).reduce((a, b) -> a + "+" + b).orElse("");
    }

    public String names() {
        return members.stream().map(Racer::name).reduce((a, b) -> a + " & " + b).orElse("");
    }
}
