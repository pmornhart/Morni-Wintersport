package de.moinmorni.wintersport.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wertung einer Disziplin: schnellste Zeit vorn, gleiche Zeit = gleicher Platz (und gleiche Medaille), wer nicht ins
 * Ziel kam, steht hinten ohne Platz. Medaillen gibt es nur, wenn mindestens {@link #MEDALS_FROM} Starter dabei waren
 * (allein ist es ein Training).
 */
public final class Ranking {

    /** Ab so vielen Startern (Einzel oder Zweierbob) gibt es Medaillen. */
    public static final int MEDALS_FROM = 2;
    /** Punkte für die Gesamtwertung nach Platz (ab Platz 8 je 1 Punkt, ohne Ziel 0). */
    private static final int[] POINTS = {10, 8, 6, 5, 4, 3, 2};
    private static final int LATER_POINTS = 1;

    private Ranking() {
    }

    /** Ein Starter: Kennung (Einzel: Spieler, Zweierbob: das Paar), Zeit samt Strafen, im Ziel? */
    public record Entry(String id, long timeMs, boolean finished) {
    }

    /** Platzierter Starter: Platz 0 = nicht im Ziel. */
    public record Placed(Entry entry, int place, Medal medal, int points) {
        public boolean finished() {
            return entry.finished();
        }
    }

    public static List<Placed> rank(List<Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing((Entry e) -> !e.finished())
                .thenComparingLong(e -> e.finished() ? e.timeMs() : Long.MAX_VALUE));
        boolean medals = entries.size() >= MEDALS_FROM;
        List<Placed> out = new ArrayList<>();
        int place = 0;
        long lastTime = -1;
        for (int i = 0; i < sorted.size(); i++) {
            Entry e = sorted.get(i);
            if (!e.finished()) {
                out.add(new Placed(e, 0, null, 0));
                continue;
            }
            if (e.timeMs() != lastTime) {
                place = i + 1;
                lastTime = e.timeMs();
            }
            out.add(new Placed(e, place, medals ? Medal.forPlace(place) : null, points(place)));
        }
        return out;
    }

    /** Punkte für einen Platz (0 = nicht im Ziel). */
    public static int points(int place) {
        if (place <= 0) {
            return 0;
        }
        return place <= POINTS.length ? POINTS[place - 1] : LATER_POINTS;
    }
}
