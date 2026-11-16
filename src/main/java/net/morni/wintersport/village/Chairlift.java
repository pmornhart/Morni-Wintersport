package net.morni.wintersport.village;

import net.morni.wintersport.Texts;
import net.morni.wintersport.WintersportPlugin;
import net.morni.wintersport.course.Lift;
import net.morni.wintersport.course.Places;
import net.morni.wintersport.course.Terrain;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Der Sessellift: Sessel (Anzeige-Objekte) laufen ständig im Kreis – am westlichen Seil hinauf, am östlichen hinab.
 * Wer im Dorf auf der roten Linie der Talstation steht, wird vom nächsten freien Sessel mitgenommen (sitzt auf
 * einem unsichtbaren Sitz, der mitfährt) und steigt oben an der Bergstation von selbst aus.
 */
public final class Chairlift {

    /** Blöcke pro Tick (etwa 7 Blöcke pro Sekunde). */
    private static final double SPEED = 0.36;
    private static final double SPACING = 14;
    private static final int MOVE_EVERY = 2;
    /** Wie weit unter dem Sessel der Sitz (Rüstungsständer) ist, damit man auf dem Sessel sitzt. */
    private static final double RIDE_DROP = 0.35;
    private static final float VIEW = 3f;
    /** Bis hierhin (Strecke am Seil) darf man einsteigen. */
    private static final double BOARD_WINDOW = 1.2;

    private final class Chair {
        final double base;
        BlockDisplay seat;
        BlockDisplay hanger;
        ArmorStand ride;
        UUID rider;
        boolean up = true;

        Chair(double base) {
            this.base = base;
        }
    }

    private final WintersportPlugin plugin;
    private final List<Chair> chairs = new ArrayList<>();
    private double offset;
    private boolean running;

    public Chairlift(WintersportPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        double loop = 2 * Lift.length();
        int n = (int) Math.floor(loop / SPACING);
        for (int i = 0; i < n; i++) {
            Chair c = new Chair(i * loop / n);
            spawn(c);
            chairs.add(c);
        }
        running = true;
    }

    public void stop() {
        for (Chair c : chairs) {
            dismount(c, false);
            if (c.seat != null) {
                c.seat.remove();
            }
            if (c.hanger != null) {
                c.hanger.remove();
            }
        }
        chairs.clear();
        running = false;
    }

    private double pos(Chair c) {
        return c.base + offset;
    }

    private void spawn(Chair c) {
        double[] at = Lift.at(pos(c));
        c.up = at[3] > 0;
        Location seatLoc = seatLocation(at);
        c.seat = plugin.world().spawn(seatLoc, BlockDisplay.class, d -> {
            d.setBlock(Material.DARK_OAK_STAIRS.createBlockData(c.up ? "[facing=south]" : "[facing=north]"));
            d.setPersistent(false);
            d.setViewRange(VIEW);
            d.setTeleportDuration(MOVE_EVERY);
            d.setTransformation(new Transformation(new Vector3f(-0.55f, -0.1f, -0.45f), new AxisAngle4f(),
                    new Vector3f(1.1f, 0.75f, 0.9f), new AxisAngle4f()));
        });
        c.hanger = plugin.world().spawn(seatLoc, BlockDisplay.class, d -> {
            d.setBlock(Material.IRON_BLOCK.createBlockData());
            d.setPersistent(false);
            d.setViewRange(VIEW);
            d.setTeleportDuration(MOVE_EVERY);
            d.setTransformation(new Transformation(new Vector3f(-0.04f, 0.5f, -0.04f), new AxisAngle4f(),
                    new Vector3f(0.08f, (float) Lift.HANG - 0.4f, 0.08f), new AxisAngle4f()));
        });
    }

    private Location seatLocation(double[] at) {
        return new Location(plugin.world(), at[0], Terrain.FLOOR_Y + at[1] - Lift.HANG, at[2], at[3] > 0 ? 180f : 0f, 0f);
    }

