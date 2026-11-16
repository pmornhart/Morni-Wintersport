package net.morni.wintersport;

import org.bukkit.Particle;
import org.bukkit.entity.Player;

/** Leise rieselnder Schnee um jeden Spieler in der Winterwelt (nur Anzeige, jeder sieht seinen eigenen). */
final class Snow {

    private static final int EVERY = 4;
    private static final int FLAKES = 7;
    private static final double SPREAD = 14;

    private final WintersportPlugin plugin;
    private final java.util.Random random = new java.util.Random();

    Snow(WintersportPlugin plugin) {
        this.plugin = plugin;
    }

    void tick(long tick) {
        if (tick % EVERY != 0 || !plugin.settings().snowfall()) {
            return;
        }
        for (Player p : plugin.world().getPlayers()) {
            var l = p.getLocation();
            for (int i = 0; i < FLAKES; i++) {
                double x = l.getX() + (random.nextDouble() - 0.5) * SPREAD;
                double y = l.getY() + 3 + random.nextDouble() * 8;
                double z = l.getZ() + (random.nextDouble() - 0.5) * SPREAD;
                p.spawnParticle(Particle.SNOWFLAKE, x, y, z, 0, (random.nextDouble() - 0.5) * 0.05, -0.12, 0.03, 1);
            }
        }
    }
}
