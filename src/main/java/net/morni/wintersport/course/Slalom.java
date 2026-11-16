package net.morni.wintersport.course;

import net.morni.wintersport.logic.Gate;
import net.morni.wintersport.logic.Line;
import java.util.ArrayList;
import java.util.List;

/**
 * Riesenslalom: vier gleiche Pisten nebeneinander (je Lauf fahren bis zu vier gleichzeitig). Jede Piste ist neun
 * Blöcke breit, aus Packeis mit Schneewällen, und hat zwölf Tore – auf jeder Terrasse eines, abwechselnd links (rot)
 * und rechts (blau). Gestartet wird oben auf dem Plateau hinter einem Starttor aus Zauntoren.
 */
public final class Slalom {

    /** Mittlere Blockspalte jeder Piste. */
    public static final int[] LANE_X = {-158, -140, -122, -104};
    /** Halbe Breite der Eisfläche (Mitte ± 4 = 9 Blöcke). */
    public static final int HALF = 4;
    /** Ab hier (nach Norden) gibt es kein Eis mehr – Rückwand des Starthauses. */
    public static final int TOP_Z = -292;
    /** Reihe mit dem Starttor (Zauntore). */
    public static final int GATE_ROW_Z = -284;
    /** Wo das Boot vor dem Start steht (Mitte). */
    public static final double START_Z = -285.4;
    /** Ziellinie. */
    public static final double FINISH_Z = -35.0;
    /** Bis hier Eis zum Ausrollen, danach Schnee (bremst) und der Fangzaun. */
    public static final int RUNOUT_Z = -27;
    public static final int FENCE_Z = -20;
    /** Durchfahrt eines Tors: von der Mitte 0,5 bis 4 Blöcke zur Seite. */
    private static final double GATE_INNER = 0.5;
    private static final double GATE_OUTER = 4.0;
    private static final double LINE_HALF = 5.0;

    private Slalom() {
    }

    public static int lanes() {
        return LANE_X.length;
    }

    /** x-Mitte der Piste (Blockmitte). */
    public static double centerX(int lane) {
        return LANE_X[lane] + 0.5;
    }

    /** Reihe (z) des Tors {@code g} (0 = oberstes). */
    public static int gateRow(int g) {
        int terrace = Terrain.TERRACES - 1 - g;
        return Terrain.FOOT_Z - (Terrain.PERIOD * terrace + 8);
    }

    public static int gateCount() {
        return Terrain.TERRACES;
    }

    /** Linkes Tor (rot) für gerade Nummern, rechtes (blau) für ungerade. „Links“ = Westen, von oben gesehen. */
    public static boolean leftGate(int g) {
        return g % 2 == 0;
    }

    public static List<Gate> gates(int lane) {
        double cx = centerX(lane);
        List<Gate> out = new ArrayList<>();
        for (int g = 0; g < gateCount(); g++) {
            double z = gateRow(g) + 0.5;
            Line line = Line.acrossZ(z, cx - LINE_HALF, cx + LINE_HALF);
            double a = leftGate(g) ? cx - GATE_OUTER : cx + GATE_INNER;
            double b = leftGate(g) ? cx - GATE_INNER : cx + GATE_OUTER;
            out.add(new Gate(line, (a - line.ax()) / line.length(), (b - line.ax()) / line.length(), leftGate(g)));
        }
        return out;
    }

    public static Line finish(int lane) {
        double cx = centerX(lane);
        return Line.acrossZ(FINISH_Z, cx - LINE_HALF, cx + LINE_HALF);
    }

    /** Ist (x, z) auf der Eisfläche der Piste? */
    public static boolean onIce(int lane, int x, int z) {
        return Math.abs(x - LANE_X[lane]) <= HALF && z >= TOP_Z && z <= RUNOUT_Z;
    }

    /** Wohin man nach dem Tor {@code g} zurückgesetzt wird (−1 = Start). */
    public static double[] resetAfter(int lane, int g) {
        if (g < 0) {
            return new double[] {centerX(lane), START_Z};
        }
        return new double[] {centerX(lane), gateRow(g) + 3.5};
    }
}
