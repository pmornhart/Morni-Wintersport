package de.moinmorni.wintersport.world;

import de.moinmorni.wintersport.course.Places;
import java.util.Random;

/**
 * Das verschneite Bergdorf (Wartebereich): Dorfplatz mit großem Christbaum, Siegertreppchen mit Tafeln, Almhütten
 * mit Schneedächern, Glühweinstand, Eisfläche zum Rutschen, Wege zum Lift, zu den Zielen und zum Stadion, Laternen.
 */
final class VillagePlan {

    private static final int TREE_HEIGHT = 20;

    private VillagePlan() {
    }

    static Plan build() {
        Plan p = new Plan();
        Random r = new Random(2412);
        paths(p, r);
        plaza(p, r);
        tree(p, r);
        podium(p);
        boards(p);
        Decor.chalet(p, -34, -24, -24, -16, 0, true, r);
        Decor.chalet(p, 24, -24, 34, -16, 0, true, r);
        Decor.chalet(p, 30, -4, 38, 6, 0, false, r);
        Decor.chalet(p, 28, 32, 36, 42, 0, false, r);
        Decor.chalet(p, -14, 36, -6, 44, 0, false, r);
        Decor.chalet(p, 6, 36, 14, 44, 0, false, r);
        Decor.chalet(p, -40, 34, -32, 42, 0, false, r);
        Decor.chalet(p, -44, -2, -36, 6, 0, true, r);
        mulledWine(p);
        rink(p, r);
        return p;
    }

