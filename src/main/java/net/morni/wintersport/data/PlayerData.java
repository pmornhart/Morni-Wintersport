package net.morni.wintersport.data;

import net.morni.wintersport.logic.Discipline;
import net.morni.wintersport.logic.Medal;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Werte einer Person: Wettkämpfe, Rennen, Medaillen, Punkte, Gesamtsiege, Bestzeiten je Disziplin, Biathlon-Treffer. */
public final class PlayerData {

    private final UUID uuid;
    private String name;
    private int events;
    private int races;
    private int wins;
    private int gold;
    private int silver;
    private int bronze;
    private int points;
    private int hits;
    private int shots;
    private final Map<Discipline, Long> best = new EnumMap<>(Discipline.class);

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            name = n;
        }
    }

    public int events() {
        return events;
    }

    public int races() {
        return races;
    }

    public int wins() {
        return wins;
    }

    public int gold() {
        return gold;
    }

    public int silver() {
        return silver;
    }

    public int bronze() {
        return bronze;
    }

    public int points() {
        return points;
    }

    public int hits() {
        return hits;
    }

    public int shots() {
        return shots;
    }

    /** Bestzeit oder −1. */
    public long best(Discipline d) {
        return best.getOrDefault(d, -1L);
    }

    /**
     * Ein Rennen ist gewertet.
     *
     * @param timeMs Zeit oder −1 (nicht im Ziel)
     * @return true bei neuer Bestzeit
     */
    public boolean addRace(Discipline d, long timeMs, Medal medal, int pts) {
        races++;
        points += Math.max(0, pts);
        if (medal == Medal.GOLD) {
            gold++;
        } else if (medal == Medal.SILVER) {
            silver++;
        } else if (medal == Medal.BRONZE) {
            bronze++;
        }
        if (timeMs > 0 && (best(d) < 0 || timeMs < best(d))) {
            best.put(d, timeMs);
            return true;
        }
        return false;
    }

    public void addEvent(boolean won) {
        events++;
        if (won) {
            wins++;
        }
    }

    public void addShots(int hit, int fired) {
        hits += Math.max(0, hit);
        shots += Math.max(0, fired);
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("wettkaempfe", events);
        y.set("rennen", races);
        y.set("gesamtsiege", wins);
        y.set("gold", gold);
        y.set("silber", silver);
        y.set("bronze", bronze);
        y.set("punkte", points);
        y.set("biathlon.treffer", hits);
        y.set("biathlon.schuesse", shots);
        best.forEach((d, ms) -> y.set("bestzeit." + d.id(), ms));
        return y.saveToString();
    }

    public static PlayerData fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        PlayerData d = new PlayerData(uuid, y.getString("name", "?"));
        d.events = y.getInt("wettkaempfe");
        d.races = y.getInt("rennen");
        d.wins = y.getInt("gesamtsiege");
        d.gold = y.getInt("gold");
        d.silver = y.getInt("silber");
        d.bronze = y.getInt("bronze");
        d.points = y.getInt("punkte");
        d.hits = y.getInt("biathlon.treffer");
        d.shots = y.getInt("biathlon.schuesse");
        ConfigurationSection s = y.getConfigurationSection("bestzeit");
        if (s != null) {
            for (String k : s.getKeys(false)) {
                Discipline disc = Discipline.byId(k);
                if (disc != null) {
                    d.best.put(disc, s.getLong(k));
                }
            }
        }
        return d;
    }
}
