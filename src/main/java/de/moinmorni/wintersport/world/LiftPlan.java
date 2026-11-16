package de.moinmorni.wintersport.world;

import de.moinmorni.wintersport.course.Lift;
import de.moinmorni.wintersport.course.Places;
import de.moinmorni.wintersport.course.Terrain;

/** Tal- und Bergstation des Sessellifts (Holzhaus mit Seilscheibe) und die Stützen dazwischen. */
final class LiftPlan {

    private LiftPlan() {
    }

    static Plan build() {
        Plan p = new Plan();
        station(p, Places.LIFT_VALLEY_Z, Terrain.height(Places.LIFT_VALLEY_Z), true);
        station(p, Places.LIFT_TOP_Z, Terrain.height(Places.LIFT_TOP_Z), false);
        int lx = Places.LIFT_X;
        for (int z : Lift.pylons()) {
            int h = Terrain.height(z);
            int top = (int) Math.floor(Lift.cableY(z + 0.5)) + 1;
            p.set(lx, h, z, "stone_bricks");
            for (int y = h + 1; y < top; y++) {
                p.set(lx, y, z, "polished_andesite");
            }
            for (int x = lx - 3; x <= lx + 3; x++) {
                p.set(x, top, z, "smooth_stone_slab[type=bottom]");
            }
            p.set(lx, top, z, "polished_andesite");
            p.set(lx, top + 1, z, "lantern[hanging=false]");
            // Schnee auf dem Sockel
            p.set(lx + 1, h + 1, z, "snow[layers=3]");
            p.set(lx - 1, h + 1, z, "snow[layers=2]");
        }
        return p;
    }

    /** Station: Plattform, Holzpfosten, Dach, Seilscheibe (Ring) auf Seilhöhe. */
    private static void station(Plan p, int z0, int h, boolean valley) {
        int lx = Places.LIFT_X;
        int dir = valley ? 1 : -1;
        int zFront = z0 - dir * 4;
        int zBack = z0 + dir * 4;
        int cable = h + Places.LIFT_CABLE;
        for (int x = lx - 6; x <= lx + 6; x++) {
            for (int z = Math.min(zFront, zBack); z <= Math.max(zFront, zBack); z++) {
                p.set(x, h, z, Math.abs(x - lx) == 6 || z == zBack ? "stone_bricks" : "spruce_planks");
                for (int y = h + 1; y <= cable + 3; y++) {
                    p.air(x, y, z);
                }
            }
        }
        // Pfosten und Dach
        for (int x : new int[] {lx - 6, lx + 6}) {
            for (int z : new int[] {zFront, zBack}) {
                for (int y = h + 1; y <= cable + 2; y++) {
                    p.set(x, y, z, "spruce_log[axis=y]");
                }
            }
        }
        for (int x = lx - 7; x <= lx + 7; x++) {
            for (int z = Math.min(zFront, zBack) - 1; z <= Math.max(zFront, zBack) + 1; z++) {
                p.set(x, cable + 3, z, Math.abs(x - lx) >= 6 ? "spruce_slab[type=bottom]" : "smooth_quartz_slab[type=bottom]");
                p.set(x, cable + 4, z, "snow[layers=2]");
            }
        }
        // Seilscheibe: Ring aus Stein auf Seilhöhe, Achse in der Mitte
        for (int x = lx - 3; x <= lx + 3; x++) {
            for (int z = zBack - 3 * dir; z != zBack + dir; z += dir) {
                double d = Math.hypot(x - lx, z - (zBack - dir));
                if (d > 1.6 && d < 3.2) {
                    p.set(x, cable, z, "smooth_stone_slab[type=bottom]");
                }
            }
        }
        for (int y = cable; y <= cable + 2; y++) {
            p.set(lx, y, zBack - dir, "polished_andesite");
        }
        // Einstieg (Tal) bzw. Ausstieg (Berg): rote Linie, Laternen, Bänke
        for (int z = Math.min(zFront, zBack) + 1; z < Math.max(zFront, zBack); z++) {
            p.set(lx - 2, h, z, "red_concrete");
        }
        Decor.hangingLantern(p, lx, cable + 2, z0, 1);
        Decor.bench(p, lx + 4, zBack - dir, lx + 4, zBack - 2 * dir, h + 1, "east");
        p.banner(lx - 6, cable, zFront - dir, valley ? "light_blue_wall_banner[facing=north]" : "light_blue_wall_banner[facing=south]",
                Decor.snowflake());
    }
}
