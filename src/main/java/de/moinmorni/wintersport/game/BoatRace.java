package de.moinmorni.wintersport.game;

import de.moinmorni.wintersport.Texts;
import de.moinmorni.wintersport.Ui;
import de.moinmorni.wintersport.WintersportPlugin;
import de.moinmorni.wintersport.course.Bob;
import de.moinmorni.wintersport.course.Path;
import de.moinmorni.wintersport.course.Places;
import de.moinmorni.wintersport.course.Slalom;
import de.moinmorni.wintersport.course.Terrain;
import de.moinmorni.wintersport.logic.Discipline;
import de.moinmorni.wintersport.logic.Gate;
import de.moinmorni.wintersport.logic.Heats;
import de.moinmorni.wintersport.logic.Line;
import de.moinmorni.wintersport.logic.RaceClock;
import de.moinmorni.wintersport.logic.Teams;
import de.moinmorni.wintersport.logic.TimeFormat;
import io.papermc.paper.entity.TeleportFlag;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Riesenslalom oder Bob: Läufe mit bis zu vier Booten nebeneinander (je eine eigene Bahn). Ablauf je Lauf: an den
 * Start, ins Boot, Countdown, Starttor auf, Zeitnahme (Tore bzw. Kontrollpunkte, Ziellinie auf die Hundertstel). Wer
 * aussteigt oder feststeckt, kommt zum letzten Tor bzw. Kontrollpunkt zurück – die Zeit läuft weiter.
 */
public final class BoatRace implements Stage {

    private enum HeatPhase { PREP, COUNT, RUN, AFTER }

    private static final int PREP_TICKS = 50;
    private static final int COUNT_SECONDS = 3;
    private static final int AFTER_TICKS = 50;
    private static final int RELEASE_TICKS = 40;
    private static final int SEAT_DELAY = 5;
    private static final int TICKS = 20;
    private static final double FALL_LIMIT = 3.5;
    private static final double PILOT_STEP = 1.2;

    /** Ein Boot: Einzel oder Zweierbob. */
    final class Team {
        final List<Racer> members;
        final int heat;
        int lane;
        Boat boat;
        double px;
        double pz;
        boolean finished;
        boolean dnf;
        long timeMs;
        int nextGate;
        int misses;
        int lastGate = -1;
        int sample = -1;
        int checkpoint;
        boolean resetting;
        long releaseAt = -1;
        boolean released;
        final Deque<double[]> pilot = new ArrayDeque<>();

        Team(List<Racer> members, int heat) {
            this.members = new ArrayList<>(members);
            this.heat = heat;
        }

        String names() {
            return members.stream().map(Racer::name).reduce((a, b) -> a + "+" + b).orElse("");
        }

        boolean active() {
            return !finished && !dnf;
        }
    }

    private final Competition comp;
    private final WintersportPlugin plugin;
    private final Discipline discipline;
    private final boolean slalom;
    private final List<Team> teams = new ArrayList<>();
    private final List<List<Team>> heats = new ArrayList<>();
    private int heat = -1;
    private HeatPhase phase = HeatPhase.PREP;
    private int phaseTicks;
    private RaceClock clock;
    private long goTick;
    private long nowTick;
    private boolean done;

    BoatRace(Competition comp, Discipline discipline) {
        this.comp = comp;
        this.plugin = comp.plugin();
        this.discipline = discipline;
        this.slalom = discipline == Discipline.SLALOM;
        List<List<Racer>> groups = new ArrayList<>();
        for (List<Racer> party : comp.parties()) {
            // Wer gerade die Verbindung verloren hat und zurückkommen darf, bekommt seinen Platz in den Läufen.
            List<Racer> here = party.stream().filter(r -> r.player() != null || comp.waitingFor(r.uuid())).toList();
            if (!here.isEmpty()) {
                groups.add(here);
            }
        }
        List<List<Racer>> boats = Teams.bobs(groups, !slalom && plugin.settings().twoManBob());
        List<List<List<Racer>>> split = Heats.split(boats, lanes());
        for (int h = 0; h < split.size(); h++) {
            List<Team> list = new ArrayList<>();
            for (int i = 0; i < split.get(h).size(); i++) {
                Team t = new Team(split.get(h).get(i), h);
                t.lane = i;
                list.add(t);
                teams.add(t);
            }
            heats.add(list);
        }
    }

