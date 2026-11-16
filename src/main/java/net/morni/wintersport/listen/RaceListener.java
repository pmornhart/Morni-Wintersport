package net.morni.wintersport.listen;

import net.morni.wintersport.WintersportPlugin;
import net.morni.wintersport.game.BiathlonRace;
import net.morni.wintersport.game.BoatRace;
import net.morni.wintersport.game.Competition;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

/** Boote (aussteigen = zurück auf die Strecke), Sessellift (nur oben aussteigen) und Pfeile am Schießstand. */
public final class RaceListener implements Listener {

    private final WintersportPlugin plugin;

    public RaceListener(WintersportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExit(VehicleExitEvent e) {
        if (!(e.getExited() instanceof Player p)) {
            return;
        }
        Competition c = plugin.games().playing(p.getUniqueId());
        if (c != null && c.stage() instanceof BoatRace race) {
            race.exited(p);
        }
    }

    /** Aus dem Sessel steigt man erst oben aus. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent e) {
        if (e.getEntity() instanceof Player p && e.getDismounted().getScoreboardTags().contains("wintersport_lift")
                && e.getDismounted().isValid() && plugin.lift().holds(p) && p.isOnline()) {
            e.setCancelled(true);
        }
    }

    /** Boote im Wettkampf schieben sich nicht gegenseitig (jedes hat seine eigene Bahn). */
    @EventHandler(ignoreCancelled = true)
    public void onCollide(VehicleEntityCollisionEvent e) {
        if (e.getVehicle().getScoreboardTags().contains("wintersport_boat")
                && e.getEntity().getScoreboardTags().contains("wintersport_boat")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p) || !(e.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        Competition c = plugin.games().playing(p.getUniqueId());
        if (c != null && c.stage() instanceof BiathlonRace race && race.shot(p, arrow)) {
            return;
        }
        if (p.getWorld() == plugin.world() && !plugin.builder(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        Competition c = plugin.games().current();
        if (c != null && c.stage() instanceof BiathlonRace race && race.isOurs(arrow)) {
            race.hit(arrow, e.getHitBlock());
            e.setCancelled(true);
        }
    }
}
