package net.morni.wintersport.world;

import net.morni.wintersport.course.Biathlon;
import net.morni.wintersport.course.Bob;
import net.morni.wintersport.course.Path;
import net.morni.wintersport.course.Places;
import net.morni.wintersport.course.Slalom;
import java.util.BitSet;

/**
 * Was wo frei bleiben muss (keine Bäume, keine Schneehügel): Pisten, Bobkanäle, Rodelbahn, Lift, Dorf, Stadion und die
 * Zielbereiche. Einmal für die ganze Welt als Raster ausgerechnet.
 */
final class Zones {

    private static final int W = Places.MAX_X - Places.MIN_X + 1;
    private static final int H = Places.MAX_Z - Places.MIN_Z + 1;
    private static BitSet reserved;
    private static BitSet tracks;

    private Zones() {
    }

    private static int idx(int x, int z) {
        return (x - Places.MIN_X) * H + (z - Places.MIN_Z);
    }

    private static boolean inside(int x, int z) {
        return x >= Places.MIN_X && x <= Places.MAX_X && z >= Places.MIN_Z && z <= Places.MAX_Z;
    }

    /** Gebaut oder Weg: hier keine Bäume und keine Schneehaufen. */
    static synchronized boolean reserved(int x, int z) {
        init();
        return inside(x, z) && reserved.get(idx(x, z));
    }

    /** Eis oder Wand einer Strecke (genauer als {@link #reserved}). */
    static synchronized boolean track(int x, int z) {
        init();
        return inside(x, z) && tracks.get(idx(x, z));
    }

    private static void box(BitSet set, int x1, int z1, int x2, int z2) {
        for (int x = Math.max(Places.MIN_X, x1); x <= Math.min(Places.MAX_X, x2); x++) {
            for (int z = Math.max(Places.MIN_Z, z1); z <= Math.min(Places.MAX_Z, z2); z++) {
                set.set(idx(x, z));
            }
        }
    }

    private static void around(BitSet set, Path path, double radius) {
        int r = (int) Math.ceil(radius) + 1;
        for (Path.Sample s : path.samples()) {
            int cx = (int) Math.floor(s.x());
            int cz = (int) Math.floor(s.z());
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    if (inside(x, z) && Math.hypot(x + 0.5 - s.x(), z + 0.5 - s.z()) <= radius) {
                        set.set(idx(x, z));
                    }
                }
            }
        }
    }

    private static void init() {
        if (reserved != null) {
            return;
        }
        BitSet res = new BitSet(W * H);
        BitSet tr = new BitSet(W * H);
        for (int lx : Slalom.LANE_X) {
            box(tr, lx - Slalom.HALF - 1, Slalom.TOP_Z, lx + Slalom.HALF + 1, Slalom.FENCE_Z);
            box(res, lx - Slalom.HALF - 3, Slalom.TOP_Z - 8, lx + Slalom.HALF + 3, Slalom.FENCE_Z);
        }
        for (int lane = 0; lane < Bob.lanes(); lane++) {
            around(tr, Bob.path(lane), Bob.WALL + 0.5);
            around(res, Bob.path(lane), Bob.WALL + 3);
            box(res, Bob.LANE_X[lane] - 4, (int) Bob.TOP_Z - 8, Bob.LANE_X[lane] + 4, (int) Bob.TOP_Z + 8);
        }
        box(tr, Places.RODEL_X - Places.RODEL_HALF - 1, Places.RODEL_START_Z - 4, Places.RODEL_X + Places.RODEL_HALF + 1,
                Places.RODEL_RUNOUT_Z + 3);
        box(res, Places.RODEL_X - Places.RODEL_HALF - 3, Places.RODEL_START_Z - 9, Places.RODEL_X + Places.RODEL_HALF + 3,
                Places.RODEL_RUNOUT_Z + 4);
        box(res, Places.LIFT_X - 6, Places.LIFT_TOP_Z - 8, Places.LIFT_X + 6, Places.LIFT_VALLEY_Z + 6);
        // Zielbereiche mit Tribünen, Dorf, Stadion
        box(res, -170, -44, -92, -4);
        box(res, 64, -44, 180, -4);
        box(res, -96, -44, -60, -20);
        box(res, -52, -30, 50, 50);
        box(res, 50, 30, 166, 106);
        box(res, -60, -24, 60, 30);
        reserved = res;
        tracks = tr;
        reserved.or(tracks);
        // Wege vom Dorf zu den Zielen und zum Stadion
        box(reserved, -100, -20, -40, -12);
        box(reserved, 40, -20, 80, -12);
        box(reserved, 40, 20, 60, 40);
        // Rundkurs selbst ist schon im Stadion-Kasten; Strafrunde und Schießstand auch.
        if (Biathlon.LANES > 0) {
            box(reserved, (int) Biathlon.PEN_WEST - 5, (int) Biathlon.PEN_NORTH - 5, (int) Biathlon.PEN_EAST + 5,
                    (int) Biathlon.PEN_SOUTH + 5);
        }
    }
}