    /** Gepflasterter Weg (Breite 3) von a nach b, mit etwas Schnee darauf und Laternen alle zehn Blöcke. */
    static void path(Plan p, int x1, int z1, int x2, int z2, Random r) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int cx = (int) Math.round(x1 + (x2 - x1) * t);
            int cz = (int) Math.round(z1 + (z2 - z1) * t);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = cx + dx;
                    int z = cz + dz;
                    p.set(x, 0, z, r.nextInt(5) == 0 ? "cobblestone" : r.nextInt(3) == 0 ? "andesite" : "stone_bricks");
                    if (r.nextInt(6) == 0) {
                        p.set(x, 1, z, "snow[layers=1]");
                    } else {
                        p.air(x, 1, z);
                    }
                }
            }
            if (i % 10 == 5) {
                int ox = Math.abs(z2 - z1) > Math.abs(x2 - x1) ? 2 : 0;
                int oz = ox == 0 ? 2 : 0;
                Decor.lanternPost(p, cx + ox, 1, cz + oz);
            }
        }
    }

    private static void paths(Plan p, Random r) {
        // zum Lift und weiter zum Riesenslalom
        path(p, -14, 2, -48, -14, r);
        path(p, -48, -14, -66, -27, r);
        path(p, -66, -27, -100, -12, r);
        path(p, -100, -12, -112, -12, r);
        // zur Bobbahn
        path(p, 14, 2, 46, -10, r);
        path(p, 46, -10, 98, -12, r);
        // zum Stadion
        path(p, 12, 20, 46, 32, r);
        path(p, 46, 32, 72, 52, r);
        path(p, 72, 52, 88, 64, r);
        // nach Süden zur Ankunft
        path(p, 0, 22, 0, 30, r);
    }

    private static void plaza(Plan p, Random r) {
        int cz = Places.PLAZA_Z;
        int rad = Places.PLAZA_R;
        for (int x = -rad - 1; x <= rad + 1; x++) {
            for (int z = cz - rad - 1; z <= cz + rad + 1; z++) {
                double d = Math.hypot(x, z - cz);
                if (d > rad + 0.5) {
                    continue;
                }
                String block;
                if (d > rad - 2) {
                    block = r.nextInt(4) == 0 ? "stone_bricks" : "snow_block";
                } else if (d > rad - 3) {
                    block = "polished_andesite";
                } else {
                    block = Decor.lozenge(x, z, 2) ? "stone_bricks" : "polished_diorite";
                }
                p.set(x, 0, z, block);
                if (d > rad - 2 && r.nextInt(3) == 0) {
                    p.set(x, 1, z, "snow[layers=" + (1 + r.nextInt(2)) + "]");
                } else {
                    p.air(x, 1, z);
                }
            }
        }
        // Bänke und Laternen rund um den Platz
        for (int a = 0; a < 360; a += 45) {
            double rad2 = Math.toRadians(a + 22.5);
            int x = (int) Math.round(Math.sin(rad2) * (rad - 2));
            int z = cz + (int) Math.round(Math.cos(rad2) * (rad - 2));
            Decor.lanternPost(p, x, 1, z);
        }
        Decor.bench(p, -4, cz + 11, 4, cz + 11, 1, "south");
        Decor.bench(p, -11, cz - 3, -11, cz + 3, 1, "west");
        Decor.bench(p, 11, cz - 3, 11, cz + 3, 1, "east");
    }

    /** Christbaum: hohe Fichte mit Kugeln und Lichtern, Stern oben, Geschenke darunter. */
    private static void tree(Plan p, Random r) {
        int tx = Places.TREE[0];
        int tz = Places.TREE[1];
        int top = TREE_HEIGHT;
        for (int y = 1; y < top; y++) {
            p.set(tx, y, tz, "spruce_log[axis=y]");
        }
        String[] baubles = {"red_concrete", "gold_block", "shroomlight", "pearlescent_froglight", "red_concrete",
            "ochre_froglight", "sea_lantern", "verdant_froglight", "red_concrete", "gold_block"};
        for (int y = 3; y <= top; y++) {
            double radius = 1 + (top - y) * 0.31;
            if ((top - y) % 3 == 2) {
                radius -= 0.8;
            }
            int ir = (int) Math.ceil(radius);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    double d = Math.hypot(dx, dz);
                    if ((dx == 0 && dz == 0) || d > radius + 0.3) {
                        continue;
                    }
                    boolean shell = d > radius - 1.1;
                    String block = Decor.LEAVES;
                    if (shell && r.nextInt(7) == 0) {
                        block = baubles[r.nextInt(baubles.length)];
                    }
                    p.set(tx + dx, y, tz + dz, block);
                }
            }
        }
        // Schnee auf den Ästen
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                for (int y = top + 1; y >= 3; y--) {
                    var d = p.get(tx + dx, y, tz + dz);
                    if (d != null) {
                        if (d.getMaterial().name().endsWith("LEAVES") && r.nextInt(3) > 0) {
                            Decor.snowCap(p, tx + dx, y, tz + dz, 1);
                        }
                        break;
                    }
                }
            }
        }
        // Stern
        p.set(tx, top + 1, tz, "gold_block");
        p.set(tx, top + 2, tz, "glowstone");
        p.set(tx, top + 3, tz, "gold_block");
        p.set(tx - 1, top + 2, tz, "gold_block");
        p.set(tx + 1, top + 2, tz, "gold_block");
        p.set(tx, top + 2, tz - 1, "gold_block");
        p.set(tx, top + 2, tz + 1, "gold_block");
        p.set(tx, top + 4, tz, "end_rod[facing=up]");
        // Geschenke
        String[][] gifts = {{"red_wool", "yellow_carpet"}, {"light_blue_wool", "white_carpet"}, {"lime_wool", "red_carpet"},
            {"white_wool", "red_carpet"}, {"yellow_wool", "blue_carpet"}, {"magenta_wool", "lime_carpet"}};
        int[][] spots = {{-3, 4}, {3, 3}, {-4, -1}, {4, -2}, {1, 5}, {-1, -5}, {5, 1}, {-2, 4}};
        for (int i = 0; i < spots.length; i++) {
            String[] g = gifts[i % gifts.length];
            int gx = tx + spots[i][0];
            int gz = tz + spots[i][1];
            p.set(gx, 1, gz, g[0]);
            p.set(gx, 2, gz, g[1]);
        }
    }

    /** Siegertreppchen mit Rückwand in Weiß-Blau. */
    private static void podium(Plan p) {
        int cx = Places.PODIUM[0];
        int cz = Places.PODIUM[1];
        int[][] steps = {{cx - 1, 3, 1}, {cx - 5, 2, 2}, {cx + 3, 1, 3}};
        String[] carpets = {"yellow_carpet", "light_gray_carpet", "orange_carpet"};
        for (int[] s : steps) {
            for (int x = s[0]; x <= s[0] + 2; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    for (int y = 1; y <= s[1]; y++) {
                        p.set(x, y, z, y == s[1] ? "smooth_quartz" : "quartz_bricks");
                    }
                    p.set(x, s[1] + 1, z, carpets[s[2] - 1]);
                }
            }
        }
        for (int x = cx - 7; x <= cx + 7; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                p.set(x, 0, z, "polished_andesite");
            }
            for (int y = 1; y <= 7; y++) {
                boolean frame = Math.abs(x - cx) == 7 || y == 7;
                p.set(x, y, cz - 3, frame ? "stripped_spruce_log[axis=y]"
                        : Decor.lozenge(x, y, 2) ? "light_blue_concrete" : "white_concrete");
            }
            p.set(x, 8, cz - 3, "spruce_slab[type=bottom]");
            p.set(x, 9, cz - 3, "snow[layers=2]");
        }
        p.banner(cx - 4, 6, cz - 2, "green_wall_banner[facing=south]", Decor.emerald());
        p.banner(cx + 4, 6, cz - 2, "yellow_wall_banner[facing=south]", Decor.gold());
        p.set(cx - 7, 8, cz - 3, "lantern[hanging=false]");
        p.set(cx + 7, 8, cz - 3, "lantern[hanging=false]");
    }

    /** Holzrahmen für die beiden Tafeln (Medaillenspiegel und Bestzeiten). */
    private static void boards(Plan p) {
        for (double[] b : new double[][] {Places.BOARD_MEDALS, Places.BOARD_TIMES}) {
            int bx = (int) Math.floor(b[0]);
            int bz = (int) Math.floor(b[2]) - 2;
            for (int x = bx - 4; x <= bx + 4; x++) {
                for (int y = 1; y <= 7; y++) {
                    boolean frame = Math.abs(x - bx) == 4 || y == 7 || y == 1;
                    p.set(x, y, bz, frame ? "spruce_log[axis=y]" : "dark_oak_planks");
                }
                p.set(x, 8, bz, "spruce_slab[type=bottom]");
                p.set(x, 9, bz, "snow[layers=2]");
            }
        }
    }

    /** Glühweinstand: Bude mit rot-weißem Dach, Theke, Kessel, Fässer, Laternen. */
    private static void mulledWine(Plan p) {
        int cx = Places.MULLED[0];
        int cz = Places.MULLED[1];
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                p.set(x, 0, z, "spruce_planks");
                boolean post = Math.abs(x - cx) == 3 && Math.abs(z - cz) == 2;
                if (post) {
                    for (int y = 1; y <= 3; y++) {
                        p.set(x, y, z, "spruce_log[axis=y]");
                    }
                }
                if (z == cz + 2 && !post) {
                    p.set(x, 1, z, "spruce_planks");
                    p.set(x, 2, z, "spruce_slab[type=bottom]");
                }
                if (z == cz - 2 && !post) {
                    for (int y = 1; y <= 3; y++) {
                        p.set(x, y, z, "spruce_planks");
                    }
                }
            }
            // Dach: rot-weiß gestreift, nach vorn geneigt
            for (int z = cz - 3; z <= cz + 3; z++) {
                int y = 4 + (z < cz ? 1 : 0);
                p.set(x, y, z, Math.floorMod(x, 2) == 0 ? "red_wool" : "white_wool");
                p.set(x, y + 1, z, "snow[layers=1]");
            }
        }
        p.set(cx - 1, 1, cz, "campfire[lit=true,signal_fire=false,facing=south]");
        p.set(cx - 1, 2, cz, "water_cauldron[level=3]");
        p.set(cx + 1, 1, cz - 1, "barrel[facing=up,open=false]");
        p.set(cx + 2, 1, cz - 1, "barrel[facing=up,open=false]");
        p.set(cx + 2, 2, cz - 1, "barrel[facing=up,open=false]");
        p.set(cx - 2, 3, cz + 2, "flower_pot");
        p.set(cx, 3, cz + 2, "decorated_pot[facing=south]");
        p.set(cx + 2, 3, cz + 2, "flower_pot");
        Decor.hangingLantern(p, cx - 2, 3, cz + 1, 0);
        Decor.hangingLantern(p, cx + 2, 3, cz + 1, 0);
        // Stehtische davor
        for (int[] t : new int[][] {{cx - 3, cz + 5}, {cx + 3, cz + 5}}) {
            p.set(t[0], 1, t[1], "spruce_fence");
            p.set(t[0], 2, t[1], "spruce_pressure_plate");
        }
    }

    /** Eisfläche: zugefrorener Weiher mit Schneerand, Bänken und Lichterketten. */
    private static void rink(Plan p, Random r) {
        for (int x = Places.RINK_X1 - 2; x <= Places.RINK_X2 + 2; x++) {
            for (int z = Places.RINK_Z1 - 2; z <= Places.RINK_Z2 + 2; z++) {
                boolean inside = x >= Places.RINK_X1 && x <= Places.RINK_X2 && z >= Places.RINK_Z1 && z <= Places.RINK_Z2;
                double cornerD = Math.hypot(Math.max(0, Math.max(Places.RINK_X1 + 3 - x, x - Places.RINK_X2 + 3)),
                        Math.max(0, Math.max(Places.RINK_Z1 + 3 - z, z - Places.RINK_Z2 + 3)));
                if (inside && cornerD <= 3.2) {
                    p.set(x, 0, z, (x + z) % 7 == 0 ? "packed_ice" : "ice");
                    p.set(x, -1, z, "packed_ice");
                    p.air(x, 1, z);
                } else if (cornerD <= 5.2) {
                    p.set(x, 0, z, "snow_block");
                    p.set(x, 1, z, "snow[layers=" + (2 + r.nextInt(2)) + "]");
                }
            }
        }
        // Eingänge (kein Schneewall)
        for (int z = 19; z <= 22; z++) {
            p.air(Places.RINK_X2 + 1, 1, z);
            p.air(Places.RINK_X2 + 2, 1, z);
        }
        int[][] posts = {{Places.RINK_X1 - 2, Places.RINK_Z1 - 2}, {Places.RINK_X2 + 2, Places.RINK_Z1 - 2},
            {Places.RINK_X1 - 2, Places.RINK_Z2 + 2}, {Places.RINK_X2 + 2, Places.RINK_Z2 + 2}};
        for (int[] q : posts) {
            for (int y = 1; y <= 5; y++) {
                p.set(q[0], y, q[1], "spruce_fence");
            }
            p.set(q[0], 6, q[1], "lantern[hanging=false]");
        }
        // Lichterkette an der Nord- und Südseite
        for (int x = Places.RINK_X1 - 1; x <= Places.RINK_X2 + 1; x++) {
            for (int zz : new int[] {Places.RINK_Z1 - 2, Places.RINK_Z2 + 2}) {
                if (x % 4 == 0) {
                    p.set(x, 5, zz, "iron_chain[axis=x]");
                    p.set(x, 4, zz, Math.floorMod(x, 8) == 0 ? "ochre_froglight" : "pearlescent_froglight");
                } else {
                    p.set(x, 5, zz, "iron_chain[axis=x]");
                }
            }
        }
        Decor.bench(p, Places.RINK_X1, Places.RINK_Z2 + 4, Places.RINK_X1 + 5, Places.RINK_Z2 + 4, 1, "south");
        Decor.bench(p, Places.RINK_X2 - 5, Places.RINK_Z1 - 4, Places.RINK_X2, Places.RINK_Z1 - 4, 1, "north");
    }
}
