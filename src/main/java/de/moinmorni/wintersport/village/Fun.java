package de.moinmorni.wintersport.village;

import de.moinmorni.wintersport.Texts;
import de.moinmorni.wintersport.WintersportPlugin;
import de.moinmorni.wintersport.course.Places;
import de.moinmorni.wintersport.course.Terrain;
import de.moinmorni.wintersport.logic.TimeFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.boat.BambooRaft;

/**
 * Spaß im Dorf, solange man wartet: die Rodelbahn vom Gipfel ins Tal. Wer oben aufs Eis tritt, sitzt sofort in einem
 * Schlitten (Bambusfloß); unten wird die Zeit angezeigt und der Schlitten verschwindet.
 */
public final class Fun {

    private static final int BOARD_EVERY = 5;
    private static final long COOLDOWN_MS = 3000;
    private static final int STOPPED_TICKS = 30;
    private static final double STOPPED_SPEED = 0.02;

    private static final class Sled {
        final Boat boat;
        final long startTick;
        boolean timed;
        int still;

        Sled(Boat boat, long startTick) {
            this.boat = boat;
            this.startTick = startTick;
        }
    }

    private final WintersportPlugin plugin;
    private final Map<UUID, Sled> sleds = new HashMap<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public Fun(WintersportPlugin plugin) {
        this.plugin = plugin;
    }

    /** Sitzt jemand im Schlitten? */
    public boolean busy(Player p) {
        return sleds.containsKey(p.getUniqueId());
    }

    public void tick(long tick) {
        Iterator<Map.Entry<UUID, Sled>> it = sleds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Sled> e = it.next();
            Sled s = e.getValue();
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null || !s.boat.isValid() || !s.boat.getPassengers().contains(p)) {
                s.boat.remove();
                cooldown.put(e.getKey(), System.currentTimeMillis());
                it.remove();
                continue;
            }
            Location l = s.boat.getLocation();
            if (!s.timed && l.getZ() > Places.RODEL_FINISH_Z) {
                s.timed = true;
                long ms = Math.round((tick - s.startTick) * 50.0);
                String lang = plugin.lang(p);
                p.sendActionBar(Texts.text(lang, "rodel.time", Placeholder.unparsed("time", TimeFormat.format(ms, lang))));
                p.sendMessage(Texts.msg(lang, "rodel.time", Placeholder.unparsed("time", TimeFormat.format(ms, lang))));
            }
            s.still = s.boat.getVelocity().length() < STOPPED_SPEED ? s.still + 1 : 0;
            if (s.timed && (s.still >= STOPPED_TICKS || l.getZ() > Places.RODEL_RUNOUT_Z + 3)) {
                end(e.getKey(), s);
                it.remove();
            }
        }
        if (tick % BOARD_EVERY == 0) {
            board(tick);
        }
    }

    private void end(UUID uuid, Sled s) {
        for (Entity e : s.boat.getPassengers()) {
            s.boat.removePassenger(e);
        }
        s.boat.remove();
        cooldown.put(uuid, System.currentTimeMillis());
    }

    private void board(long tick) {
        for (UUID u : plugin.village().waiting()) {
            Player p = Bukkit.getPlayer(u);
            if (p == null || p.getVehicle() != null || sleds.containsKey(u) || p.getWorld() != plugin.world()) {
                continue;
            }
            Long last = cooldown.get(u);
            if (last != null && System.currentTimeMillis() - last < COOLDOWN_MS) {
                continue;
            }
            Location l = p.getLocation();
            boolean onStart = Math.abs(l.getBlockX() - Places.RODEL_X) <= Places.RODEL_HALF
                    && l.getBlockZ() >= Places.RODEL_START_Z && l.getBlockZ() <= Places.RODEL_START_Z + 2
                    && Math.abs(l.getY() - Terrain.standY(l.getZ())) < 1.5;
            if (!onStart) {
                continue;
            }
            Location at = new Location(plugin.world(), l.getX(), Terrain.standY(Places.RODEL_START_Z + 1.5),
                    Places.RODEL_START_Z + 1.5, 0f, 0f);
            Boat sled = plugin.world().spawn(at, BambooRaft.class, b -> {
                b.setPersistent(false);
                b.addScoreboardTag("wintersport_sled");
            });
            p.teleport(at);
            sled.addPassenger(p);
            sleds.put(u, new Sled(sled, tick));
            p.sendMessage(Texts.msg(plugin.lang(p), "rodel.go"));
        }
    }

    /** Aus dem Schlitten holen (Wettkampf startet, Spieler geht). */
    public boolean leave(Player p) {
        Sled s = sleds.remove(p.getUniqueId());
        if (s == null) {
            return false;
        }
        end(p.getUniqueId(), s);
        return true;
    }

    public void stop() {
        sleds.values().forEach(s -> s.boat.remove());
        sleds.clear();
    }
}