    private int lanes() {
        return slalom ? Slalom.lanes() : Bob.lanes();
    }

    @Override
    public Discipline discipline() {
        return discipline;
    }

    @Override
    public void start() {
        for (Team t : teams) {
            for (Racer r : t.members) {
                Player p = r.player();
                if (p == null) {
                    continue;
                }
                String lang = plugin.lang(p);
                p.sendMessage(Texts.msg(lang, slalom ? "slalom.intro" : "bob.intro",
                        Placeholder.unparsed("secs", String.valueOf(plugin.settings().slalomPenaltySeconds()))));
                if (t.members.size() > 1) {
                    p.sendMessage(Texts.msg(lang, "bob.pair", Placeholder.unparsed("name", t.members.get(0).name())));
                }
                if (heats.size() > 1 && t.heat > 0) {
                    p.sendMessage(Texts.msg(lang, "heat.wait", Placeholder.unparsed("n", String.valueOf(t.heat + 1))));
                }
                toStand(p, r);
            }
        }
        startHeat(0);
    }

    private void startHeat(int h) {
        heat = h;
        phase = HeatPhase.PREP;
        phaseTicks = 0;
        for (int lane = 0; lane < lanes(); lane++) {
            setGate(lane, false);
        }
        for (Team t : heats.get(h)) {
            if (gone(t)) {
                t.dnf = true;
                continue;
            }
            Location start = startOf(t.lane);
            place(t, start);
            for (Racer r : t.members) {
                Player p = r.player();
                if (p != null) {
                    String lang = plugin.lang(p);
                    p.sendMessage(Texts.msg(lang, "heat.your-turn", Placeholder.unparsed("lane", String.valueOf(t.lane + 1))));
                }
            }
        }
        for (Player p : comp.everyone()) {
            String lang = plugin.lang(p);
            if (heats.size() > 1) {
                Ui.title(p, Texts.text(lang, "heat.title", Placeholder.unparsed("n", String.valueOf(h + 1)),
                        Placeholder.unparsed("total", String.valueOf(heats.size()))), Texts.text(lang, "count.sub"), 1500);
            }
        }
    }

    private Location startOf(int lane) {
        double x = slalom ? Slalom.centerX(lane) : Bob.x(lane, 0);
        double z = slalom ? Slalom.START_Z : Bob.START_Z;
        return new Location(plugin.world(), x, Terrain.standY(z), z, 0f, 0f);
    }

    /** Starttor der Bahn auf- oder zumachen. */
    private void setGate(int lane, boolean open) {
        int x1 = slalom ? Slalom.LANE_X[lane] - Slalom.HALF : Bob.LANE_X[lane] - 1;
        int x2 = slalom ? Slalom.LANE_X[lane] + Slalom.HALF : Bob.LANE_X[lane] + 1;
        int z = slalom ? Slalom.GATE_ROW_Z : Bob.GATE_ROW_Z;
        int y = Terrain.FLOOR_Y + Terrain.TOP + 1;
        for (int x = x1; x <= x2; x++) {
            Block b = plugin.world().getBlockAt(x, y, z);
            if (b.getBlockData() instanceof Openable o && o.isOpen() != open) {
                o.setOpen(open);
                b.setBlockData(o, false);
            }
        }
    }

