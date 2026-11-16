package net.morni.wintersport;

import net.morni.wintersport.data.Leaderboard;
import net.morni.wintersport.data.PlayerData;
import net.morni.wintersport.game.BiathlonRace;
import net.morni.wintersport.game.BoatRace;
import net.morni.wintersport.game.Competition;
import net.morni.wintersport.logic.Discipline;
import net.morni.wintersport.logic.Program;
import net.morni.wintersport.logic.TimeFormat;
import net.morni.wintersport.village.VillageMenus;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Alle Befehle. Einstellungen gibt es hier keine (die stehen in der config.yml, später im Panel). */
final class Commands implements TabExecutor {

    private static final List<String> ADMIN_SUBS = List.of("info", "status", "start", "stopp", "neubauen", "neuladen",
            "fahren", "laufen", "testdatum");

    private final WintersportPlugin plugin;

    Commands(WintersportPlugin plugin) {
        this.plugin = plugin;
    }

    private String lang(CommandSender s) {
        return s instanceof Player p ? plugin.lang(p) : "de";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("wsadmin")) {
            return admin(sender, args);
        }
        if (name.equals("stats")) {
            stats(sender, args);
            return true;
        }
        if (name.equals("top")) {
            top(sender, args);
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.msg("de", "only-players"));
            return true;
        }
        switch (name) {
            case "bereit" -> plugin.village().toggleReady(p);
            case "abstimmen" -> {
                Program prog = args.length > 0 ? Program.byId(args[0]) : null;
                if (prog == null || !plugin.village().inVillage(p.getUniqueId()) || !plugin.open()) {
                    VillageMenus.vote(plugin, p);
                } else {
                    plugin.village().vote(p, prog);
                }
            }
            case "zuschauen" -> {
                if (plugin.games().of(p.getUniqueId()) == null) {
                    plugin.games().spectate(p);
                }
            }
            case "verlassen" -> {
                Competition c = plugin.games().of(p.getUniqueId());
                if (c != null) {
                    c.leave(p);
                    p.sendMessage(Texts.msg(plugin.lang(p), "leave.done"));
                } else {
                    plugin.fun().leave(p);
                    plugin.lift().unseat(p);
                    plugin.village().enter(p);
                    p.sendMessage(Texts.msg(plugin.lang(p), "leave.nothing"));
                }
            }
            default -> {
            }
        }
        return true;
    }

    private void stats(CommandSender sender, String[] args) {
        String lang = lang(sender);
        Optional<PlayerData> data;
        if (args.length > 0) {
            Player online = Bukkit.getPlayerExact(args[0]);
            UUID uuid = online != null ? online.getUniqueId() : offlineUuid(args[0]);
            data = uuid == null ? Optional.empty() : plugin.store().read(uuid);
            if (data.isEmpty()) {
                sender.sendMessage(Texts.msg(lang, "unknown-player", Placeholder.unparsed("name", args[0])));
                return;
            }
        } else if (sender instanceof Player p) {
            data = Optional.of(plugin.data(p));
        } else {
            sender.sendMessage(Texts.msg(lang, "only-players"));
            return;
        }
        PlayerData d = data.get();
        sender.sendMessage(Texts.text(lang, "stats.header", Placeholder.unparsed("name", d.name())));
        if (d.races() == 0) {
            sender.sendMessage(Texts.text(lang, "stats.none"));
            return;
        }
        sender.sendMessage(Texts.text(lang, "stats.medals", Placeholder.unparsed("ng", String.valueOf(d.gold())),
                Placeholder.unparsed("ns", String.valueOf(d.silver())), Placeholder.unparsed("nb", String.valueOf(d.bronze())),
                Placeholder.unparsed("points", String.valueOf(d.points()))));
        sender.sendMessage(Texts.text(lang, "stats.events", Placeholder.unparsed("events", String.valueOf(d.events())),
                Placeholder.unparsed("races", String.valueOf(d.races())), Placeholder.unparsed("wins", String.valueOf(d.wins()))));
        for (Discipline disc : Discipline.values()) {
            if (d.best(disc) > 0) {
                sender.sendMessage(Texts.text(lang, "stats.best", Placeholder.unparsed("discipline",
                        Texts.plain(lang, "discipline." + disc.id())), Placeholder.unparsed("time", TimeFormat.format(d.best(disc), lang))));
            }
        }
        if (d.shots() > 0) {
            sender.sendMessage(Texts.text(lang, "stats.hits", Placeholder.unparsed("hits", String.valueOf(d.hits())),
                    Placeholder.unparsed("shots", String.valueOf(d.shots()))));
        }
    }

    private static UUID offlineUuid(String name) {
        @SuppressWarnings("deprecation")
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        return op == null ? null : op.getUniqueId();
    }

    private void top(CommandSender sender, String[] args) {
        String lang = lang(sender);
        List<Discipline> which = args.length > 0 && Discipline.byId(args[0]) != null ? List.of(Discipline.byId(args[0]))
                : Arrays.asList(Discipline.values());
        for (Discipline d : which) {
            sender.sendMessage(Texts.text(lang, "top.header", Placeholder.unparsed("discipline", Texts.plain(lang, "discipline." + d.id()))));
            List<Leaderboard.Time> top = plugin.board().top(d);
            if (top.isEmpty()) {
                sender.sendMessage(Texts.text(lang, "board.empty"));
            }
            for (int i = 0; i < top.size(); i++) {
                sender.sendMessage(Texts.text(lang, "board.times.row", Placeholder.unparsed("place", String.valueOf(i + 1)),
                        Placeholder.unparsed("name", top.get(i).name()), Placeholder.unparsed("time", TimeFormat.format(top.get(i).ms(), lang))));
            }
        }
    }

    // ---------- Verwaltung (zum Testen) ----------

    private boolean admin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/wsadmin " + String.join("|", ADMIN_SUBS)));
            return true;
        }
        Competition c = plugin.games().current();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> sender.sendMessage(Component.text("Wintersport " + plugin.getPluginMeta().getVersion()
                    + " · bereit=" + plugin.ready() + " · Saison=" + plugin.settings().season().id() + " · offen=" + plugin.open()
                    + " · Netzwerk=" + plugin.net().connected()));
            case "status" -> {
                List<String> lines = new ArrayList<>();
                lines.add("bereit=" + plugin.ready() + " offen=" + plugin.open() + " dorf=" + plugin.village().waiting().size()
                        + " countdown=" + plugin.village().countdown() + " naechstes=" + plugin.village().next().id()
                        + " laeuft=" + (c != null) + " zeit=" + plugin.world().getTime());
                if (c != null) {
                    lines.addAll(c.debug());
                }
                sender.sendMessage(Component.text(String.join("\n", lines)));
            }
            case "start" -> {
                Program prog = args.length > 1 ? Program.byId(args[1]) : plugin.village().next();
                if (prog == null || !plugin.ready() || !plugin.open() || !plugin.village().start(prog)) {
                    sender.sendMessage(Component.text("Kein Start (läuft schon, nicht bereit, geschlossen oder niemand da)."));
                } else {
                    sender.sendMessage(Component.text("Gestartet: " + prog.id()));
                }
            }
            case "stopp" -> {
                plugin.games().abort();
                sender.sendMessage(Component.text("Wettkampf abgebrochen."));
            }
            case "neubauen" -> {
                sender.sendMessage(Component.text("Baue neu …"));
                plugin.rebuild(() -> sender.sendMessage(Component.text("Neu gebaut.")));
            }
            case "neuladen" -> {
                plugin.reloadSettings();
                sender.sendMessage(Component.text("config.yml neu geladen · Saison=" + plugin.settings().season().id()
                        + " · offen=" + plugin.open()));
            }
            case "testdatum" -> {
                if (args.length < 2 || args[1].equalsIgnoreCase("aus")) {
                    plugin.testDate(null);
                } else {
                    try {
                        plugin.testDate(LocalDate.parse(args[1]));
                    } catch (DateTimeParseException e) {
                        sender.sendMessage(Component.text("Datum wie 2027-01-15"));
                        return true;
                    }
                }
                plugin.stateChanged();
                sender.sendMessage(Component.text("Heute=" + plugin.today() + " · offen=" + plugin.open()));
            }
            case "fahren" -> drive(sender, c, args);
            case "laufen" -> walk(sender, c, args);
            default -> sender.sendMessage(Component.text("/wsadmin " + String.join("|", ADMIN_SUBS)));
        }
        return true;
    }

    /** /wsadmin fahren <spieler> tor <n> [daneben] | ziel | weiter <blöcke> – Boot selbst fahren lassen (Tests). */
    private void drive(CommandSender sender, Competition c, String[] args) {
        if (args.length < 3 || c == null || !(c.stage() instanceof BoatRace race)) {
            sender.sendMessage(Component.text("Kein Rennen im Boot."));
            return;
        }
        Player p = Bukkit.getPlayerExact(args[1]);
        if (p == null) {
            sender.sendMessage(Component.text("Unbekannt: " + args[1]));
            return;
        }
        List<double[]> points = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "tor" -> args.length > 3 ? race.gatePoints(p, Integer.parseInt(args[3]), args.length > 4) : List.of();
            case "ziel" -> race.finishPoints(p);
            case "weiter" -> race.aheadPoints(p, args.length > 3 ? Double.parseDouble(args[3]) : 20);
            default -> List.of();
        };
        boolean ok = !points.isEmpty() && race.drive(p, points);
        sender.sendMessage(Component.text(ok ? "fährt " + points.size() + " Punkte" : "geht nicht"));
    }

    /** /wsadmin laufen <spieler> runde|strafrunde – Läufer selbst laufen lassen (Tests). */
    private void walk(CommandSender sender, Competition c, String[] args) {
        if (args.length < 3 || c == null || !(c.stage() instanceof BiathlonRace race)) {
            sender.sendMessage(Component.text("Kein Biathlon."));
            return;
        }
        Player p = Bukkit.getPlayerExact(args[1]);
        if (p == null) {
            sender.sendMessage(Component.text("Unbekannt: " + args[1]));
            return;
        }
        List<double[]> points = race.lapPoints(p, args[2].toLowerCase(Locale.ROOT).startsWith("straf"));
        boolean ok = !points.isEmpty() && race.walk(p, points);
        sender.sendMessage(Component.text(ok ? "läuft " + points.size() + " Punkte" : "geht nicht"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("wsadmin") && args.length == 1) {
            return filter(ADMIN_SUBS, args[0]);
        }
        if (name.equals("wsadmin") && args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filter(Arrays.stream(Program.values()).map(Program::id).toList(), args[1]);
        }
        if (name.equals("abstimmen") && args.length == 1) {
            return filter(Arrays.stream(Program.values()).map(Program::id).toList(), args[0]);
        }
        if (name.equals("top") && args.length == 1) {
            return filter(Arrays.stream(Discipline.values()).map(Discipline::id).toList(), args[0]);
        }
        if (name.equals("stats") && args.length == 1) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
