package de.moinmorni.wintersport.world;

import de.moinmorni.wintersport.course.Lift;
import de.moinmorni.wintersport.course.Slalom;
import de.moinmorni.wintersport.course.Terrain;
import de.moinmorni.wintersport.logic.Gate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Was nicht aus Blöcken besteht: die Torstangen mit Flaggen im Riesenslalom (Boote fahren hindurch, nichts bleibt
 * hängen) und die Seile des Sessellifts. Nicht dauerhaft gespeichert – sie entstehen bei jedem Start neu, die Chunks
 * dafür bleiben geladen.
 */
public final class Props {

    /** So weit sieht man Anzeige-Objekte (Vielfaches von 64 Blöcken). */
    private static final float VIEW = 4f;
    private static final float POLE = 0.11f;
    private static final float POLE_HEIGHT = 2.1f;
    private static final float FLAG_WIDTH = 0.8f;
    private static final float FLAG_HEIGHT = 0.55f;
    private static final double CABLE_SEGMENT = 16;
    private static final float CABLE = 0.07f;

    private final Plugin plugin;
    private final World world;
    private final List<Display> all = new ArrayList<>();

    public Props(Plugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    /** Chunks, die für Stangen, Seile und Schrift geladen bleiben müssen. */
    public static Set<Long> chunks() {
        Set<Long> out = new HashSet<>();
        for (int lane = 0; lane < Slalom.lanes(); lane++) {
            for (Gate g : Slalom.gates(lane)) {
                double[] p = g.poles();
                out.add(key((int) Math.floor(p[0]) >> 4, (int) Math.floor(p[1]) >> 4));
                out.add(key((int) Math.floor(p[2]) >> 4, (int) Math.floor(p[3]) >> 4));
            }
        }
        for (int z = (int) Lift.TOP_Z - 8; z <= Lift.VALLEY_Z + 8; z += 8) {
            out.add(key((int) Math.floor(Lift.UP_X) >> 4, z >> 4));
            out.add(key((int) Math.floor(Lift.DOWN_X) >> 4, z >> 4));
        }
        return out;
    }

    public static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    public void spawnAll() {
        removeAll();
        for (int lane = 0; lane < Slalom.lanes(); lane++) {
            for (Gate g : Slalom.gates(lane)) {
                gate(g);
            }
        }
        cable(Lift.UP_X);
        cable(Lift.DOWN_X);
    }

    /** Fehlt etwas (z. B. von außen gelöscht)? Dann alles neu. */
    public void check() {
        if (all.stream().anyMatch(d -> !d.isValid())) {
            spawnAll();
        }
    }

    public void removeAll() {
        all.forEach(Display::remove);
        all.clear();
    }

    /** Tor: zwei Flaggen außen an der Durchfahrt, jede zwischen zwei dünnen Stangen. */
    private void gate(Gate g) {
        double[] p = g.poles();
        double y = Terrain.standY(p[1]);
        Material colour = g.red() ? Material.RED_CONCRETE : Material.BLUE_CONCRETE;
        Material cloth = g.red() ? Material.RED_WOOL : Material.BLUE_WOOL;
        double a = Math.min(p[0], p[2]);
        double b = Math.max(p[0], p[2]);
        double z = p[1];
        for (double x : new double[] {a - FLAG_WIDTH, a, b, b + FLAG_WIDTH}) {
            all.add(box(new Location(world, x, y, z), colour, -POLE / 2, 0, -POLE / 2, POLE, POLE_HEIGHT, POLE));
        }
        float top = POLE_HEIGHT - 0.15f - FLAG_HEIGHT;
        all.add(box(new Location(world, a - FLAG_WIDTH, y, z), cloth, 0, top, -0.02f, FLAG_WIDTH, FLAG_HEIGHT, 0.04f));
        all.add(box(new Location(world, b, y, z), cloth, 0, top, -0.02f, FLAG_WIDTH, FLAG_HEIGHT, 0.04f));
    }

    private BlockDisplay box(Location at, Material m, float ox, float oy, float oz, float sx, float sy, float sz) {
        return world.spawn(at, BlockDisplay.class, d -> {
            d.setBlock(m.createBlockData());
            d.setPersistent(false);
            d.setViewRange(VIEW);
            d.setTransformation(new Transformation(new Vector3f(ox, oy, oz), new AxisAngle4f(),
                    new Vector3f(sx, sy, sz), new AxisAngle4f()));
        });
    }

    /** Seil von der Berg- zur Talstation in geraden Stücken (dünner, geneigter Block). */
    private void cable(double x) {
        double z0 = Lift.TOP_Z;
        double z1 = Lift.VALLEY_Z;
        int n = (int) Math.ceil((z1 - z0) / CABLE_SEGMENT);
        double step = (z1 - z0) / n;
        for (int i = 0; i < n; i++) {
            double za = z0 + i * step;
            double zb = za + step;
            double ya = Lift.cableY(za);
            double yb = Lift.cableY(zb);
            float len = (float) Math.hypot(zb - za, yb - ya);
            float angle = (float) Math.atan2(ya - yb, zb - za);
            Location at = new Location(world, x, Terrain.FLOOR_Y + ya, za);
            all.add(world.spawn(at, BlockDisplay.class, d -> {
                d.setBlock(Material.GRAY_CONCRETE.createBlockData());
                d.setPersistent(false);
                d.setViewRange(VIEW);
                d.setTransformation(new Transformation(new Vector3f(-CABLE / 2, -CABLE / 2, 0),
                        new AxisAngle4f(angle, 1, 0, 0), new Vector3f(CABLE, CABLE, len), new AxisAngle4f()));
            }));
        }
    }

    Plugin plugin() {
        return plugin;
    }
}
