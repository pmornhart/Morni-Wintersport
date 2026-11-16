package net.morni.wintersport.course;

import net.morni.wintersport.logic.Line;
import java.util.ArrayList;
import java.util.List;

/**
 * Bobbahn: vier gleiche Eiskanäle nebeneinander durch den Bergwald. Jeder Kanal ist drei Blöcke breit (Blaueis) mit
 * Wänden aus Packeis und schlängelt sich in zwölf Kehren den Berg hinab: auf jeder Terrasse eine Querfahrt mit zwei
 * runden Kurven, dazwischen gerade Stücke bergab, auf denen der Bob über die Absätze springt.
 */
public final class Bob {

    /** Westlichste Spalte jeder Bahn (die Querfahrten gehen 14 Blöcke nach Osten und zurück). */
    public static final int[] LANE_X = {74, 100, 126, 152};
    /** Querfahrt: so weit nach Osten. */
    public static final int SWING = 14;
    /** Halbe Breite des Kanals (Mitte ± 1,6 → drei Blöcke). */
    public static final double HALF = 1.6;
    /** Wände bis zu diesem Abstand von der Mittellinie. */
    public static final double WALL = 2.75;
    public static final double RADIUS = 4.0;
    /** Erste Reihe des Kanals (Rückwand des Starthauses). */
    public static final double TOP_Z = -293.5;
    public static final int GATE_ROW_Z = -287;
    public static final double START_Z = -288.4;
    public static final double FINISH_Z = -35.0;
    /** Bis hier Blaueis, danach Schnee zum Bremsen; der Kanal endet an einem Strohballen-Polster. */
    public static final int RUNOUT_Z = -29;
    public static final double END_Z = -23.5;
    /** Kontrollpunkte (zum Zurücksetzen) alle so viele Blöcke Strecke. */
    private static final double CHECK_EVERY = 16;

    private static final Path[] PATHS = new Path[LANE_X.length];

    private Bob() {
    }

    public static int lanes() {
        return LANE_X.length;
    }

    /** x-Mitte des Kanals bei Querversatz {@code rel} (0 oder {@link #SWING}). */
    public static double x(int lane, int rel) {
        return LANE_X[lane] + rel + 0.5;
    }

    /** Mittellinie der Bahn. */
    public static synchronized Path path(int lane) {
        if (PATHS[lane] == null) {
            List<double[]> c = new ArrayList<>();
            c.add(new double[] {x(lane, 0), TOP_Z});
            int side = 0;
            for (int i = Terrain.TERRACES - 1; i >= 0; i--) {
                double z = Terrain.terraceMiddle(i);
                c.add(new double[] {x(lane, side), z});
                side = side == 0 ? SWING : 0;
                c.add(new double[] {x(lane, side), z});
            }
            c.add(new double[] {x(lane, side), END_Z});
            PATHS[lane] = new Path(c, RADIUS);
        }
        return PATHS[lane];
    }

    /** x-Mitte am Ende (Ziel): nach zwölf Querfahrten wieder auf der Westseite. */
    public static double finishX(int lane) {
        return x(lane, Terrain.TERRACES % 2 == 0 ? 0 : SWING);
    }

    public static Line finish(int lane) {
        double cx = finishX(lane);
        return Line.acrossZ(FINISH_Z, cx - 3, cx + 3);
    }

    /** Stellen zum Zurücksetzen: Strecke s, alle {@link #CHECK_EVERY} Blöcke, ab dem Start. */
    public static List<Path.Sample> checkpoints(int lane) {
        Path p = path(lane);
        List<Path.Sample> out = new ArrayList<>();
        double startS = p.samples().get(p.nearest(x(lane, 0), START_Z, -1)).s();
        out.add(p.at(startS));
        for (double s = startS + CHECK_EVERY; s < p.length() - 20; s += CHECK_EVERY) {
            out.add(p.at(s));
        }
        return out;
    }
}
