package de.moinmorni.wintersport.game;

import de.moinmorni.wintersport.Items;
import de.moinmorni.wintersport.Texts;
import de.moinmorni.wintersport.Ui;
import de.moinmorni.wintersport.WintersportPlugin;
import de.moinmorni.wintersport.course.Biathlon;
import de.moinmorni.wintersport.course.Path;
import de.moinmorni.wintersport.course.Places;
import de.moinmorni.wintersport.course.Terrain;
import de.moinmorni.wintersport.logic.BiathlonProgress;
import de.moinmorni.wintersport.logic.Discipline;
import de.moinmorni.wintersport.logic.Line;
import de.moinmorni.wintersport.logic.RaceClock;
import de.moinmorni.wintersport.logic.TimeFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Biathlon im Massenstart: alle laufen zusammen los (Schnelligkeit), Kontrollpunkte der Reihe nach, über die
 * Rundenlinie. Nach den ersten Runden an die eigene Bahn am Schießstand: fünf Pfeile (hier ohne Schwerkraft), jede
 * getroffene Scheibe klappt weiß um. Fehler = Strafrunden in der roten Schleife, dann weiter. Wer zuerst nach der
 * letzten Runde über die Linie kommt, gewinnt.
 */
public final class BiathlonRace implements Stage {

    private static final int COUNT_SECONDS = 3;
    private static final int PREP_TICKS = 40;
    private static final int TICKS = 20;
    private static final int ARROW_LIFE = 50;
    private static final int AFTER_LAST_SHOT = 15;
    private static final double MAT_LEASH = 0.8;
    private static final int SLOT_BOW = 0;
    private static final int SLOT_ARROWS = 1;
    private static final double PILOT_STEP = 0.9;
    /** Nach einer ganzen Runde noch so viele Abtastpunkte weiter (über die Linie hinaus). */
    private static final int PILOT_EXTRA = 48;

    private enum Phase { PREP, COUNT, RUN, OVER }

    /** Eine Läuferin / ein Läufer. */
    final class Runner {
        final Racer racer;
        final int lane;
        final BiathlonProgress progress;
        double px;
        double pz;
        boolean finished;
        boolean dnf;
        long timeMs;
        long shootUntil;
        long lastShot = -1;
        int arrowsOut;
        boolean onMat;
        /** Schon an der Startbahn gewesen (wer beim Start gerade weg war, kommt beim Wiederkommen dorthin)? */
        boolean placed;
        final java.util.Deque<double[]> pilot = new java.util.ArrayDeque<>();

        Runner(Racer racer, int lane) {
            this.racer = racer;
            this.lane = lane;
            this.progress = new BiathlonProgress(plugin.settings().biathlonLaps(), plugin.settings().biathlonShootings(),
                    Biathlon.TARGETS, Biathlon.checkpoints().size());
        }

        boolean active() {
            return !finished && !dnf;
        }
    }

    private record Shot(Runner runner, long tick) {
    }

    private final Competition comp;
    private final WintersportPlugin plugin;
    private final List<Runner> runners = new ArrayList<>();
    private final Map<UUID, Shot> arrows = new HashMap<>();
    private Phase phase = Phase.PREP;
    private int phaseTicks;
    private RaceClock clock;
    private long goTick;
    private long nowTick;
    private boolean done;

    BiathlonRace(Competition comp) {
        this.comp = comp;
        this.plugin = comp.plugin();
        int lane = 0;
        for (Racer r : comp.racers()) {
            // Wer gerade die Verbindung verloren hat und zurückkommen darf, bekommt seine Bahn.
            if ((r.player() != null || comp.waitingFor(r.uuid())) && !r.left() && lane < Biathlon.LANES) {
                runners.add(new Runner(r, lane++));
            }
        }
    }

    @Override
    public Discipline discipline() {
        return Discipline.BIATHLON;
    }