    /** Team mit neuem Boot an diese Stelle setzen (Start oder Zurücksetzen). */
    private void place(Team t, Location at) {
        t.resetting = true;
        removeBoat(t);
        t.pilot.clear();
        for (Racer r : t.members) {
            Player p = r.player();
            if (p == null) {
                continue;
            }
            if (p.getVehicle() != null) {
                p.leaveVehicle();
            }
            comp.dress(p, r, true);
            p.teleport(at);
        }
        Class<? extends Boat> type = slalom ? org.bukkit.entity.boat.BirchBoat.class : org.bukkit.entity.boat.DarkOakBoat.class;
        Boat boat = plugin.world().spawn(at, type, b -> {
            b.setPersistent(false);
            b.setRotation(at.getYaw(), 0f);
            b.addScoreboardTag("wintersport_boat");
        });
        t.boat = boat;
        t.px = at.getX();
        t.pz = at.getZ();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            seat(t);
            t.resetting = false;
        }, SEAT_DELAY);
    }

    private void seat(Team t) {
        if (t.boat == null || !t.boat.isValid()) {
            return;
        }
        for (Racer r : t.members) {
            Player p = r.player();
            if (p != null && !t.boat.getPassengers().contains(p)) {
                if (p.getVehicle() != null) {
                    p.leaveVehicle();
                }
                p.teleport(t.boat.getLocation());
                t.boat.addPassenger(p);
            }
        }
    }

    private boolean seated(Team t) {
        if (t.boat == null || !t.boat.isValid()) {
            return false;
        }
        for (Racer r : t.members) {
            Player p = r.player();
            if (p != null && !t.boat.getPassengers().contains(p)) {
                return false;
            }
        }
        return true;
    }

    private void removeBoat(Team t) {
        if (t.boat != null) {
            for (Entity e : new ArrayList<>(t.boat.getPassengers())) {
                t.boat.removePassenger(e);
            }
            t.boat.remove();
            t.boat = null;
        }
    }

    @Override
    public void tick(long tick) {
        nowTick = tick;
        if (done || heat < 0) {
            return;
        }
        phaseTicks++;
        List<Team> now = heats.get(heat);
        switch (phase) {
            case PREP -> {
                if (phaseTicks % 10 == 0) {
                    now.stream().filter(t -> !t.resetting && t.active()).forEach(this::seat);
                }
                if (phaseTicks >= PREP_TICKS) {
                    phase = HeatPhase.COUNT;
                    phaseTicks = 0;
                }
            }
            case COUNT -> {
                if (phaseTicks % TICKS == 1) {
                    int n = COUNT_SECONDS - phaseTicks / TICKS;
                    for (Team t : now) {
                        for (Racer r : t.members) {
                            Player p = r.player();
                            if (p != null) {
                                String lang = plugin.lang(p);
                                Ui.title(p, Texts.text(lang, "count.number", Placeholder.unparsed("n", String.valueOf(n))),
                                        Texts.text(lang, "count.sub"), 900);
                                Ui.sound(p, "minecraft:block.note_block.bell", 1f);
                            }
                        }
                    }
                    now.stream().filter(t -> !t.resetting && t.active() && !seated(t)).forEach(this::seat);
                }
                if (phaseTicks >= COUNT_SECONDS * TICKS) {
                    go(tick, now);
                }
            }
            case RUN -> run(tick, now);
            case AFTER -> {
                releaseDue(now, true);
                if (phaseTicks >= AFTER_TICKS) {
                    if (heat + 1 < heats.size()) {
                        startHeat(heat + 1);
                    } else {
                        done = true;
                    }
                }
            }
            default -> {
            }
        }
    }

    private void go(long tick, List<Team> now) {
        phase = HeatPhase.RUN;
        phaseTicks = 0;
        goTick = tick;
        clock = new RaceClock(tick);
        for (Team t : now) {
            setGate(t.lane, true);
            for (Racer r : t.members) {
                Player p = r.player();
                if (p != null) {
                    String lang = plugin.lang(p);
                    Ui.title(p, Texts.text(lang, "count.go"), net.kyori.adventure.text.Component.empty(), 700);
                    Ui.sound(p, "minecraft:entity.firework_rocket.launch", 1.2f);
                }
            }
        }
    }

    private void run(long tick, List<Team> now) {
        long limit = (slalom ? plugin.settings().slalomLimit() : plugin.settings().bobLimit()) * (long) TICKS;
        for (Team t : now) {
            if (t.active()) {
                if (gone(t)) {
                    t.dnf = true;
                    removeBoat(t);
                    continue;
                }
                if (t.members.stream().allMatch(r -> r.player() == null)) {
                    // Verbindung verloren, Platz noch frei: das Boot wartet, die Uhr läuft weiter (Zeitlimit gilt).
                    if (tick - goTick >= limit) {
                        t.dnf = true;
                        release(t);
                    }
                    continue;
                }
                steer(t);
                track(t, tick);
                if (t.active() && tick - goTick >= limit) {
                    t.dnf = true;
                    for (Racer r : t.members) {
                        Player p = r.player();
                        if (p != null) {
                            p.sendMessage(Texts.msg(plugin.lang(p), "race.dnf"));
                        }
                    }
                    release(t);
                }
            }
        }
        releaseDue(now, false);
        if (now.stream().noneMatch(Team::active)) {
            phase = HeatPhase.AFTER;
            phaseTicks = 0;
        }
    }

    /** Niemand mehr da und niemand, der zurückkommen darf? */
    private boolean gone(Team t) {
        return t.members.stream().allMatch(r -> r.player() == null && !comp.waitingFor(r.uuid()));
    }

    private void releaseDue(List<Team> now, boolean all) {
        for (Team t : now) {
            if (!t.released && (t.finished || t.dnf) && (all || (t.releaseAt >= 0 && nowTick >= t.releaseAt))) {
                release(t);
            }
        }
    }

    /** Ein Tick Zeitnahme für ein Boot. */
    private void track(Team t, long tick) {
        if (t.resetting) {
            return;
        }
        if (!seated(t)) {
            reset(t);
            return;
        }
        Location l = t.boat.getLocation();
        double qx = l.getX();
        double qz = l.getZ();
        if (slalom) {
            List<Gate> gates = Slalom.gates(t.lane);
            while (t.nextGate < gates.size()) {
                Gate.Pass pass = gates.get(t.nextGate).check(t.px, t.pz, qx, qz);
                if (pass == Gate.Pass.NONE) {
                    break;
                }
                if (pass == Gate.Pass.PASSED) {
                    t.lastGate = t.nextGate;
                    sound(t, "minecraft:block.note_block.chime", 1.6f);
                } else {
                    missed(t);
                    t.lastGate = t.nextGate;
                }
                t.nextGate++;
            }
        } else {
            Path path = Bob.path(t.lane);
            t.sample = path.nearest(qx, qz, t.sample);
            List<Path.Sample> cps = Bob.checkpoints(t.lane);
            double s = path.samples().get(t.sample).s();
            while (t.checkpoint + 1 < cps.size() && cps.get(t.checkpoint + 1).s() <= s) {
                t.checkpoint++;
            }
        }
        Line finish = slalom ? Slalom.finish(t.lane) : Bob.finish(t.lane);
        Line.Hit hit = finish.cross(t.px, t.pz, qx, qz);
        t.px = qx;
        t.pz = qz;
        if (hit != null && (slalom || t.sample > Bob.path(t.lane).samples().size() / 2)) {
            if (slalom) {
                while (t.nextGate < Slalom.gateCount()) {
                    missed(t);
                    t.nextGate++;
                }
            }
            finish(t, clock.at(tick, hit.t()));
            return;
        }
        if (l.getY() < Terrain.standY(qz) - FALL_LIMIT) {
            reset(t);
        }
    }

    private void missed(Team t) {
        t.misses++;
        for (Racer r : t.members) {
            Player p = r.player();
            if (p != null) {
                p.sendActionBar(Texts.text(plugin.lang(p), "race.gate.missed",
                        Placeholder.unparsed("secs", String.valueOf(plugin.settings().slalomPenaltySeconds()))));
                Ui.sound(p, "minecraft:block.note_block.bass", 0.6f);
            }
        }
    }

    private void sound(Team t, String key, float pitch) {
        for (Racer r : t.members) {
            Player p = r.player();
            if (p != null) {
                Ui.sound(p, key, pitch);
            }
        }
    }

    private long penaltyMs(Team t) {
        return t.misses * plugin.settings().slalomPenaltySeconds() * 1000L;
    }

    private void finish(Team t, long rawMs) {
        t.finished = true;
        t.timeMs = rawMs + penaltyMs(t);
        t.releaseAt = nowTick + RELEASE_TICKS;
        Location at = t.boat != null ? t.boat.getLocation() : null;
        for (Racer r : t.members) {
            Player p = r.player();
            if (p == null) {
                continue;
            }
            String lang = plugin.lang(p);
            String time = TimeFormat.format(t.timeMs, lang);
            String extra = penaltyMs(t) > 0 ? Texts.raw(lang, "race.finish.penalty").replace("<secs>",
                    String.valueOf(penaltyMs(t) / 1000)) : "";
            Ui.title(p, Texts.text(lang, "race.finish.title"), Texts.text(lang, "race.finish.sub", Placeholder.unparsed("time", time)), 2000);
            p.sendMessage(Texts.msg(lang, "race.finish.msg", Placeholder.unparsed("time", time), Placeholder.parsed("extra", extra)));
            Ui.sound(p, "minecraft:entity.player.levelup", 1.4f);
        }
        if (at != null) {
            plugin.world().spawnParticle(Particle.FIREWORK, at.clone().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.05);
        }
    }

    /** Boot weg, Leute auf die Tribüne. */
    private void release(Team t) {
        t.released = true;
        t.resetting = true;
        removeBoat(t);
        for (Racer r : t.members) {
            Player p = r.player();
            if (p != null) {
                toStand(p, r);
            }
        }
    }

    private void toStand(Player p, Racer r) {
        if (p.getVehicle() != null) {
            p.leaveVehicle();
        }
        comp.dress(p, r, false);
        p.teleport(view());
    }

    /** Zurück zum letzten Tor bzw. Kontrollpunkt (neues Boot, die Zeit läuft weiter). */
    private void reset(Team t) {
        if (!t.active() || t.resetting || phase != HeatPhase.RUN) {
            return;
        }
        Location at;
        if (slalom) {
            double[] p = Slalom.resetAfter(t.lane, t.lastGate);
            at = new Location(plugin.world(), p[0], Terrain.standY(p[1]), p[1], 0f, 0f);
        } else {
            Path.Sample s = Bob.checkpoints(t.lane).get(t.checkpoint);
            at = new Location(plugin.world(), s.x(), Terrain.standY(s.z()), s.z(), s.yaw(), 0f);
            t.sample = Bob.path(t.lane).nearest(s.x(), s.z(), -1);
        }
        place(t, at);
        for (Racer r : t.members) {
            Player p = r.player();
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "race.reset"));
            }
        }
    }

    // ---------- Selbstfahrer (nur zum Testen: /wsadmin fahren) ----------

    /** Wegpunkte zum Abfahren (x, z), je Tick höchstens {@link #PILOT_STEP} Blöcke. */
    public boolean drive(Player p, List<double[]> points) {
        Team t = teamOf(p);
        if (t == null || !t.active() || phase != HeatPhase.RUN) {
            return false;
        }
        t.pilot.clear();
        t.pilot.addAll(points);
        return true;
    }

    private void steer(Team t) {
        if (t.pilot.isEmpty() || t.resetting || t.boat == null || !t.boat.isValid()) {
            return;
        }
        Location l = t.boat.getLocation();
        double budget = PILOT_STEP;
        double x = l.getX();
        double z = l.getZ();
        while (budget > 0 && !t.pilot.isEmpty()) {
            double[] w = t.pilot.peekFirst();
            double d = Math.hypot(w[0] - x, w[1] - z);
            if (d <= budget) {
                x = w[0];
                z = w[1];
                budget -= d;
                t.pilot.pollFirst();
            } else {
                x += (w[0] - x) / d * budget;
                z += (w[1] - z) / d * budget;
                budget = 0;
            }
        }
        float yaw = Path.yaw(x - l.getX(), z - l.getZ());
        Location to = new Location(l.getWorld(), x, Terrain.standY(z), z, yaw, 0f);
        t.boat.teleport(to, TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }

    /** Wegpunkte durch Tor n (Slalom), richtig oder daneben. */
    public List<double[]> gatePoints(Player p, int n, boolean wrong) {
        Team t = teamOf(p);
        List<double[]> out = new ArrayList<>();
        if (t == null || !slalom || n < 0 || n >= Slalom.gateCount()) {
            return out;
        }
        Gate g = Slalom.gates(t.lane).get(n);
        double[] mid = g.middle();
        double x = wrong ? 2 * Slalom.centerX(t.lane) - mid[0] : mid[0];
        out.add(new double[] {x, mid[1] - 3});
        out.add(new double[] {x, mid[1] + 3});
        return out;
    }

    /** Wegpunkte bis ins Ziel (Slalom: durch alle übrigen Tore richtig; Bob: der Kanal entlang). */
    public List<double[]> finishPoints(Player p) {
        Team t = teamOf(p);
        List<double[]> out = new ArrayList<>();
        if (t == null) {
            return out;
        }
        if (slalom) {
            for (int g = t.nextGate; g < Slalom.gateCount(); g++) {
                out.addAll(gatePoints(p, g, false));
            }
            out.add(new double[] {Slalom.centerX(t.lane), Slalom.FINISH_Z + 2});
        } else {
            Path path = Bob.path(t.lane);
            int from = Math.max(0, t.sample);
            for (int i = from; i < path.samples().size(); i += 2) {
                Path.Sample s = path.samples().get(i);
                if (s.z() > Bob.FINISH_Z + 2.5) {
                    break;
                }
                out.add(new double[] {s.x(), s.z()});
            }
            out.add(new double[] {Bob.finishX(t.lane), Bob.FINISH_Z + 2});
        }
        return out;
    }

    /** Wegpunkte n Blöcke weiter den Kanal entlang (Bob). */
    public List<double[]> aheadPoints(Player p, double blocks) {
        Team t = teamOf(p);
        List<double[]> out = new ArrayList<>();
        if (t == null || slalom) {
            return out;
        }
        Path path = Bob.path(t.lane);
        int from = Math.max(0, t.sample);
        double s0 = path.samples().get(from).s();
        for (int i = from; i < path.samples().size(); i += 2) {
            Path.Sample s = path.samples().get(i);
            if (s.s() - s0 > blocks) {
                break;
            }
            out.add(new double[] {s.x(), s.z()});
        }
        return out;
    }

    private Team teamOf(Player p) {
        for (Team t : teams) {
            for (Racer r : t.members) {
                if (r.uuid().equals(p.getUniqueId())) {
                    return t;
                }
            }
        }
        return null;
    }

    /** Ausgestiegen? Im Lauf: zurück auf die Strecke. Vor dem Start: wieder hinein. */
    public boolean exited(Player p) {
        Team t = teamOf(p);
        if (t == null || t.resetting || !t.active() || heat < 0 || t.heat != heat) {
            return false;
        }
        if (phase == HeatPhase.RUN) {
            Bukkit.getScheduler().runTask(plugin, () -> reset(t));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> seat(t), 2L);
        }
        return true;
    }

    /** Gehört dieses Boot zu einem Lauf? */
    public boolean ours(Entity boat) {
        return teams.stream().anyMatch(t -> t.boat == boat);
    }

    @Override
    public void second() {
        if (heat < 0) {
            return;
        }
        for (Team t : heats.get(heat)) {
            if (!t.active() || phase != HeatPhase.RUN) {
                continue;
            }
            for (Racer r : t.members) {
                Player p = r.player();
                if (p == null) {
                    continue;
                }
                String lang = plugin.lang(p);
                String time = TimeFormat.format(clock.now(nowTick), lang);
                if (slalom) {
                    String extra = t.misses > 0 ? Texts.raw(lang, "race.bar.penalty").replace("<secs>", String.valueOf(penaltyMs(t) / 1000)) : "";
                    p.sendActionBar(Texts.text(lang, "race.bar.slalom", Placeholder.unparsed("time", time),
                            Placeholder.unparsed("n", String.valueOf(t.nextGate)), Placeholder.unparsed("total", String.valueOf(Slalom.gateCount())),
                            Placeholder.parsed("extra", extra)));
                } else {
                    p.sendActionBar(Texts.text(lang, "race.bar.bob", Placeholder.unparsed("time", time),
                            Placeholder.unparsed("n", String.valueOf(bends(t))), Placeholder.unparsed("total", String.valueOf(Terrain.TERRACES))));
                }
            }
        }
        // Zuschauer und Wartende: wer gerade fährt
        long left = heats.get(heat).stream().filter(Team::active).count();
        for (Player p : comp.everyone()) {
            Team t = teamOf(p);
            if (t != null && t.heat == heat && t.active()) {
                continue;
            }
            String lang = plugin.lang(p);
            p.sendActionBar(Texts.text(lang, "race.bar.watch", Placeholder.unparsed("n", String.valueOf(heat + 1)),
                    Placeholder.unparsed("total", String.valueOf(heats.size())), Placeholder.unparsed("left", String.valueOf(left))));
        }
        plugin.boards().live(discipline, liveRows(), heat + 1, heats.size());
    }

    /** Wie viele Kehren schon hinter dem Bob liegen. */
    private int bends(Team t) {
        int n = 0;
        for (int i = Terrain.TERRACES - 1; i >= 0; i--) {
            if (t.pz > Terrain.terraceMiddle(i) + 2) {
                n++;
            }
        }
        return n;
    }

    private List<Result> liveRows() {
        return results();
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public List<Result> results() {
        List<Result> out = new ArrayList<>();
        for (Team t : teams) {
            if (t.members.isEmpty()) {
                continue;
            }
            out.add(new Result(List.copyOf(t.members), t.finished ? t.timeMs : 0, t.finished, t.misses, 0, 0));
        }
        return out;
    }

    @Override
    public void quit(Racer r) {
        for (Team t : teams) {
            if (t.members.contains(r)) {
                Player p = r.player();
                if (p != null && p.getVehicle() != null) {
                    t.resetting = true;
                    p.leaveVehicle();
                    t.resetting = false;
                }
                if (t.members.stream().allMatch(m -> m == r || m.player() == null)) {
                    if (t.active()) {
                        t.dnf = true;
                    }
                    removeBoat(t);
                }
            }
        }
    }

    /** Verbindung weg: aus dem Boot steigen lassen, ohne dass das als „zurück aufs Eis“ zählt; das Boot wartet. */
    @Override
    public void away(Racer r) {
        for (Team t : teams) {
            Player p = r.player();
            if (t.members.contains(r) && p != null && p.getVehicle() != null) {
                boolean was = t.resetting;
                t.resetting = true;
                p.leaveVehicle();
                t.resetting = was;
            }
        }
    }

    /**
     * Rechtzeitig zurück: Läuft sein Lauf gerade, sitzt er gleich wieder im Boot – zu zweit im laufenden Bob, allein in
     * einem neuen Boot am letzten Tor bzw. Kontrollpunkt (die Uhr ist weitergelaufen). Wartet sein Lauf noch, sitzt er
     * am Start; ist er vorbei oder kommt er erst, geht es auf die Tribüne.
     */
    @Override
    public void rejoin(Racer r, Player p) {
        Team t = teamOf(p);
        if (t == null || !t.active() || t.heat != heat || phase == HeatPhase.AFTER) {
            toStand(p, r);
            return;
        }
        comp.dress(p, r, true);
        boolean partnerAboard = t.boat != null && t.boat.isValid() && !t.boat.getPassengers().isEmpty();
        if (phase != HeatPhase.RUN || partnerAboard) {
            if (t.boat == null || !t.boat.isValid()) {
                place(t, startOf(t.lane));
            } else {
                seat(t);
            }
        } else {
            t.resetting = false;
            reset(t);
        }
    }

    @Override
    public void giveUp(Racer r) {
        for (Team t : teams) {
            if (t.members.contains(r) && t.active()) {
                t.dnf = true;
                release(t);
            }
        }
    }

    @Override
    public void cleanup() {
        for (Team t : teams) {
            t.resetting = true;
            removeBoat(t);
        }
        for (int lane = 0; lane < lanes(); lane++) {
            setGate(lane, false);
        }
        plugin.boards().live(discipline, List.of(), 0, 0);
    }

    @Override
    public Location view() {
        int[] s = slalom ? Places.STAND_SLALOM : Places.STAND_BOB;
        return new Location(plugin.world(), s[0] + 0.5, Terrain.FLOOR_Y + 3, s[1] + 4.5, 180f, -12f);
    }

    @Override
    public String status(Racer r, String lang) {
        if (heat < 0) {
            return "";
        }
        return Texts.raw(lang, "sidebar.heat").replace("<n>", String.valueOf(heat + 1)).replace("<total>", String.valueOf(heats.size()));
    }

    @Override
    public void reset(Player p) {
        Team t = teamOf(p);
        if (t != null && t.heat == heat) {
            reset(t);
        }
    }

    @Override
    public List<String> debug() {
        List<String> out = new ArrayList<>();
        out.add("stage=" + discipline.id() + " heat=" + (heat + 1) + "/" + heats.size() + " phase=" + phase + " done=" + done);
        for (Team t : teams) {
            Location l = t.boat != null && t.boat.isValid() ? t.boat.getLocation() : null;
            out.add("t=" + t.names() + " heat=" + (t.heat + 1) + " lane=" + (t.lane + 1) + " state="
                    + (t.finished ? "finished" : t.dnf ? "dnf" : heat == t.heat ? phase.name().toLowerCase() : "waiting")
                    + " gate=" + t.nextGate + " misses=" + t.misses + " cp=" + t.checkpoint + " time=" + t.timeMs
                    + " boat=" + (l != null) + " passengers=" + (t.boat != null && t.boat.isValid() ? t.boat.getPassengers().size() : 0)
                    + (l == null ? "" : String.format(java.util.Locale.ROOT, " x=%.1f y=%.1f z=%.1f", l.getX(), l.getY(), l.getZ())));
        }
        return out;
    }
}