    public void tick(long tick) {
        if (!running) {
            return;
        }
        offset += SPEED;
        double len = Lift.length();
        for (Chair c : chairs) {
            double[] at = Lift.at(pos(c));
            boolean up = at[3] > 0;
            if (up != c.up || c.seat == null || !c.seat.isValid() || c.hanger == null || !c.hanger.isValid()) {
                // Am Wendepunkt (oder verloren gegangen): neu aufstellen, damit der Sessel in die andere Richtung schaut.
                dismount(c, up != c.up && c.rider != null);
                if (c.seat != null) {
                    c.seat.remove();
                }
                if (c.hanger != null) {
                    c.hanger.remove();
                }
                spawn(c);
                continue;
            }
            Location l = seatLocation(at);
            if (tick % MOVE_EVERY == 0) {
                c.seat.teleport(l);
                c.hanger.teleport(l);
            }
            if (c.ride != null) {
                Player rider = c.rider == null ? null : Bukkit.getPlayer(c.rider);
                double u = Math.floorMod((long) (pos(c) * 1000), (long) (2 * len * 1000)) / 1000.0;
                if (rider == null || !c.ride.isValid() || !c.ride.getPassengers().contains(rider) || u > len - 3) {
                    dismount(c, rider != null);
                } else {
                    c.ride.teleport(l.clone().add(0, -RIDE_DROP, 0), io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS);
                }
            }
        }
        if (tick % 4 == 0) {
            board();
        }
    }

    /** Wartende auf der roten Linie in den nächsten freien Sessel setzen. */
    private void board() {
        double len = Lift.length();
        Chair free = null;
        for (Chair c : chairs) {
            double u = Math.floorMod((long) (pos(c) * 1000), (long) (2 * len * 1000)) / 1000.0;
            if (u < BOARD_WINDOW && c.rider == null) {
                free = c;
            }
        }
        if (free == null) {
            return;
        }
        for (UUID u : plugin.village().waiting()) {
            Player p = Bukkit.getPlayer(u);
            if (p == null || p.getVehicle() != null || p.getWorld() != plugin.world()) {
                continue;
            }
            Location l = p.getLocation();
            boolean onLine = l.getBlockX() >= Places.LIFT_X - 3 && l.getBlockX() <= Places.LIFT_X - 1
                    && l.getBlockZ() >= Places.LIFT_VALLEY_Z - 3 && l.getBlockZ() <= Places.LIFT_VALLEY_Z + 3
                    && l.getY() < Terrain.FLOOR_Y + 3;
            if (onLine) {
                seat(free, p);
                return;
            }
        }
    }

    private void seat(Chair c, Player p) {
        Location at = seatLocation(Lift.at(pos(c))).add(0, -RIDE_DROP, 0);
        c.ride = plugin.world().spawn(at, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setMarker(true);
            a.setGravity(false);
            a.setInvulnerable(true);
            a.setPersistent(false);
            a.setSilent(true);
            a.addScoreboardTag("wintersport_lift");
        });
        c.rider = p.getUniqueId();
        c.ride.addPassenger(p);
        p.sendMessage(Texts.msg(plugin.lang(p), "lift.board"));
    }

    /** Fahrgast absetzen; {@code atTop}: oben auf der Bergstation abstellen. */
    private void dismount(Chair c, boolean atTop) {
        Player rider = c.rider == null ? null : Bukkit.getPlayer(c.rider);
        c.rider = null;
        if (c.ride != null) {
            c.ride.eject();
            c.ride.remove();
            c.ride = null;
        }
        if (rider != null && atTop) {
            rider.teleport(new Location(plugin.world(), Places.LIFT_X + 0.5, Terrain.FLOOR_Y + Terrain.TOP + 1,
                    Places.LIFT_TOP_Z + 6.5, 180f, 0f));
            rider.sendMessage(Texts.msg(plugin.lang(rider), "lift.top"));
        }
    }

    /** Sitzt jemand im Lift? */
    public boolean riding(Player p) {
        return chairs.stream().anyMatch(c -> p.getUniqueId().equals(c.rider));
    }

    /** Aus dem Lift holen (z. B. weil der Wettkampf startet). */
    public void unseat(Player p) {
        for (Chair c : chairs) {
            if (p.getUniqueId().equals(c.rider)) {
                dismount(c, false);
            }
        }
    }

    /** Darf der Fahrgast aussteigen? Nur oben (das macht der Lift selbst). */
    public boolean holds(Player p) {
        return riding(p);
    }
}
