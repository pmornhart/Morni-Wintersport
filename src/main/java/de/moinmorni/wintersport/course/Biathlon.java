package de.moinmorni.wintersport.course;

import de.moinmorni.wintersport.logic.Line;
import java.util.List;

/**
 * Biathlon-Stadion im Tal (östlich vom Dorf): Rundkurs (Loipe, fünf Blöcke breit) im Uhrzeigersinn mit drei
 * Kontrollpunkten, Start und Ziel auf der Nordgeraden, nördlich davon der Schießstand mit zwölf Bahnen und je fünf
 * Scheiben in 14 Blöcken Abstand, westlich davon die Strafrunde.
 */
public final class Biathlon {

    /** Bahnen am Schießstand (so viele können gleichzeitig mitlaufen). */
    public static final int LANES = 12;
    /** Scheiben je Bahn. */
    public static final int TARGETS = 5;
    private static final int LANE_X0 = 79;
    private static final int LANE_STEP = 6;
    /** Reihe der Schießmatten und der Scheiben. */
    public static final int MAT_Z = 52;
    public static final int TARGET_Z = 38;
    /** Höhe der Scheiben (über dem Tal). */
    public static final int TARGET_Y = 2;

    /** Mittellinie der Loipe. */
    public static final double NORTH_Z = 58.5;
    public static final double SOUTH_Z = 96.5;
    public static final double WEST_X = 74.5;
    public static final double EAST_X = 156.5;
    public static final double HALF = 2.6;
    /** Start/Ziel-Linie (auch Rundenlinie) und das Starttor davor. */
    public static final double LAP_X = 104.0;
    public static final int START_GATE_X = 102;

    /** Strafrunde (kleine Schleife westlich vom Schießstand). */
    public static final double PEN_WEST = 60.5;
    public static final double PEN_EAST = 68.5;
    public static final double PEN_NORTH = 38.5;
    public static final double PEN_SOUTH = 50.5;
    public static final double PEN_HALF = 1.6;

    private static Path loop;
    private static Path penalty;

    private Biathlon() {
    }

    public static synchronized Path loop() {
        if (loop == null) {
            loop = new Path(List.of(new double[] {LAP_X - 3.5, NORTH_Z}, new double[] {EAST_X, NORTH_Z},
                    new double[] {EAST_X, SOUTH_Z}, new double[] {WEST_X, SOUTH_Z}, new double[] {WEST_X, NORTH_Z},
                    new double[] {LAP_X - 3.5, NORTH_Z}), 8);
        }
        return loop;
    }

    public static synchronized Path penaltyLoop() {
        if (penalty == null) {
            double mid = (PEN_WEST + PEN_EAST) / 2;
            penalty = new Path(List.of(new double[] {mid - 2, PEN_SOUTH}, new double[] {PEN_EAST, PEN_SOUTH},
                    new double[] {PEN_EAST, PEN_NORTH}, new double[] {PEN_WEST, PEN_NORTH},
                    new double[] {PEN_WEST, PEN_SOUTH}, new double[] {mid - 2, PEN_SOUTH}), 3);
        }
        return penalty;
    }

    /** Rundenlinie (Start, Runde, Ziel). */
    public static Line lapLine() {
        return Line.acrossX(LAP_X, NORTH_Z - HALF - 0.5, NORTH_Z + HALF + 0.5);
    }

    /** Kontrollpunkte: Mitte der Ost-, Süd- und Westgeraden. */
    public static List<Line> checkpoints() {
        double midZ = (NORTH_Z + SOUTH_Z) / 2;
        double midX = (WEST_X + EAST_X) / 2;
        return List.of(Line.acrossZ(midZ, EAST_X - HALF - 1, EAST_X + HALF + 1),
                Line.acrossX(midX, SOUTH_Z - HALF - 1, SOUTH_Z + HALF + 1),
                Line.acrossZ(midZ, WEST_X - HALF - 1, WEST_X + HALF + 1));
    }

    /** Linie der Strafrunde (zählt eine Runde) und ihr Wendepunkt gegenüber. */
    public static Line penaltyLine() {
        double mid = (PEN_WEST + PEN_EAST) / 2;
        return Line.acrossX(mid, PEN_SOUTH - PEN_HALF - 0.5, PEN_SOUTH + PEN_HALF + 0.5);
    }

    public static Line penaltyTurn() {
        double mid = (PEN_WEST + PEN_EAST) / 2;
        return Line.acrossX(mid, PEN_NORTH - PEN_HALF - 0.5, PEN_NORTH + PEN_HALF + 0.5);
    }

    /** Spalte (Mitte) der Schießbahn k. */
    public static int laneX(int k) {
        return LANE_X0 + LANE_STEP * k;
    }

    /** Scheibe t (0–4) der Bahn k: Block x. */
    public static int targetX(int k, int t) {
        return laneX(k) - TARGETS / 2 + t;
    }

    /** Startplatz i (0–11) hinter dem Starttor: {x, z}, Blick nach Osten. */
    public static double[] grid(int i) {
        int row = i / 4;
        int col = i % 4;
        double[] zs = {NORTH_Z - 2, NORTH_Z - 0.65, NORTH_Z + 0.65, NORTH_Z + 2};
        return new double[] {START_GATE_X - 1.5 - 2 * row, zs[col]};
    }

    /** Wo man nach dem Schießen (ohne Strafrunde) weiterläuft: hinter der Rundenlinie, Blick nach Osten. */
    public static double[] afterRange() {
        return new double[] {LAP_X + 2.5, NORTH_Z};
    }

    /** Anfang der Strafrunde. */
    public static double[] penaltyStart() {
        return new double[] {(PEN_WEST + PEN_EAST) / 2 - 2.5, PEN_SOUTH};
    }

    /** Liegt (x, z) auf der Loipe oder der Strafrunde? */
    public static boolean onTrack(double x, double z) {
        return loop().distance(x, z) <= HALF || penaltyLoop().distance(x, z) <= PEN_HALF;
    }
}
