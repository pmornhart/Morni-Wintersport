package de.moinmorni.wintersport.world;

import de.moinmorni.wintersport.course.Places;
import de.moinmorni.wintersport.course.Terrain;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Der Boden der ganzen Welt: verschneites Tal, der Skiberg in Terrassen, ringsum Berge (im Norden hohe Gipfel mit
 * Felswänden, an den Seiten und im Süden Hänge, die das Tal abschließen). Nur eine Kruste von ein paar Blöcken – von
 * unten sieht es niemand. Gebaut in Streifen, damit der Server dabei flüssig bleibt.
 */
final class Ground {

    private static final int STRIP = 48;
    private static final int CRUST = 4;
    /** So hoch ist die unsichtbare Wand am Rand der Welt. */
    private static final int BORDER_HEIGHT = 10;

    private Ground() {
    }

    /** Baupläne (Streifen von West nach Ost), erst beim Bauen erzeugt. */
    static List<Supplier<Plan>> strips() {
        List<Supplier<Plan>> out = new ArrayList<>();
        for (int x = Places.MIN_X; x <= Places.MAX_X; x += STRIP) {
            int from = x;
            int to = Math.min(Places.MAX_X, x + STRIP - 1);
            out.add(() -> strip(from, to));
        }
        return out;
    }

    /** Boden für einen Ausschnitt (das Dorf wird zuerst gebaut). */
    static Plan area(int x1, int z1, int x2, int z2) {
        Plan all = strip(x1, x2);
        Plan out = new Plan();
        all.forEach((k, d) -> {
            int z = Plan.keyZ(k);
            if (z >= z1 && z <= z2) {
                out.set(Plan.keyX(k), Plan.keyY(k), z, d);
            }
        });
        return out;
    }

    private static double noise(int x, int z) {
        return Math.sin(x * 0.11 + 1.3) * Math.cos(z * 0.13 - 0.7) + 0.5 * Math.sin(x * 0.27 + z * 0.19)
                + 0.25 * Math.cos(x * 0.53 - z * 0.41);
    }

    /** Höhe der Oberfläche (über dem Tal) samt Bergen am Rand. */
    static int surface(int x, int z) {
        int h = Terrain.height(z);
        double extra = 0;
        if (x < -172) {
            extra = Math.max(extra, (-172 - x) * 1.5);
        }
        if (x > 180) {
            extra = Math.max(extra, (x - 180) * 1.5);
        }
        if (z > 108) {
            extra = Math.max(extra, (z - 108) * 1.15);
        }
        if (z < -298) {
            extra = Math.max(extra, (-298 - z) * 2.3);
        }
        if (extra > 0) {
            extra += (noise(x, z) + 1) * 3.0 * Math.min(1, extra / 6);
        }
        return h + (int) Math.round(extra);
    }

    private static Plan strip(int x1, int x2) {
        Plan p = new Plan();
        Random r = new Random(7331L * x1 + 17);
        for (int x = x1; x <= x2; x++) {
            for (int z = Places.MIN_Z; z <= Places.MAX_Z; z++) {
                int h = surface(x, z);
                int low = Math.min(Math.min(surface(x - 1, z), surface(x + 1, z)), Math.min(surface(x, z - 1), surface(x, z + 1)));
                int steep = h - low;
                boolean rocky = steep >= 3 && h > Terrain.TOP;
                int depth = Math.max(CRUST, steep + 3);
                for (int y = h - depth; y <= h; y++) {
                    String block;
                    if (y == h) {
                        block = rocky ? Decor.rock(r) : Decor.SNOW;
                    } else if (y >= h - 1 && !rocky) {
                        block = Decor.SNOW;
                    } else {
                        block = Decor.rock(r);
                    }
                    p.set(x, y, z, block);
                }
                if (rocky && r.nextInt(3) == 0) {
                    p.set(x, h + 1, z, "snow[layers=" + (1 + r.nextInt(3)) + "]");
                }
                boolean free = !Zones.reserved(x, z);
                // Terrassen-Absatz weich machen: halbe Schneeschicht vor der Stufe.
                if (free && !rocky && surface(x, z - 1) == h + 1) {
                    p.set(x, h + 1, z, "snow[layers=4]");
                } else if (free && !rocky && noise(x * 2, z * 2) > 1.1) {
                    p.set(x, h + 1, z, "snow[layers=" + (1 + r.nextInt(2)) + "]");
                }
                if (x == Places.MIN_X || x == Places.MAX_X || z == Places.MIN_Z || z == Places.MAX_Z) {
                    for (int y = h + 1; y <= h + BORDER_HEIGHT; y++) {
                        p.set(x, y, z, "barrier");
                    }
                }
            }
        }
        return p;
    }
}
