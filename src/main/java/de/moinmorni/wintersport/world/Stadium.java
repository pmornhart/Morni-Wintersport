package de.moinmorni.wintersport.world;

import de.moinmorni.wintersport.course.Biathlon;
import de.moinmorni.wintersport.course.Path;
import de.moinmorni.wintersport.course.Places;
import java.util.Random;

/**
 * Biathlon-Stadion: Loipe (gespurt, mit Fahnen am Rand), Start- und Zielbogen mit Starttor, Schießstand mit zwölf
 * überdachten Matten und Scheiben vor einem Schneewall, die Strafrunde und eine Tribüne innen im Rund.
 */
final class Stadium {

    private Stadium() {
    }

    static Plan build() {
        Plan p = new Plan();
        Random r = new Random(1900);
        track(p, Biathlon.loop(), Biathlon.HALF, true, r);
        track(p, Biathlon.penaltyLoop(), Biathlon.PEN_HALF, false, r);
        range(p);
        start(p);
        Pistes.grandstand(p, Places.STAND_BIATHLON[0], Places.STAND_BIATHLON[1], 30);
        return p;
    }

    /** Loipe: weiß gespurt, zwei Spurrillen, außen Fahnen. */
    private static void track(Plan p, Path path, double half, boolean main, Random r) {
        int x1 = (int) Math.floor(path.samples().stream().mapToDouble(Path.Sample::x).min().orElse(0)) - 5;
        int x2 = (int) Math.ceil(path.samples().stream().mapToDouble(Path.Sample::x).max().orElse(0)) + 5;
        int z1 = (int) Math.floor(path.samples().stream().mapToDouble(Path.Sample::z).min().orElse(0)) - 5;
        int z2 = (int) Math.ceil(path.samples().stream().mapToDouble(Path.Sample::z).max().orElse(0)) + 5;
        double[][] dist = new double[x2 - x1 + 1][z2 - z1 + 1];
        for (double[] row : dist) {
            java.util.Arrays.fill(row, Double.MAX_VALUE);
        }
        for (Path.Sample s : path.samples()) {
            for (int x = (int) s.x() - 5; x <= (int) s.x() + 5; x++) {
                for (int z = (int) s.z() - 5; z <= (int) s.z() + 5; z++) {
                    if (x >= x1 && x <= x2 && z >= z1 && z <= z2) {
                        double d = Math.hypot(x + 0.5 - s.x(), z + 0.5 - s.z());
                        dist[x - x1][z - z1] = Math.min(dist[x - x1][z - z1], d);
                    }
                }
            }
        }
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                double d = dist[x - x1][z - z1];
                if (d <= half) {
                    boolean groove = main && Math.abs(d - 1.0) < 0.35;
                    p.set(x, 0, z, groove ? "light_gray_concrete" : "white_concrete");
                    p.air(x, 1, z);
                } else if (d <= half + 1.2) {
                    p.set(x, 0, z, "snow_block");
                    p.air(x, 1, z);
                }
            }
        }
        // Fahnen am Rand: außen blau, innen rot (Strafrunde ganz rot)
        int i = 0;
        for (Path.Sample s : path.samples()) {
            if (s.s() < i * 7.0) {
                continue;
            }
            i++;
            double rad = Math.toRadians(s.yaw());
            double nx = Math.cos(rad);
            double nz = Math.sin(rad);
            for (int side : new int[] {-1, 1}) {
                int fx = (int) Math.floor(s.x() + nx * side * (half + 1.6));
                int fz = (int) Math.floor(s.z() + nz * side * (half + 1.6));
                p.set(fx, 1, fz, "spruce_fence");
                String colour = !main ? "red" : side > 0 ? "light_blue" : "red";
                p.set(fx, 2, fz, colour + "_banner[rotation=" + Math.floorMod(Math.round(-s.yaw() / 22.5f) + 4, 16) + "]");
            }
        }
    }

    private static void range(Plan p) {
        int first = Biathlon.laneX(0) - 3;
        int last = Biathlon.laneX(Biathlon.LANES - 1) + 3;
        int mz = Biathlon.MAT_Z;
        int tz = Biathlon.TARGET_Z;
        // Schneewall hinter den Scheiben fängt alles ab
        for (int x = first - 2; x <= last + 2; x++) {
            for (int z = tz - 4; z <= tz - 1; z++) {
                int height = 8 - (tz - 1 - z) - (x < first || x > last ? 2 : 0);
                for (int y = 1; y <= height; y++) {
                    p.set(x, y, z, y == height ? "snow_block" : "packed_ice");
                }
                p.set(x, height + 1, z, "snow[layers=3]");
            }
        }
        for (int k = 0; k < Biathlon.LANES; k++) {
            int lx = Biathlon.laneX(k);
            // Scheibenbrett: weiß mit fünf schwarzen Scheiben, oben ein kleines Dach
            for (int x = lx - 3; x <= lx + 3; x++) {
                for (int y = 1; y <= 3; y++) {
                    p.set(x, y, tz, "white_concrete");
                }
                p.set(x, 4, tz, "spruce_slab[type=bottom]");
                p.set(x, 5, tz, "snow[layers=1]");
            }
            for (int t = 0; t < Biathlon.TARGETS; t++) {
                p.set(Biathlon.targetX(k, t), Biathlon.TARGET_Y, tz, "black_concrete");
            }
            p.set(lx - 3, 1, tz, "spruce_log[axis=y]");
            p.set(lx + 3, 1, tz, "spruce_log[axis=y]");
            // Matte
            for (int z = mz - 1; z <= mz + 1; z++) {
                for (int x = lx - 1; x <= lx + 1; x++) {
                    p.set(x, 0, z, "spruce_planks");
                    p.set(x, 1, z, x == lx ? "green_carpet" : "lime_carpet");
                }
            }
            // Trennwände zwischen den Bahnen
            for (int z = mz - 2; z <= mz + 2; z++) {
                p.set(lx + 3, 1, z, "spruce_fence");
            }
            // Bahnnummer am Boden vor der Matte (Zahl kommt als Schrift darüber)
            p.set(lx, 0, mz - 2, "light_blue_concrete");
        }
        // Dach über den Matten
        for (int x = first; x <= last; x++) {
            for (int z = mz - 2; z <= mz + 3; z++) {
                p.set(x, 5, z, "spruce_slab[type=top]");
                p.set(x, 6, z, "snow[layers=2]");
            }
            if ((x - first) % 6 == 0) {
                for (int y = 1; y <= 4; y++) {
                    p.set(x, y, mz + 3, "spruce_log[axis=y]");
                }
                Decor.hangingLantern(p, x + 3, 4, mz + 1, 0);
            }
            p.set(x, 1, mz + 3, x % 2 == 0 ? "spruce_fence" : "spruce_fence");
        }
        // Schnee zwischen Matten und Scheiben (niemand läuft dort)
        for (int x = first; x <= last; x++) {
            for (int z = tz + 1; z < mz - 2; z++) {
                p.set(x, 0, z, "snow_block");
            }
        }
    }

    /** Start- und Zielbogen über der Rundenlinie, Starttor davor. */
    private static void start(Plan p) {
        int lx = (int) Math.floor(Biathlon.LAP_X);
        int zc = (int) Math.floor(Biathlon.NORTH_Z);
        int w = 3;
        for (int dy = 1; dy <= 6; dy++) {
            p.set(lx, dy, zc - w - 1, "stripped_spruce_log[axis=y]");
            p.set(lx, dy, zc + w + 1, "stripped_spruce_log[axis=y]");
        }
        for (int z = zc - w - 1; z <= zc + w + 1; z++) {
            p.set(lx, 7, z, "spruce_planks");
            p.set(lx, 8, z, "snow[layers=2]");
        }
        for (int z = zc - w + 1; z <= zc + w - 1; z += 2) {
            p.banner(lx, 6, z, "yellow_banner[rotation=4]", Decor.gold());
        }
        for (int z = zc - w; z <= zc + w; z++) {
            p.set(lx, 0, z, Math.floorMod(z, 2) == 0 ? "black_concrete" : "white_concrete");
        }
        for (int z = (int) Math.floor(Biathlon.NORTH_Z - Biathlon.HALF); z <= (int) Math.floor(Biathlon.NORTH_Z + Biathlon.HALF); z++) {
            p.set(Biathlon.START_GATE_X, 1, z, "spruce_fence_gate[facing=east,open=false,in_wall=false]");
        }
    }
}
