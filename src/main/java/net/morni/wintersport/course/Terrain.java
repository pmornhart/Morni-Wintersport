package net.morni.wintersport.course;

/**
 * Höhe des Skibergs (ohne Minecraft): Das Tal liegt auf Höhe 0 (Welt-y {@link #FLOOR_Y}). Nördlich vom Bergfuß
 * ({@link #FOOT_Z}) steigt der Berg in Terrassen an – je {@link #PERIOD} Blöcke eine Stufe aus zwei Absätzen zu je einem
 * Block. Boote fahren auf den flachen Stücken und springen über die Absätze (höchstens zwei Blöcke tief, so geht kein
 * Boot kaputt). Weil die Höhe nur von z abhängt, passen Piste, Bobbahn, Rodelbahn und Lift von selbst zusammen.
 */
public final class Terrain {

    /** Welt-y der Talsohle (oberster Block); man steht auf {@code FLOOR_Y + 1}. */
    public static final int FLOOR_Y = 100;
    /** Letzte Reihe des Tals; nördlich davon (kleineres z) beginnt der Berg. */
    public static final int FOOT_Z = -40;
    /** Länge einer Terrasse samt Absätzen. */
    public static final int PERIOD = 20;
    public static final int TERRACES = 12;
    /** Höhe des Gipfelplateaus (Starthäuser, Bergstation). */
    public static final int TOP = 2 * TERRACES;
    /** Ab hier (nach Norden) liegt das Plateau. */
    public static final int PLATEAU_Z = FOOT_Z - PERIOD * TERRACES;

    private Terrain() {
    }

    /** Höhe des Bodens (oberster Block, relativ zum Tal) in der Reihe z. */
    public static int height(int z) {
        int d = FOOT_Z - z;
        if (d <= 0) {
            return 0;
        }
        if (d > PERIOD * TERRACES) {
            return TOP;
        }
        int i = (d - 1) / PERIOD;
        int r = (d - 1) % PERIOD;
        return 2 * i + Math.min(r + 1, 2);
    }

    public static int height(double z) {
        return height((int) Math.floor(z));
    }

    /** Welt-y, auf dem man (oder ein Boot) an dieser Stelle steht. */
    public static double standY(double z) {
        return FLOOR_Y + height(z) + 1;
    }

    /** z-Mitte der flachen Terrasse i (0 = unterste) – hier stehen Tore und Kurven. */
    public static double terraceMiddle(int i) {
        return FOOT_Z - (PERIOD * i + 13) + 0.5;
    }
}
