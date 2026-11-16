package de.moinmorni.wintersport.world;

import de.moinmorni.wintersport.course.Places;
import de.moinmorni.wintersport.course.Slalom;
import de.moinmorni.wintersport.course.Terrain;
import java.util.Random;

/**
 * Riesenslalom und Rodelbahn: Eispisten aus Packeis mit Schneewällen, oben Starthäuser mit Starttor, unten Zielbogen,
 * Auslauf, Fangzaun und die Tribüne. Die Torstangen sind Anzeige-Objekte (siehe Props) – Boote fahren nicht dagegen.
 */
final class Pistes {

    private static final int LIGHT_EVERY = 16;

    private Pistes() {
    }

    static Plan slalom() {
        Plan p = new Plan();
        Random r = new Random(4242);
        for (int lane = 0; lane < Slalom.lanes(); lane++) {
            int lx = Slalom.LANE_X[lane];
            lane(p, lx, Slalom.HALF, Slalom.TOP_Z, Slalom.RUNOUT_Z, Slalom.FENCE_Z, lane % 2 == 0, r);
            startHouse(p, lx, Slalom.HALF, Slalom.GATE_ROW_Z, true);
            finishArch(p, lx, Slalom.HALF, (int) Math.floor(Slalom.FINISH_Z));
        }
        grandstand(p, Places.STAND_SLALOM[0], Places.STAND_SLALOM[1], 34);
        fence(p, Slalom.LANE_X[0] - 8, Slalom.LANE_X[Slalom.lanes() - 1] + 8, Slalom.FENCE_Z);
        return p;
    }

    static Plan rodel() {
        Plan p = new Plan();
        Random r = new Random(777);
        int lx = Places.RODEL_X;
        lane(p, lx, Places.RODEL_HALF, Places.RODEL_START_Z, Places.RODEL_RUNOUT_Z, Places.RODEL_RUNOUT_Z + 7, false, r);
        // Oben: Schnee-Plattform vor der Bahn, kleine Hütte „Rodelverleih“ daneben.
        int top = Terrain.TOP;
        for (int x = lx - Places.RODEL_HALF; x <= lx + Places.RODEL_HALF; x++) {
            for (int z = Places.RODEL_START_Z - 6; z < Places.RODEL_START_Z; z++) {
                p.set(x, top, z, "snow_block");
                p.air(x, top + 1, z);
            }
            p.set(x, top, Places.RODEL_START_Z, "red_concrete");
        }
        // Vordach über dem Start mit Laternen
        for (int x = lx - Places.RODEL_HALF - 1; x <= lx + Places.RODEL_HALF + 1; x++) {
            for (int z = Places.RODEL_START_Z - 5; z <= Places.RODEL_START_Z - 1; z++) {
                boolean post = (x == lx - Places.RODEL_HALF - 1 || x == lx + Places.RODEL_HALF + 1)
                        && (z == Places.RODEL_START_Z - 5 || z == Places.RODEL_START_Z - 1);
                if (post) {
                    for (int y = top + 1; y <= top + 4; y++) {
                        p.set(x, y, z, "spruce_log[axis=y]");
                    }
                }
                p.set(x, top + 5, z, "spruce_slab[type=bottom]");
                p.set(x, top + 6, z, "snow[layers=2]");
            }
        }
        Decor.hangingLantern(p, lx, top + 4, Places.RODEL_START_Z - 3, 0);
        p.banner(lx, top + 4, Places.RODEL_START_Z - 1, "red_banner[rotation=0]", Decor.redWhite());
        for (int z = Places.RODEL_START_Z; z <= Places.RODEL_START_Z + 2; z++) {
            p.set(lx - 4, top, z, "red_concrete");
            p.set(lx + 4, top, z, "red_concrete");
        }
        finishArch(p, lx, Places.RODEL_HALF, Places.RODEL_FINISH_Z);
        fence(p, lx - 6, lx + 6, Places.RODEL_RUNOUT_Z + 7);
        return p;
    }

