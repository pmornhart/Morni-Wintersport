package de.moinmorni.wintersport;

import de.moinmorni.wintersport.logic.Season;
import org.bukkit.configuration.file.FileConfiguration;

/** Werte aus der config.yml (Erklärungen stehen dort). */
public record Settings(
        String world,
        Season season,
        boolean evening,
        boolean snowfall,
        int villageCountdown,
        int minPlayers,
        int maxPlayers,
        int pauseSeconds,
        int ceremonySeconds,
        int slalomPenaltySeconds,
        int slalomLimit,
        boolean twoManBob,
        int bobLimit,
        int biathlonLaps,
        int biathlonShootings,
        int biathlonLimit,
        int shootSeconds,
        int runSpeed) {

    static Settings load(FileConfiguration c) {
        return new Settings(
                c.getString("welt", "wintersport"),
                Season.parse(c.getString("saison", "immer")),
                "abend".equalsIgnoreCase(c.getString("tageszeit", "tag").trim()),
                c.getBoolean("schneefall", true),
                clamp(c.getInt("dorf.countdown-sekunden", 30), 3, 300),
                clamp(c.getInt("dorf.mindestens-spieler", 2), 1, 12),
                clamp(c.getInt("wettkampf.hoechstens-spieler", 12), 1, 12),
                clamp(c.getInt("wettkampf.pause-sekunden", 8), 2, 120),
                clamp(c.getInt("wettkampf.siegerehrung-sekunden", 12), 3, 120),
                clamp(c.getInt("slalom.strafe-sekunden", 3), 0, 60),
                clamp(c.getInt("slalom.zeitlimit-sekunden", 90), 20, 600),
                c.getBoolean("bob.zweierbob", true),
                clamp(c.getInt("bob.zeitlimit-sekunden", 90), 20, 600),
                clamp(c.getInt("biathlon.runden", 3), 1, 10),
                clamp(c.getInt("biathlon.schiessen", 2), 0, 9),
                clamp(c.getInt("biathlon.zeitlimit-sekunden", 360), 30, 1800),
                clamp(c.getInt("biathlon.schiess-zeit-sekunden", 40), 10, 300),
                clamp(c.getInt("biathlon.tempo", 2), 0, 4));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