    @Override
    public void start() {
        setStartGate(false);
        for (int k = 0; k < Biathlon.LANES; k++) {
            resetTargets(k);
        }
        for (Runner run : runners) {
            Player p = run.racer.player();
            if (p == null) {
                continue;
            }
            comp.dress(p, run.racer, false);
            double[] g = Biathlon.grid(run.lane);
            Location at = new Location(plugin.world(), g[0], Terrain.FLOOR_Y + 1, g[1], -90f, 0f);
            p.teleport(at);
            run.px = at.getX();
            run.pz = at.getZ();
            run.placed = true;
            freeze(p, true);
            p.sendMessage(Texts.msg(plugin.lang(p), "biathlon.intro", Placeholder.unparsed("laps",
                    String.valueOf(plugin.settings().biathlonLaps()))));
        }
    }

    private void freeze(Player p, boolean frozen) {
        var speed = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(frozen ? 0 : speed.getDefaultValue());
        }
        var jump = p.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) {
            jump.setBaseValue(frozen ? 0 : jump.getDefaultValue());
        }
    }

    private void setStartGate(boolean open) {
        int y = Terrain.FLOOR_Y + 1;
        for (int z = (int) Math.floor(Biathlon.NORTH_Z - Biathlon.HALF); z <= (int) Math.floor(Biathlon.NORTH_Z + Biathlon.HALF); z++) {
            Block b = plugin.world().getBlockAt(Biathlon.START_GATE_X, y, z);
            if (b.getBlockData() instanceof Openable o && o.isOpen() != open) {
                o.setOpen(open);
                b.setBlockData(o, false);
            }
        }
    }

    private void resetTargets(int lane) {
        for (int t = 0; t < Biathlon.TARGETS; t++) {
            plugin.world().getBlockAt(Biathlon.targetX(lane, t), Terrain.FLOOR_Y + Biathlon.TARGET_Y, Biathlon.TARGET_Z)
                    .setType(Material.BLACK_CONCRETE, false);
        }
    }

    @Override
    public void tick(long tick) {
        nowTick = tick;
        if (done) {
            return;
        }
        phaseTicks++;
        switch (phase) {
            case PREP -> {
                if (phaseTicks >= PREP_TICKS) {
                    phase = Phase.COUNT;
                    phaseTicks = 0;
                }
            }
            case COUNT -> {
                if (phaseTicks % TICKS == 1) {
                    int n = COUNT_SECONDS - phaseTicks / TICKS;
                    for (Runner run : runners) {
                        Player p = run.racer.player();
                        if (p != null) {
                            String lang = plugin.lang(p);
                            Ui.title(p, Texts.text(lang, "count.number", Placeholder.unparsed("n", String.valueOf(n))),
                                    Texts.text(lang, "count.sub"), 900);
                            Ui.sound(p, "minecraft:block.note_block.bell", 1f);
                        }
                    }
                }
                if (phaseTicks >= COUNT_SECONDS * TICKS) {
                    go(tick);
                }
            }
            case RUN -> run(tick);
            default -> {
            }
        }
    }

    private void go(long tick) {
        phase = Phase.RUN;
        phaseTicks = 0;
        goTick = tick;
        clock = new RaceClock(tick);
        setStartGate(true);
        int amp = plugin.settings().runSpeed() - 1;
        for (Runner run : runners) {
            Player p = run.racer.player();
            if (p == null) {
                continue;
            }
            freeze(p, false);
            if (amp >= 0) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, amp, false, false, true));
            }
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "count.go"), net.kyori.adventure.text.Component.empty(), 700);
            Ui.sound(p, "minecraft:entity.firework_rocket.launch", 1.2f);
        }
    }

    private void run(long tick) {
        long limit = plugin.settings().biathlonLimit() * (long) TICKS;
        for (Runner run : runners) {
            if (!run.active()) {
                continue;
            }
            Player p = run.racer.player();
            if (p == null) {
                // Verbindung verloren, Platz noch frei: Stand und Uhr bleiben, das Zeitlimit gilt weiter.
                if (!comp.waitingFor(run.racer.uuid()) || tick - goTick >= limit) {
                    run.dnf = true;
                }
                continue;
            }
            if (run.progress.phase() == BiathlonProgress.Phase.SHOOTING) {
                run.pilot.clear();
                shooting(run, p, tick);
            } else {
                move(run, p, tick);
            }
            if (run.active() && tick - goTick >= limit) {
                run.dnf = true;
                p.sendMessage(Texts.msg(plugin.lang(p), "race.dnf"));
                toStand(run, p);
            }
        }
        expireArrows(tick);
        if (runners.stream().noneMatch(Runner::active)) {
            phase = Phase.OVER;
            done = true;
        }
    }

    /** Laufen: Linien prüfen. */
    private void move(Runner run, Player p, long tick) {
        walk(run, p);
        Location l = p.getLocation();
        double qx = l.getX();
        double qz = l.getZ();
        double px = run.px;
        double pz = run.pz;
        run.px = qx;
        run.pz = qz;
        if (l.getY() < Terrain.FLOOR_Y - 4) {
            p.teleport(new Location(plugin.world(), Biathlon.afterRange()[0], Terrain.FLOOR_Y + 1, Biathlon.afterRange()[1], -90f, 0f));
            run.px = Biathlon.afterRange()[0];
            run.pz = Biathlon.afterRange()[1];
            return;
        }
        BiathlonProgress pr = run.progress;
        if (pr.phase() == BiathlonProgress.Phase.PENALTY) {
            if (Biathlon.penaltyTurn().cross(px, pz, qx, qz) != null) {
                pr.penaltyTurn();
            }
            if (Biathlon.penaltyLine().cross(px, pz, qx, qz) != null && pr.penaltyLine()) {
                Ui.sound(p, "minecraft:block.note_block.chime", 1.2f);
                if (pr.phase() == BiathlonProgress.Phase.RUNNING) {
                    p.sendMessage(Texts.msg(plugin.lang(p), "biathlon.penalty.done"));
                    run.pilot.clear();
                    double[] a = Biathlon.afterRange();
                    p.teleport(new Location(plugin.world(), a[0], Terrain.FLOOR_Y + 1, a[1], -90f, 0f));
                    run.px = a[0];
                    run.pz = a[1];
                }
            }
            return;
        }
        List<Line> cps = Biathlon.checkpoints();
        for (int i = 0; i < cps.size(); i++) {
            if (cps.get(i).cross(px, pz, qx, qz) != null && pr.checkpoint(i)) {
                Ui.sound(p, "minecraft:block.note_block.hat", 1.5f);
            }
        }
        Line.Hit hit = Biathlon.lapLine().cross(px, pz, qx, qz);
        if (hit == null) {
            return;
        }
        switch (pr.lapLine()) {
            case LAP -> p.sendMessage(Texts.msg(plugin.lang(p), "biathlon.lap", Placeholder.unparsed("n", String.valueOf(pr.lap()))));
            case SHOOT -> {
                p.sendMessage(Texts.msg(plugin.lang(p), "biathlon.lap", Placeholder.unparsed("n", String.valueOf(pr.lap()))));
                toRange(run, p, tick);
            }
            case FINISH -> finish(run, p, clock.at(tick, hit.t()));
            default -> {
            }
        }
    }

    private Location mat(Runner run) {
        return new Location(plugin.world(), Biathlon.laneX(run.lane) + 0.5, Terrain.FLOOR_Y + 1.07, Biathlon.MAT_Z + 0.5, 180f, 0f);
    }

    private void toRange(Runner run, Player p, long tick) {
        resetTargets(run.lane);
        p.teleport(mat(run));
        run.onMat = true;
        run.shootUntil = tick + plugin.settings().shootSeconds() * (long) TICKS;
        run.lastShot = -1;
        run.arrowsOut = 0;
        String lang = plugin.lang(p);
        ItemStack bow = Items.kitTag(Items.named(Material.BOW, 1, Texts.text(lang, "item.bow"), Texts.lines(lang, "item.bow.lore")), "bow");
        bow.editMeta(m -> m.setUnbreakable(true));
        p.getInventory().setItem(SLOT_BOW, bow);
        p.getInventory().setItem(SLOT_ARROWS, new ItemStack(Material.ARROW, Biathlon.TARGETS));
        p.getInventory().setHeldItemSlot(SLOT_BOW);
        Ui.title(p, Texts.text(lang, "biathlon.shoot.title"), Texts.text(lang, "biathlon.shoot.sub"), 2000);
        Ui.sound(p, "minecraft:block.wooden_door.open", 1f);
    }

    /** Am Schießstand: auf der Matte halten, nach fünf Schuss (oder Zeit um) weiter. */
    private void shooting(Runner run, Player p, long tick) {
        Location m = mat(run);
        Location l = p.getLocation();
        if (l.getWorld() != m.getWorld() || Math.hypot(l.getX() - m.getX(), l.getZ() - m.getZ()) > MAT_LEASH) {
            m.setYaw(l.getYaw());
            m.setPitch(l.getPitch());
            p.teleport(m);
        }
        BiathlonProgress pr = run.progress;
        boolean allIn = pr.allShot() && run.arrowsOut == 0 && run.lastShot >= 0 && tick - run.lastShot >= AFTER_LAST_SHOT;
        if (allIn || tick >= run.shootUntil) {
            endShooting(run, p);
        }
    }

    private void endShooting(Runner run, Player p) {
        BiathlonProgress pr = run.progress;
        int hits = pr.roundHits();
        int penalty = pr.endShooting();
        run.onMat = false;
        arrows.values().removeIf(s -> s.runner == run);
        p.getInventory().setItem(SLOT_BOW, null);
        p.getInventory().remove(Material.ARROW);
        String lang = plugin.lang(p);
        String extra = penalty > 0 ? Texts.raw(lang, "biathlon.result.penalty").replace("<n>", String.valueOf(penalty))
                : Texts.raw(lang, "biathlon.result.clean");
        p.sendMessage(Texts.msg(lang, "biathlon.result", Placeholder.unparsed("hits", String.valueOf(hits)),
                Placeholder.unparsed("total", String.valueOf(Biathlon.TARGETS)), Placeholder.parsed("extra", extra)));
        run.pilot.clear();
        double[] to = penalty > 0 ? Biathlon.penaltyStart() : Biathlon.afterRange();
        p.teleport(new Location(plugin.world(), to[0], Terrain.FLOOR_Y + 1, to[1], -90f, 0f));
        run.px = to[0];
        run.pz = to[1];
        Ui.sound(p, penalty > 0 ? "minecraft:block.note_block.bass" : "minecraft:entity.player.levelup", 1f);
    }

    private void finish(Runner run, Player p, long ms) {
        run.finished = true;
        run.timeMs = ms;
        String lang = plugin.lang(p);
        String time = TimeFormat.format(ms, lang);
        Ui.title(p, Texts.text(lang, "race.finish.title"), Texts.text(lang, "race.finish.sub", Placeholder.unparsed("time", time)), 2000);
        p.sendMessage(Texts.msg(lang, "race.finish.msg", Placeholder.unparsed("time", time), Placeholder.parsed("extra", "")));
        Ui.sound(p, "minecraft:entity.player.levelup", 1.4f);
        plugin.world().spawnParticle(Particle.FIREWORK, p.getLocation().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.05);
        toStand(run, p);
    }

    private void toStand(Runner run, Player p) {
        p.removePotionEffect(PotionEffectType.SPEED);
        freeze(p, false);
        comp.dress(p, run.racer, false);
        p.teleport(view());
    }

    // ---------- Pfeile ----------

    /** Ein Pfeil wurde abgeschossen. @return true, wenn er zu einem Schießstand gehört */
    public boolean shot(Player p, AbstractArrow arrow) {
        Runner run = runnerOf(p);
        if (run == null || run.progress.phase() != BiathlonProgress.Phase.SHOOTING) {
            return false;
        }
        arrow.setGravity(false);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setPersistent(false);
        if (arrow instanceof Arrow a) {
            a.setCritical(false);
        }
        arrows.put(arrow.getUniqueId(), new Shot(run, nowTick));
        run.arrowsOut++;
        run.lastShot = nowTick;
        return true;
    }

    /** Ein Pfeil hat etwas getroffen (Block oder nichts Bestimmtes). */
    public void hit(AbstractArrow arrow, Block block) {
        Shot s = arrows.remove(arrow.getUniqueId());
        arrow.remove();
        if (s == null) {
            return;
        }
        Runner run = s.runner;
        run.arrowsOut = Math.max(0, run.arrowsOut - 1);
        boolean hit = false;
        if (block != null && block.getType() == Material.BLACK_CONCRETE && block.getZ() == Biathlon.TARGET_Z
                && block.getY() == Terrain.FLOOR_Y + Biathlon.TARGET_Y) {
            for (int t = 0; t < Biathlon.TARGETS; t++) {
                if (block.getX() == Biathlon.targetX(run.lane, t)) {
                    hit = true;
                }
            }
        }
        if (hit) {
            block.setType(Material.WHITE_CONCRETE, false);
            plugin.world().spawnParticle(Particle.SNOWFLAKE, block.getLocation().add(0.5, 0.5, 1.1), 12, 0.2, 0.2, 0.1, 0.02);
        }
        run.progress.shot(hit);
        Player p = run.racer.player();
        if (p != null) {
            String lang = plugin.lang(p);
            p.sendActionBar(Texts.text(lang, hit ? "biathlon.hit" : "biathlon.miss"));
            Ui.sound(p, hit ? "minecraft:block.note_block.bell" : "minecraft:block.note_block.didgeridoo", hit ? 1.6f : 0.8f);
        }
    }

    private void expireArrows(long tick) {
        List<UUID> old = new ArrayList<>();
        arrows.forEach((id, s) -> {
            if (tick - s.tick >= ARROW_LIFE) {
                old.add(id);
            }
        });
        for (UUID id : old) {
            Shot s = arrows.remove(id);
            var e = plugin.getServer().getEntity(id);
            if (e != null) {
                e.remove();
            }
            s.runner.arrowsOut = Math.max(0, s.runner.arrowsOut - 1);
            s.runner.progress.shot(false);
        }
    }

    public boolean isOurs(AbstractArrow arrow) {
        return arrows.containsKey(arrow.getUniqueId());
    }

    private Runner runnerOf(Player p) {
        for (Runner run : runners) {
            if (run.racer.uuid().equals(p.getUniqueId())) {
                return run;
            }
        }
        return null;
    }

    /** Läuft man gerade (Hunger aus, kein Hinausfallen)? */
    public boolean running(Player p) {
        Runner run = runnerOf(p);
        return run != null && run.active();
    }

    @Override
    public void second() {
        if (phase != Phase.RUN) {
            return;
        }
        for (Runner run : runners) {
            if (!run.active()) {
                continue;
            }
            Player p = run.racer.player();
            if (p == null) {
                continue;
            }
            String lang = plugin.lang(p);
            BiathlonProgress pr = run.progress;
            String time = TimeFormat.format(clock.now(nowTick), lang);
            switch (pr.phase()) {
                case SHOOTING -> p.sendActionBar(Texts.text(lang, "biathlon.bar.shoot", Placeholder.unparsed("time", time),
                        Placeholder.unparsed("n", String.valueOf(Math.min(Biathlon.TARGETS, pr.shotsFired() + 1))),
                        Placeholder.unparsed("total", String.valueOf(Biathlon.TARGETS)),
                        Placeholder.unparsed("secs", String.valueOf(Math.max(0, (run.shootUntil - nowTick) / TICKS)))));
                case PENALTY -> p.sendActionBar(Texts.text(lang, "biathlon.bar.penalty", Placeholder.unparsed("time", time),
                        Placeholder.unparsed("n", String.valueOf(pr.penaltyLeft()))));
                default -> p.sendActionBar(Texts.text(lang, "biathlon.bar.run", Placeholder.unparsed("time", time),
                        Placeholder.unparsed("n", String.valueOf(Math.min(pr.laps(), pr.lap() + 1))),
                        Placeholder.unparsed("total", String.valueOf(pr.laps())), Placeholder.unparsed("hits", String.valueOf(pr.hits()))));
            }
        }
        plugin.boards().live(Discipline.BIATHLON, results(), 1, 1);
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public List<Result> results() {
        List<Result> out = new ArrayList<>();
        for (Runner run : runners) {
            out.add(new Result(List.of(run.racer), run.finished ? run.timeMs : 0, run.finished, 0, run.progress.hits(),
                    run.progress.shootingsDone() * Biathlon.TARGETS));
        }
        return out;
    }

    @Override
    public void quit(Racer r) {
        for (Runner run : runners) {
            if (run.racer == r && run.active()) {
                run.dnf = true;
                Player p = r.player();
                if (p != null) {
                    p.removePotionEffect(PotionEffectType.SPEED);
                    freeze(p, false);
                }
            }
        }
    }

    /** Verbindung weg: der Stand (Runde, Treffer, Strafrunden) bleibt, Pfeile in der Luft zählen nicht mehr. */
    @Override
    public void away(Racer r) {
        Runner run = runnerOf(r);
        if (run != null) {
            run.pilot.clear();
            arrows.values().removeIf(s -> s.runner == run);
            run.arrowsOut = 0;
        }
    }

    /**
     * Rechtzeitig zurück: vor dem Start an die eigene Startbahn, im Rennen genau dort weiter, wo man war (Minecraft hat
     * den Ort gespeichert; am Schießstand zurück auf die Matte) – Runde, Treffer und Strafrunden zählen weiter.
     */
    @Override
    public void rejoin(Racer r, Player p) {
        Runner run = runnerOf(r);
        if (run == null || !run.active() || phase == Phase.OVER) {
            if (run != null) {
                toStand(run, p);
            } else {
                comp.dress(p, r, false);
                p.teleport(view());
            }
            return;
        }
        if (phase != Phase.RUN || !run.placed) {
            comp.dress(p, r, false);
            double[] g = Biathlon.grid(run.lane);
            p.teleport(new Location(plugin.world(), g[0], Terrain.FLOOR_Y + 1, g[1], -90f, 0f));
            run.px = g[0];
            run.pz = g[1];
            run.placed = true;
            if (phase != Phase.RUN) {
                freeze(p, true);
                return;
            }
        }
        freeze(p, false);
        int amp = plugin.settings().runSpeed() - 1;
        if (amp >= 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, amp, false, false, true));
        }
        if (run.progress.phase() == BiathlonProgress.Phase.SHOOTING) {
            p.teleport(mat(run));
        }
        Location l = p.getLocation();
        run.px = l.getX();
        run.pz = l.getZ();
    }

    @Override
    public void giveUp(Racer r) {
        for (Runner run : runners) {
            if (run.racer == r && run.active()) {
                run.dnf = true;
                Player p = r.player();
                if (p != null) {
                    toStand(run, p);
                }
            }
        }
    }

    @Override
    public void cleanup() {
        for (UUID id : arrows.keySet()) {
            var e = plugin.getServer().getEntity(id);
            if (e != null) {
                e.remove();
            }
        }
        arrows.clear();
        for (Runner run : runners) {
            Player p = run.racer.player();
            if (p != null) {
                p.removePotionEffect(PotionEffectType.SPEED);
                freeze(p, false);
            }
        }
        setStartGate(false);
        for (int k = 0; k < Biathlon.LANES; k++) {
            resetTargets(k);
        }
        plugin.boards().live(Discipline.BIATHLON, List.of(), 0, 0);
    }

    @Override
    public Location view() {
        int[] s = Places.STAND_BIATHLON;
        return new Location(plugin.world(), s[0] + 0.5, Terrain.FLOOR_Y + 3, s[1] + 4.5, 180f, -10f);
    }

    @Override
    public String status(Racer r, String lang) {
        Runner run = r == null ? null : runnerOf(r);
        if (run == null || !run.active()) {
            return "";
        }
        return Texts.raw(lang, "sidebar.time").replace("<time>", TimeFormat.format(clock == null ? 0 : clock.now(nowTick), lang));
    }

    private Runner runnerOf(Racer r) {
        for (Runner run : runners) {
            if (run.racer == r) {
                return run;
            }
        }
        return null;
    }

    @Override
    public void reset(Player p) {
        Runner run = runnerOf(p);
        if (run != null && run.active() && run.progress.phase() != BiathlonProgress.Phase.SHOOTING) {
            p.sendMessage(Texts.msg(plugin.lang(p), "race.reset"));
        }
    }

    /** Zum Testen: Läufer entlang der Loipe oder der Strafrunde versetzen (Schritt für Schritt). */
    public List<double[]> lapPoints(Player p, boolean penalty) {
        List<double[]> out = new ArrayList<>();
        Runner run = runnerOf(p);
        if (run == null) {
            return out;
        }
        Path path = penalty ? Biathlon.penaltyLoop() : Biathlon.loop();
        Location l = p.getLocation();
        int from = path.nearest(l.getX(), l.getZ(), -1);
        int n = path.samples().size();
        for (int k = 0; k <= n + PILOT_EXTRA; k += 3) {
            Path.Sample s = path.samples().get((from + k) % n);
            out.add(new double[] {s.x(), s.z()});
        }
        return out;
    }

    /** Zum Testen: Wegpunkte setzen (je Tick höchstens {@link #PILOT_STEP} Blöcke). */
    public boolean walk(Player p, List<double[]> points) {
        Runner run = runnerOf(p);
        if (run == null || !run.active() || phase != Phase.RUN) {
            return false;
        }
        run.pilot.clear();
        run.pilot.addAll(points);
        return true;
    }

    private void walk(Runner run, Player p) {
        if (run.pilot.isEmpty()) {
            return;
        }
        Location l = p.getLocation();
        double budget = PILOT_STEP;
        double x = l.getX();
        double z = l.getZ();
        while (budget > 0 && !run.pilot.isEmpty()) {
            double[] w = run.pilot.peekFirst();
            double d = Math.hypot(w[0] - x, w[1] - z);
            if (d <= budget) {
                x = w[0];
                z = w[1];
                budget -= d;
                run.pilot.pollFirst();
            } else {
                x += (w[0] - x) / d * budget;
                z += (w[1] - z) / d * budget;
                budget = 0;
            }
        }
        p.teleport(new Location(l.getWorld(), x, Terrain.FLOOR_Y + 1, z, Path.yaw(x - l.getX(), z - l.getZ()), 0f));
    }

    @Override
    public List<String> debug() {
        List<String> out = new ArrayList<>();
        out.add("stage=biathlon phase=" + phase + " done=" + done);
        for (Runner run : runners) {
            BiathlonProgress pr = run.progress;
            Location l = run.racer.player() != null ? run.racer.player().getLocation() : null;
            out.add("b=" + run.racer.name() + " lane=" + (run.lane + 1) + " state=" + (run.finished ? "finished" : run.dnf ? "dnf"
                    : pr.phase().name().toLowerCase()) + " lap=" + pr.lap() + " cp=" + pr.nextCheckpoint() + " hits=" + pr.hits()
                    + " misses=" + pr.misses() + " pen=" + pr.penaltyLeft() + " shots=" + pr.shotsFired() + " out=" + run.arrowsOut
                    + " time=" + run.timeMs
                    + (l == null ? "" : String.format(java.util.Locale.ROOT, " x=%.1f y=%.1f z=%.1f", l.getX(), l.getY(), l.getZ())));
        }
        return out;
    }
}