    /** Eine Piste: Eis, Schneewälle, Laternen, Auslauf, Strohballen. */
    private static void lane(Plan p, int lx, int half, int topZ, int runoutZ, int fenceZ, boolean lightsWest, Random r) {
        for (int z = topZ; z < fenceZ; z++) {
            int h = Terrain.height(z);
            boolean ice = z <= runoutZ;
            for (int x = lx - half; x <= lx + half; x++) {
                p.set(x, h, z, ice ? "packed_ice" : "snow_block");
                p.air(x, h + 1, z);
                p.air(x, h + 2, z);
            }
            for (int side : new int[] {-1, 1}) {
                int bx = lx + side * (half + 1);
                p.set(bx, h, z, "snow_block");
                p.set(bx, h + 1, z, "snow_block");
                p.set(bx, h + 2, z, "snow[layers=" + (2 + Math.floorMod(z * 7 + bx, 3)) + "]");
            }
            if (Math.floorMod(z, LIGHT_EVERY) == 0 && z > topZ + 10) {
                int side = (Math.floorDiv(z, LIGHT_EVERY) % 2 == 0) == lightsWest ? -1 : 1;
                Decor.lanternPost(p, lx + side * (half + 2), h + 1, z);
            }
            // Pistenmarkierung: rote und blaue Punkte im Schneewall (wie Pistenstangen)
            if (Math.floorMod(z, 8) == 4) {
                p.set(lx - half - 1, h + 1, z, "red_concrete");
                p.set(lx + half + 1, h + 1, z, "blue_concrete");
            }
        }
        for (int x = lx - half; x <= lx + half; x++) {
            p.set(x, 1, fenceZ - 1, "hay_block[axis=x]");
        }
    }

    /** Starthaus: Holzhütte über dem Pistenanfang, vorn das Starttor aus Zauntoren unter einem Balken mit Bannern. */
    private static void startHouse(Plan p, int lx, int half, int gateZ, boolean timing) {
        int y0 = Terrain.TOP;
        int back = gateZ - 10;
        int w = half + 2;
        for (int z = back; z <= gateZ; z++) {
            for (int dy = 1; dy <= 5; dy++) {
                p.set(lx - w, y0 + dy, z, dy == 1 ? "stone_bricks" : z == gateZ || z == back ? "spruce_log[axis=y]" : "spruce_planks");
                p.set(lx + w, y0 + dy, z, dy == 1 ? "stone_bricks" : z == gateZ || z == back ? "spruce_log[axis=y]" : "spruce_planks");
            }
            if ((z - back) % 3 == 1) {
                p.set(lx - w, y0 + 3, z, "glass_pane");
                p.set(lx + w, y0 + 3, z, "glass_pane");
            }
        }
        for (int x = lx - w; x <= lx + w; x++) {
            for (int dy = 1; dy <= 5; dy++) {
                p.set(x, y0 + dy, back, x == lx - w || x == lx + w ? "spruce_log[axis=y]" : "spruce_planks");
            }
            p.set(x, y0, back, "stone_bricks");
        }
        p.set(lx, y0 + 3, back, "glass_pane");
        // Dach (First in z-Richtung) mit Schnee
        for (int i = 0; i <= w + 1; i++) {
            int left = lx - w - 1 + i;
            int right = lx + w + 1 - i;
            int ry = y0 + 6 + i;
            if (left > right) {
                break;
            }
            for (int z = back - 1; z <= gateZ + 1; z++) {
                if (left == right) {
                    p.set(left, ry, z, "snow_block");
                    continue;
                }
                String stairs = z == back - 1 || z == gateZ + 1 ? "spruce_stairs" : "smooth_quartz_stairs";
                p.set(left, ry, z, stairs + "[facing=east,half=bottom,shape=straight]");
                p.set(right, ry, z, stairs + "[facing=west,half=bottom,shape=straight]");
                if (z == back) {
                    for (int x = left + 1; x < right; x++) {
                        p.set(x, ry, z, "spruce_planks");
                    }
                }
            }
        }
        // Innen: Decke, Licht
        for (int x = lx - w + 1; x < lx + w; x++) {
            for (int z = back + 1; z <= gateZ; z++) {
                p.set(x, y0 + 6, z, "spruce_slab[type=top]");
            }
        }
        Decor.hangingLantern(p, lx, y0 + 5, gateZ - 4, 1);
        Decor.hangingLantern(p, lx, y0 + 5, back + 2, 1);
        // Starttor: Zauntore quer über die Bahn, darüber ein Balken mit Bannern
        for (int x = lx - half; x <= lx + half; x++) {
            p.set(x, y0 + 1, gateZ, "spruce_fence_gate[facing=north,open=false,in_wall=false]");
            p.air(x, y0 + 2, gateZ);
        }
        for (int x = lx - w; x <= lx + w; x++) {
            p.set(x, y0 + 5, gateZ, "stripped_spruce_log[axis=x]");
        }
        p.banner(lx - 2, y0 + 4, gateZ + 1, "white_wall_banner[facing=south]", Decor.redWhite());
        p.banner(lx + 2, y0 + 4, gateZ + 1, "white_wall_banner[facing=south]", Decor.redWhite());
        p.banner(lx, y0 + 4, gateZ + 1, "light_blue_wall_banner[facing=south]", Decor.snowflake());
    }

