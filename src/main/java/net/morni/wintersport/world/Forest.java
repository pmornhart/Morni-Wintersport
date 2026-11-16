package net.morni.wintersport.world;

import net.morni.wintersport.course.Places;
import net.morni.wintersport.course.Terrain;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Bergwald: verschneite Fichten zwischen den Pisten und Bobkanälen, an den Hängen ringsum und locker im Tal – nie auf
 * Strecken, Wegen oder im Dorf.
 */
final class Forest {

    private static final int STRIP = 64;
    private static final int CELL = 7;

    private Forest() {
    }

    static List<Supplier<Plan>> strips() {
        List<Supplier<Plan>> out = new ArrayList<>();
        for (int x = Places.MIN_X; x <= Places.MAX_X; x += STRIP) {
            int from = x;
            int to = Math.min(Places.MAX_X, x + STRIP - 1);
            out.add(() -> strip(from, to));
        }
        return out;
    }

    private static boolean clear(int x, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx += radius) {
            for (int dz = -radius; dz <= radius; dz += radius) {
                if (Zones.reserved(x + dx, z + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Plan strip(int x1, int x2) {
        Plan p = new Plan();
        for (int gx = Math.floorDiv(x1, CELL) * CELL; gx <= x2; gx += CELL) {
            for (int gz = Places.MIN_Z + 4; gz <= Places.MAX_Z - 4; gz += CELL) {
                Random r = new Random(gx * 341873128712L + gz * 132897987541L);
                int x = gx + r.nextInt(CELL);
                int z = gz + r.nextInt(CELL);
                if (x < x1 || x > x2 || x < Places.MIN_X + 3 || x > Places.MAX_X - 3) {
                    continue;
                }
                int h = Ground.surface(x, z);
                int steep = Math.max(Math.abs(Ground.surface(x - 2, z) - h), Math.abs(Ground.surface(x, z - 2) - h));
                boolean mountain = z < Terrain.FOOT_Z;
                boolean backdrop = h > Terrain.TOP + 2 || x < -172 || x > 180 || z > 108;
                int chance = backdrop ? (h > Terrain.TOP + 26 ? 0 : 45) : mountain ? 72 : 28;
                if (steep > 3 || r.nextInt(100) >= chance || !clear(x, z, 4)) {
                    continue;
                }
                int height = 7 + r.nextInt(backdrop ? 6 : 7);
                Decor.spruce(p, x, h + 1, z, height, r);
                // Kleine Schneehügel und Gebüsch am Fuß
                if (r.nextInt(3) == 0) {
                    p.setIfEmpty(x + 1, h + 1, z + 1, "snow[layers=" + (2 + r.nextInt(3)) + "]");
                }
            }
        }
        return p;
    }
}
