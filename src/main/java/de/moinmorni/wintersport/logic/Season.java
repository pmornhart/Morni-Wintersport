package de.moinmorni.wintersport.logic;

import java.time.LocalDate;
import java.time.Month;

/**
 * Saison-Schalter aus der config.yml: {@code immer} offen oder nur im {@code winter} (1. Dezember bis 31. März).
 * Außerhalb ist der Modus „geschlossen“ – das Dorf bleibt zum Anschauen offen, Wettkämpfe gibt es keine.
 */
public enum Season {
    ALWAYS("immer"), WINTER("winter");

    /** Erster Tag der Wintersaison. */
    public static final int OPEN_MONTH = 12;
    /** Letzter Monat der Wintersaison (bis einschließlich 31. März). */
    public static final int LAST_MONTH = 3;

    private final String id;

    Season(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Aus der config.yml; unbekannt → immer. */
    public static Season parse(String s) {
        if (s != null) {
            String v = s.trim().toLowerCase(java.util.Locale.ROOT);
            if (v.equals("winter") || v.equals("saison") || v.equals("season")) {
                return WINTER;
            }
        }
        return ALWAYS;
    }

    public boolean open(LocalDate day) {
        if (this == ALWAYS) {
            return true;
        }
        int m = day.getMonthValue();
        return m >= OPEN_MONTH || m <= LAST_MONTH;
    }

    /** Wann es (wieder) losgeht: heute, wenn offen, sonst der nächste 1. Dezember. */
    public LocalDate nextOpening(LocalDate day) {
        if (open(day)) {
            return day;
        }
        return LocalDate.of(day.getYear(), Month.DECEMBER, 1);
    }

    /** Letzter Tag der laufenden Saison (null bei „immer“ oder außerhalb der Saison). */
    public LocalDate closing(LocalDate day) {
        if (this == ALWAYS || !open(day)) {
            return null;
        }
        int year = day.getMonthValue() >= OPEN_MONTH ? day.getYear() + 1 : day.getYear();
        return LocalDate.of(year, Month.MARCH, 31);
    }
}