    /** Zielbogen: zwei Pfosten, Querbalken, Banner in Gold. */
    static void finishArch(Plan p, int lx, int half, int z) {
        int h = Terrain.height(z);
        int w = half + 2;
        for (int dy = 1; dy <= 6; dy++) {
            p.set(lx - w, h + dy, z, "stripped_spruce_log[axis=y]");
            p.set(lx + w, h + dy, z, "stripped_spruce_log[axis=y]");
        }
        for (int x = lx - w; x <= lx + w; x++) {
            p.set(x, h + 7, z, "spruce_planks");
            p.set(x, h + 8, z, x % 2 == 0 ? "smooth_quartz_slab[type=bottom]" : "snow[layers=2]");
        }
        for (int x = lx - half + 1; x <= lx + half - 1; x += 2) {
            p.banner(x, h + 6, z, "yellow_banner[rotation=0]", Decor.gold());
        }
        p.set(lx - w, h + 8, z, "lantern[hanging=false]");
        p.set(lx + w, h + 8, z, "lantern[hanging=false]");
        // Zielstrich über das Eis
        for (int x = lx - half; x <= lx + half; x++) {
            p.set(x, h, z, Math.floorMod(x, 2) == 0 ? "black_concrete" : "white_concrete");
        }
    }

    /** Fangzaun am Ende des Auslaufs. */
    static void fence(Plan p, int x1, int x2, int z) {
        for (int x = x1; x <= x2; x++) {
            p.set(x, 1, z, "spruce_fence");
            p.set(x, 2, z, Math.floorMod(x, 2) == 0 ? "red_wool" : "white_wool");
        }
    }

    /**
     * Zuschauertribüne: vier Stufen mit Sitzreihen (Blick nach Norden), Rückwand mit Dach, Banner, Laternen. Mitte
     * {@code cx}, vorderste Reihe {@code z0}, Breite {@code width}.
     */
    static void grandstand(Plan p, int cx, int z0, int width) {
        int x1 = cx - width / 2;
        int x2 = cx + width / 2;
        for (int k = 0; k < 4; k++) {
            int seatZ = z0 + 2 * k;
            for (int x = x1; x <= x2; x++) {
                for (int y = 1; y <= k; y++) {
                    p.set(x, y, seatZ, "spruce_planks");
                    p.set(x, y, seatZ + 1, "spruce_planks");
                }
                boolean aisle = x == cx || x == x1 || x == x2;
                p.set(x, k + 1, seatZ, aisle ? "spruce_slab[type=bottom]"
                        : "spruce_stairs[facing=south,half=bottom,shape=straight]");
                p.set(x, k + 1, seatZ + 1, "spruce_planks");
            }
        }
        int backZ = z0 + 8;
        for (int x = x1; x <= x2; x++) {
            for (int y = 1; y <= 8; y++) {
                p.set(x, y, backZ, (x - x1) % 6 == 0 ? "spruce_log[axis=y]" : y <= 4 ? "spruce_planks" : "white_terracotta");
            }
            // Dach nach vorn geneigt
            for (int dz = 0; dz <= 5; dz++) {
                int y = 9 - dz / 2;
                p.set(x, y, backZ - dz, dz % 2 == 0 ? "smooth_quartz_slab[type=top]" : "smooth_quartz_slab[type=bottom]");
            }
        }
        for (int x = x1; x <= x2; x += 6) {
            for (int y = 4; y <= 7; y++) {
                p.set(x, y, backZ - 5, "spruce_fence");
            }
            Decor.hangingLantern(p, x + 3, 7, backZ - 3, 1);
        }
        for (int x = x1 + 3; x <= x2 - 3; x += 6) {
            p.banner(x, 6, backZ - 1, "light_blue_wall_banner[facing=north]", Decor.lozenges());
        }
        // Treppe hinauf an beiden Seiten
        for (int k = 0; k < 4; k++) {
            p.set(x1 - 1, k + 1, z0 + 2 * k, "spruce_stairs[facing=south,half=bottom,shape=straight]");
            p.set(x2 + 1, k + 1, z0 + 2 * k, "spruce_stairs[facing=south,half=bottom,shape=straight]");
            for (int y = 1; y <= k; y++) {
                p.set(x1 - 1, y, z0 + 2 * k, "spruce_planks");
                p.set(x2 + 1, y, z0 + 2 * k, "spruce_planks");
            }
        }
    }
}
