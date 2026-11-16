package net.morni.wintersport.world;

import net.morni.wintersport.course.Bob;
import net.morni.wintersport.course.Path;
import net.morni.wintersport.course.Places;
import net.morni.wintersport.course.Terrain;
import java.util.Random;

/**
 * Die vier Eiskanäle der Bobbahn: Boden aus Blaueis, Wände aus Packeis (in den Kurven außen höher), oben Schnee,
 * Laternen auf der Mauer. Oben Starthäuser, unten Zielbogen, Auslauf auf Schnee und ein Polster aus Strohballen.
 */
final class BobTrack {

    private BobTrack() {
    }

    static Plan build() {
        Plan p = new Plan();
        Random r = new Random(5150);
        for (int lane = 0; lane < Bob.lanes(); lane++) {
            channel(p, lane, r);
        }
        Pistes.grandstand(p, Places.STAND_BOB[0], Places.STAND_BOB[1], 40);
        Pistes.fence(p, Bob.LANE_X[0] - 6, Bob.LANE_X[Bob.lanes() - 1] + Bob.SWING + 6, (int) Math.ceil(Bob.END_Z) + 3);
        return p;
    }

    private static void channel(Plan p, int lane, Random r) {
        Path path = Bob.path(lane);
        int x1 = Bob.LANE_X[lane] - 5;
        int x2 = Bob.LANE_X[lane] + Bob.SWING + 5;
        int z1 = (int) Math.floor(Bob.TOP_Z) - 2;
        int z2 = (int) Math.ceil(Bob.END_Z) + 2;
        double[][] dist = new double[x2 - x1 + 1][z2 - z1 + 1];
        float[][] yaw = new float[x2 - x1 + 1][z2 - z1 + 1];
        for (double[] row : dist) {
            java.util.Arrays.fill(row, Double.MAX_VALUE);
        }
        for (Path.Sample s : path.samples()) {
            for (int x = (int) s.x() - 4; x <= (int) s.x() + 4; x++) {
                for (int z = (int) s.z() - 4; z <= (int) s.z() + 4; z++) {
                    if (x < x1 || x > x2 || z < z1 || z > z2) {
                        continue;
                    }
                    double d = Math.hypot(x + 0.5 - s.x(), z + 0.5 - s.z());
                    if (d < dist[x - x1][z - z1]) {
                        dist[x - x1][z - z1] = d;
                        yaw[x - x1][z - z1] = s.yaw();
                    }
                }
            }
        }
        int lights = 0;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                double d = dist[x - x1][z - z1];
                int h = Terrain.height(z);
                if (z < Bob.TOP_Z) {
                    continue;
                }
                if (d <= Bob.HALF) {
                    String floor = z > Bob.RUNOUT_Z ? "snow_block" : "blue_ice";
                    p.set(x, h, z, floor);
                    for (int y = h + 1; y <= h + 4; y++) {
                        p.air(x, y, z);
                    }
                } else if (d <= Bob.WALL) {
                    // Wand: so hoch, dass sie über dem höchsten Boden daneben zwei Blöcke hat; in Kurven außen drei.
                    int top = Math.max(Terrain.height(z - 1), Math.max(h, Terrain.height(z + 1))) + 2;
                    boolean curve = Math.abs(yaw[x - x1][z - z1]) > 1 && Math.abs(Math.abs(yaw[x - x1][z - z1]) - 90) > 1;
                    if (curve) {
                        top++;
                    }
                    p.set(x, h, z, "packed_ice");
                    for (int y = h + 1; y <= top; y++) {
                        p.set(x, y, z, "packed_ice");
                    }
                    p.set(x, top + 1, z, "snow[layers=" + (2 + r.nextInt(2)) + "]");
                    if (++lights % 37 == 0) {
                        p.set(x, top + 1, z, "lantern[hanging=false]");
                    }
                }
            }
        }
        // Ende: Strohballen-Polster quer über den Kanal
        double ex = Bob.finishX(lane);
        int endZ = (int) Math.ceil(Bob.END_Z);
        for (int x = (int) Math.floor(ex) - 2; x <= (int) Math.floor(ex) + 2; x++) {
            for (int y = 1; y <= 3; y++) {
                p.set(x, y, endZ, "hay_block[axis=x]");
            }
        }
        // Startrampe: Zauntore quer über den Kanal, Starthaus darüber
        int gx = Bob.LANE_X[lane];
        int top = Terrain.TOP;
        for (int x = gx - 1; x <= gx + 1; x++) {
            p.set(x, top + 1, Bob.GATE_ROW_Z, "dark_oak_fence_gate[facing=north,open=false,in_wall=false]");
        }
        hut(p, gx, (int) Math.floor(Bob.TOP_Z) - 1, Bob.GATE_ROW_Z);
        Pistes.finishArch(p, (int) Math.floor(Bob.finishX(lane)), 1, (int) Math.floor(Bob.FINISH_Z));
    }

    /** Kleines Starthaus über den ersten Metern des Kanals. */
    private static void hut(Plan p, int cx, int back, int front) {
        int y0 = Terrain.TOP;
        int w = 4;
        for (int z = back; z <= front; z++) {
            for (int dy = 4; dy <= 6; dy++) {
                p.set(cx - w, y0 + dy, z, "dark_oak_planks");
                p.set(cx + w, y0 + dy, z, "dark_oak_planks");
            }
            for (int dy = 1; dy <= 3; dy++) {
                if (!p.has(cx - w, y0 + dy, z)) {
                    p.set(cx - w, y0 + dy, z, z == back || z == front ? "dark_oak_log[axis=y]" : "dark_oak_planks");
                }
                if (!p.has(cx + w, y0 + dy, z)) {
                    p.set(cx + w, y0 + dy, z, z == back || z == front ? "dark_oak_log[axis=y]" : "dark_oak_planks");
                }
            }
        }
        for (int x = cx - w; x <= cx + w; x++) {
            for (int dy = 1; dy <= 6; dy++) {
                p.set(x, y0 + dy, back - 1, "dark_oak_planks");
            }
            p.set(x, y0 + 6, front, "stripped_dark_oak_log[axis=x]");
            for (int z = back - 1; z <= front; z++) {
                p.set(x, y0 + 7, z, Math.abs(x - cx) == w ? "dark_oak_slab[type=bottom]" : "smooth_quartz_slab[type=bottom]");
                p.set(x, y0 + 8, z, "snow[layers=2]");
            }
        }
        p.set(cx, y0 + 3, back - 1, "glass_pane");
        Decor.hangingLantern(p, cx, y0 + 6, front - 3, 1);
        p.banner(cx, y0 + 5, front + 1, "light_blue_wall_banner[facing=south]", Decor.snowflake());
        p.banner(cx - 3, y0 + 5, front + 1, "white_wall_banner[facing=south]", Decor.lozenges());
        p.banner(cx + 3, y0 + 5, front + 1, "white_wall_banner[facing=south]", Decor.lozenges());
    }
}
