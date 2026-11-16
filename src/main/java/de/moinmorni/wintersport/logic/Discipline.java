package de.moinmorni.wintersport.logic;

/** Die drei Disziplinen. Gewertet wird immer nach Zeit (kleiner ist besser). */
public enum Discipline {
    /** Riesenslalom: im Boot über die Eispiste durch rote und blaue Tore. */
    SLALOM("slalom"),
    /** Biathlon: Langlauf-Runden zu Fuß, dazwischen fünf Scheiben am Schießstand, Fehler = Strafrunde. */
    BIATHLON("biathlon"),
    /** Bobbahn: im Boot durch den Eiskanal, Partys zu zweit im Zweierbob. */
    BOB("bob");

    private final String id;

    Discipline(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Fährt man im Boot (Tore, Kontrollpunkte, Bahnen nebeneinander)? */
    public boolean boats() {
        return this != BIATHLON;
    }

    public static Discipline byId(String id) {
        for (Discipline d : values()) {
            if (d.id.equalsIgnoreCase(id)) {
                return d;
            }
        }
        return null;
    }
}
