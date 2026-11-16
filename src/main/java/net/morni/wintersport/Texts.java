package net.morni.wintersport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <em>} Smaragd,
 * {@code <au>} Gold, {@code <bl>} Eisblau, {@code <si>} Silber, {@code <bz>} Bronze. Namen von Spielern kommen immer
 * als {@code Placeholder.unparsed} hinein.
 */
public final class Texts {

    public static final int EMERALD = 0x2BB673;
    public static final int GOLD = 0xF2B632;
    public static final int BLUE = 0x8FD3FF;
    public static final int SILVER = 0xC8D0D8;
    public static final int BRONZE = 0xD08A4A;

    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE))),
                    TagResolver.resolver("si", Tag.styling(TextColor.color(SILVER))),
                    TagResolver.resolver("bz", Tag.styling(TextColor.color(BRONZE)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<bl>❄</bl> <em>Wintersport</em> <dark_gray>»</dark_gray> ", "<bl>❄</bl> <em>Winter Games</em> <dark_gray>»</dark_gray> ");
        t("mode", "Wintersport", "Winter Games");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("not-ready", "<gray>Das Bergdorf wird gerade gebaut – gleich geht's los!</gray>",
                "<gray>The mountain village is being built – just a moment!</gray>");
        t("unknown-player", "<gray>Niemanden namens <white><name></white> gefunden.</gray>",
                "<gray>Nobody called <white><name></white> found.</gray>");
        t("lobby.kick", "<au>morni.net</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit morni.net.</gray>",
                "<au>morni.net</au>\n<gray>Back to the lobby: please reconnect to morni.net.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – bis bald!</gray>", "<gray>Off to the lobby – see you soon!</gray>");

        // ---------- Dorf ----------
        t("village.title", "<bl>❄ Wintersport</bl>", "<bl>❄ Winter Games</bl>");
        t("village.subtitle", "<gray>Slalom · Biathlon · Bob – um Gold, Silber und Bronze!</gray>",
                "<gray>Slalom · biathlon · bobsleigh – go for gold!</gray>");
        t("village.welcome", "<gray>Willkommen im <bl>Bergdorf</bl>! Mit dem <white>Kompass</white> stimmst du ab, was als Nächstes kommt, mit dem <white>grünen Farbstoff</white> sagst du „bereit“.</gray>",
                "<gray>Welcome to the <bl>mountain village</bl>! Use the <white>compass</white> to vote for what comes next and the <white>green dye</white> to say you're ready.</gray>");
        t("village.how", "<gray>So geht's: Im <em>Wettkampf</em> fahrt ihr <white>Riesenslalom</white>, lauft <white>Biathlon</white> und rast die <white>Bobbahn</white> hinunter. Die Besten jeder Disziplin bekommen <au>Gold</au>, <si>Silber</si> und <bz>Bronze</bz>, Punkte entscheiden den Gesamtsieg. Bis es losgeht: Eisfläche, Rodelbahn und Sessellift!</gray>",
                "<gray>How it works: in the <em>competition</em> you race the <white>giant slalom</white>, run the <white>biathlon</white> and speed down the <white>bobsleigh track</white>. The best of each event win <au>gold</au>, <si>silver</si> and <bz>bronze</bz>, points decide the overall winner. Until then: ice rink, toboggan run and chairlift!</gray>");
        t("village.waiting", "<gray>Warte auf Mitspieler … (<white><n></white> da)</gray>", "<gray>Waiting for players … (<white><n></white> here)</gray>");
        t("village.countdown", "<gray>Start in <au><secs> s</au> · <bl><program></bl></gray>",
                "<gray>Starting in <au><secs> s</au> · <bl><program></bl></gray>");
        t("village.busy", "<gray>Gerade läuft ein Wettkampf – mit dem Kompass kannst du zuschauen.</gray>",
                "<gray>A competition is running – use the compass to watch.</gray>");
        t("village.ready", "<em>Du bist bereit.</em> <gray>Sind alle bereit, geht's gleich los.</gray>",
                "<em>You're ready.</em> <gray>Once everyone is ready, we start.</gray>");
        t("village.not-ready", "<gray>Du bist nicht mehr bereit.</gray>", "<gray>You're no longer ready.</gray>");
        t("village.voted", "<gray>Deine Stimme: <bl><program></bl>.</gray>", "<gray>Your vote: <bl><program></bl>.</gray>");
        t("village.full", "<gray>Der Wettkampf ist voll (höchstens 12) – du bist beim nächsten dabei.</gray>",
                "<gray>The competition is full (12 at most) – you're in the next one.</gray>");
        t("village.starting", "<em>Auf geht's!</em> <gray><bl><program></bl> mit <white><n></white> Leuten.</gray>",
                "<em>Here we go!</em> <gray><bl><program></bl> with <white><n></white> people.</gray>");
        t("village.closed", "<gray>Die Winterspiele machen gerade <bl>Sommerpause</bl> – ab dem <white><date></white> geht es wieder los. Das Dorf, die Eisfläche und die Rodelbahn sind trotzdem offen!</gray>",
                "<gray>The Winter Games are on their <bl>summer break</bl> – they're back on <white><date></white>. The village, ice rink and toboggan run are still open!</gray>");
        t("village.closed.title", "<bl>❄ Sommerpause</bl>", "<bl>❄ Summer break</bl>");
        t("village.closed.sub", "<gray>Wieder ab <white><date></white></gray>", "<gray>Back on <white><date></white></gray>");
        t("village.closed.bar", "<gray>Sommerpause – Wettkämpfe wieder ab <white><date></white></gray>",
                "<gray>Summer break – competitions again from <white><date></white></gray>");

        t("item.vote", "<em>✎ Abstimmen & Zuschauen</em>", "<em>✎ Vote & watch</em>");
        t("item.vote.lore", "<gray>Rechtsklick: Wettkampf oder eine Disziplin wählen</gray>\n<gray>oder einem laufenden Wettkampf zuschauen.</gray>",
                "<gray>Right-click: choose the competition or one event</gray>\n<gray>or watch a running competition.</gray>");
        t("item.ready", "<em>✔ Bereit</em>", "<em>✔ Ready</em>");
        t("item.ready.lore", "<gray>Rechtsklick: bereit / nicht bereit.</gray>", "<gray>Right-click: ready / not ready.</gray>");
        t("item.stats", "<au>★ Medaillen & Bestzeiten</au>", "<au>★ Medals & best times</au>");
        t("item.stats.lore", "<gray>Rechtsklick: deine Medaillen und die Besten.</gray>",
                "<gray>Right-click: your medals and the best.</gray>");
        t("item.lobby", "<bl>⌂ Zurück zur Lobby</bl>", "<bl>⌂ Back to the lobby</bl>");
        t("item.lobby.lore", "<gray>Rechtsklick: zurück ins Netzwerk-Dorf.</gray>", "<gray>Right-click: back to the network village.</gray>");
        t("item.reset", "<bl>⟲ Zurück auf die Strecke</bl>", "<bl>⟲ Back on track</bl>");
        t("item.reset.lore", "<gray>Rechtsklick: zurück zum letzten Tor bzw. Kontrollpunkt,</gray>\n<gray>falls du feststeckst. Die Zeit läuft weiter.</gray>",
                "<gray>Right-click: back to the last gate or checkpoint</gray>\n<gray>if you're stuck. The clock keeps running.</gray>");
        t("item.giveup", "<red>✖ Aufgeben</red>", "<red>✖ Give up</red>");
        t("item.giveup.lore", "<gray>Rechtsklick: diese Disziplin aufgeben</gray>\n<gray>(im Wettkampf bleibst du dabei).</gray>",
                "<gray>Right-click: give up this event</gray>\n<gray>(you stay in the competition).</gray>");
        t("item.leave", "<red>✖ Zurück ins Dorf</red>", "<red>✖ Back to the village</red>");
        t("item.leave.lore", "<gray>Rechtsklick: Zuschauen beenden.</gray>", "<gray>Right-click: stop watching.</gray>");
        t("item.bow", "<au>➹ Biathlon-Bogen</au>", "<au>➹ Biathlon bow</au>");
        t("item.bow.lore", "<gray>Fünf Pfeile, fünf Scheiben – ganz spannen und zielen!</gray>\n<gray>Pfeile fliegen hier schnurgerade.</gray>",
                "<gray>Five arrows, five targets – draw fully and aim!</gray>\n<gray>Arrows fly perfectly straight here.</gray>");
        t("item.bib", "<white>Startnummer <au><n></au></white>", "<white>Bib number <au><n></au></white>");

        // ---------- Programme und Disziplinen ----------
        t("program.wettkampf", "Wettkampf (alle drei)", "Competition (all three)");
        t("program.wettkampf.desc", "<gray>Riesenslalom, Biathlon und Bobbahn nacheinander –</gray>\n<gray>Medaillen in jeder Disziplin, Punkte für den Gesamtsieg.</gray>",
                "<gray>Giant slalom, biathlon and bobsleigh in a row –</gray>\n<gray>medals in every event, points for the overall win.</gray>");
        t("discipline.slalom", "Riesenslalom", "Giant Slalom");
        t("discipline.slalom.desc", "<gray>Im Boot die Eispiste hinab durch zwölf Tore –</gray>\n<gray>rote links, blaue rechts. Verpasst = <white><secs> s</white> Strafe.</gray>",
                "<gray>Down the ice slope in a boat through twelve gates –</gray>\n<gray>red on the left, blue on the right. Missed = <white><secs> s</white> penalty.</gray>");
        t("discipline.biathlon", "Biathlon", "Biathlon");
        t("discipline.biathlon.desc", "<gray>Runden auf der Loipe, dazwischen fünf Scheiben</gray>\n<gray>am Schießstand – jeder Fehlschuss = eine Strafrunde.</gray>",
                "<gray>Laps on the ski trail, five targets at the range</gray>\n<gray>in between – every miss = one penalty loop.</gray>");
        t("discipline.bob", "Bobbahn", "Bobsleigh");
        t("discipline.bob.desc", "<gray>Im Bob durch den Eiskanal mit zwölf Kehren –</gray>\n<gray>Partys fahren zu zweit im Zweierbob.</gray>",
                "<gray>Through the ice channel with twelve bends –</gray>\n<gray>parties race together in the two-man bob.</gray>");

        // ---------- Menüs ----------
        t("menu.vote.title", "Was kommt als Nächstes?", "What's next?");
        t("menu.vote.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.vote.click", "<em>▶ Klicken zum Abstimmen</em>", "<em>▶ Click to vote</em>");
        t("menu.vote.mine", "<au>✔ Deine Stimme</au>", "<au>✔ Your vote</au>");
        t("menu.vote.best", "<gray>Bestzeit: <au><time></au> <white><name></white></gray>",
                "<gray>Best time: <au><time></au> <white><name></white></gray>");
        t("menu.vote.watch", "<bl>◉ Zuschauen</bl>", "<bl>◉ Watch</bl>");
        t("menu.vote.watch.lore", "<gray>Gerade läuft: <white><discipline></white></gray>\n<em>▶ Klicken zum Zuschauen</em>",
                "<gray>Now running: <white><discipline></white></gray>\n<em>▶ Click to watch</em>");
        t("menu.vote.closed", "<gray>Sommerpause – wieder ab <white><date></white>.</gray>",
                "<gray>Summer break – back on <white><date></white>.</gray>");
        t("menu.close", "<red>Schließen</red>", "<red>Close</red>");
        t("menu.stats.title", "Medaillen & Bestzeiten", "Medals & best times");
        t("menu.stats.me", "<au>Deine Medaillen</au>", "<au>Your medals</au>");
        t("menu.stats.me.lore", "<au>● <ng></au> Gold · <si>● <ns></si> Silber · <bz>● <nb></bz> Bronze\n<gray>Wettkämpfe: <white><events></white> · Gesamtsiege: <white><wins></white></gray>\n<gray>Punkte: <white><points></white></gray>",
                "<au>● <ng></au> gold · <si>● <ns></si> silver · <bz>● <nb></bz> bronze\n<gray>Competitions: <white><events></white> · overall wins: <white><wins></white></gray>\n<gray>Points: <white><points></white></gray>");
        t("menu.stats.best", "<gray>Deine Bestzeit: <au><time></au></gray>", "<gray>Your best time: <au><time></au></gray>");
        t("menu.stats.none", "<gray>Noch keine Zeit.</gray>", "<gray>No time yet.</gray>");
        t("menu.stats.top", "<white><place>.</white> <gray><name></gray> <au><time></au>", "<white><place>.</white> <gray><name></gray> <au><time></au>");
        t("menu.stats.hits", "<gray>Biathlon-Treffer: <white><hits></white> von <white><shots></white></gray>",
                "<gray>Biathlon hits: <white><hits></white> of <white><shots></white></gray>");
        t("menu.stats.table", "<au>Ewiger Medaillenspiegel</au>", "<au>All-time medal table</au>");

        // ---------- Wettkampf ----------
        t("comp.bib", "<gray>Deine Startnummer: <au><n></au>. Viel Glück!</gray>", "<gray>Your bib number: <au><n></au>. Good luck!</gray>");
        t("comp.next.title", "<bl><discipline></bl>", "<bl><discipline></bl>");
        t("comp.next.sub", "<gray>Disziplin <white><n></white> von <white><total></white></gray>", "<gray>Event <white><n></white> of <white><total></white></gray>");
        t("comp.result.header", "<au>━━ Ergebnis <discipline> ━━</au>", "<au>━━ Result <discipline> ━━</au>");
        t("comp.result.row", "<medal> <white><place>.</white> <name> <gray>–</gray> <au><time></au><extra>",
                "<medal> <white><place>.</white> <name> <gray>–</gray> <au><time></au><extra>");
        t("comp.result.dnf", "<gray>✖ <name> – nicht im Ziel</gray>", "<gray>✖ <name> – did not finish</gray>");
        t("comp.result.penalty", " <gray>(<white><n></white> Tor(e) verpasst)</gray>", " <gray>(<white><n></white> gate(s) missed)</gray>");
        t("comp.result.hits", " <gray>(<white><n></white>/<white><total></white> Treffer)</gray>", " <gray>(<white><n></white>/<white><total></white> hits)</gray>");
        t("comp.medal.gold", "<au>● GOLD!</au> <gray>Du gewinnst <white><discipline></white>!</gray>", "<au>● GOLD!</au> <gray>You win the <white><discipline></white>!</gray>");
        t("comp.medal.silver", "<si>● Silber!</si> <gray>Platz 2 im <white><discipline></white>.</gray>", "<si>● Silver!</si> <gray>2nd place in the <white><discipline></white>.</gray>");
        t("comp.medal.bronze", "<bz>● Bronze!</bz> <gray>Platz 3 im <white><discipline></white>.</gray>", "<bz>● Bronze!</bz> <gray>3rd place in the <white><discipline></white>.</gray>");
        t("comp.medal.title.gold", "<au>● Gold</au>", "<au>● Gold</au>");
        t("comp.medal.title.silver", "<si>● Silber</si>", "<si>● Silver</si>");
        t("comp.medal.title.bronze", "<bz>● Bronze</bz>", "<bz>● Bronze</bz>");
        t("comp.training", "<gray>Allein ist es ein <white>Training</white>: keine Medaille, aber die Zeit zählt für die Bestenliste.</gray>",
                "<gray>Alone it's <white>training</white>: no medal, but your time counts for the leaderboard.</gray>");
        t("comp.standings.header", "<bl>━━ Medaillenspiegel ━━</bl>", "<bl>━━ Medal table ━━</bl>");
        t("comp.standings.row", "<white><place>.</white> <name> <au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz> <gray>· <points> Pkt.</gray>",
                "<white><place>.</white> <name> <au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz> <gray>· <points> pts</gray>");
        t("comp.between", "<gray>Gleich geht es weiter mit <bl><discipline></bl> …</gray>", "<gray>Coming up next: <bl><discipline></bl> …</gray>");
        t("comp.winner", "<au>★ Gesamtsieg für <white><name></white> mit <points> Punkten!</au>", "<au>★ Overall winner: <white><name></white> with <points> points!</au>");
        t("comp.left", "<gray><white><name></white> hat den Wettkampf verlassen.</gray>", "<gray><white><name></white> left the competition.</gray>");
        t("comp.over.title", "<au>Siegerehrung</au>", "<au>Award ceremony</au>");
        t("comp.over.sub", "<gray>Glückwunsch an alle!</gray>", "<gray>Congratulations to everyone!</gray>");
        t("comp.podium.1", "<au>1</au>", "<au>1</au>");
        t("comp.podium.2", "<si>2</si>", "<si>2</si>");
        t("comp.podium.3", "<bz>3</bz>", "<bz>3</bz>");
        t("comp.back", "<gray>Zurück ins Dorf – bis zum nächsten Mal!</gray>", "<gray>Back to the village – see you next time!</gray>");
        t("comp.giveup", "<gray>Du hast aufgegeben – schau den anderen zu.</gray>", "<gray>You gave up – watch the others.</gray>");

        // ---------- Rennen (Boot) ----------
        t("slalom.intro", "<gray>Riesenslalom: <white>W</white> = Gas, <white>A</white>/<white>D</white> = lenken. Durch jedes Tor zwischen den Stangen – <red>rote</red> Tore links, <bl>blaue</bl> rechts. Verpasst = <white><secs> s</white> Strafe.</gray>",
                "<gray>Giant slalom: <white>W</white> = go, <white>A</white>/<white>D</white> = steer. Pass every gate between its poles – <red>red</red> gates on the left, <bl>blue</bl> on the right. Missed = <white><secs> s</white> penalty.</gray>");
        t("bob.intro", "<gray>Bobbahn: <white>W</white> = Gas, <white>A</white>/<white>D</white> = lenken – nicht zu wild in die Kurven! Partys fahren zu zweit, der Erste lenkt.</gray>",
                "<gray>Bobsleigh: <white>W</white> = go, <white>A</white>/<white>D</white> = steer – don't take the bends too wild! Parties ride together, the first one steers.</gray>");
        t("bob.pair", "<gray>Ihr fahrt im <bl>Zweierbob</bl>: <white><name></white> lenkt.</gray>", "<gray>You're in the <bl>two-man bob</bl>: <white><name></white> steers.</gray>");
        t("heat.title", "<bl>Lauf <n> von <total></bl>", "<bl>Heat <n> of <total></bl>");
        t("heat.your-turn", "<em>Du bist dran!</em> <gray>Bahn <white><lane></white> – gleich geht das Starttor auf.</gray>",
                "<em>Your turn!</em> <gray>Lane <white><lane></white> – the start gate opens in a moment.</gray>");
        t("heat.wait", "<gray>Du fährst im <white>Lauf <n></white> – schau bis dahin von der Tribüne zu.</gray>",
                "<gray>You race in <white>heat <n></white> – watch from the stand until then.</gray>");
        t("count.number", "<au><n></au>", "<au><n></au>");
        t("count.go", "<em>LOS!</em>", "<em>GO!</em>");
        t("count.sub", "<gray>Fertig machen …</gray>", "<gray>Get ready …</gray>");
        t("race.gate.missed", "<red>Tor verpasst!</red> <gray>+<secs> s</gray>", "<red>Gate missed!</red> <gray>+<secs> s</gray>");
        t("race.finish.title", "<em>Im Ziel!</em>", "<em>Finished!</em>");
        t("race.finish.sub", "<au><time></au>", "<au><time></au>");
        t("race.finish.msg", "<em>Im Ziel!</em> <gray>Zeit: <au><time></au><extra></gray>", "<em>Finished!</em> <gray>Time: <au><time></au><extra></gray>");
        t("race.finish.penalty", " <gray>(davon <white><secs> s</white> Strafe)</gray>", " <gray>(incl. <white><secs> s</white> penalty)</gray>");
        t("race.dnf", "<gray>Die Zeit ist um – leider <white>nicht im Ziel</white>.</gray>", "<gray>Time's up – <white>did not finish</white>.</gray>");
        t("race.reset", "<gray>Zurück auf die Strecke – die Zeit läuft weiter!</gray>", "<gray>Back on track – the clock keeps running!</gray>");
        t("race.bar.slalom", "<au><time></au> <dark_gray>·</dark_gray> <gray>Tor <white><n></white>/<white><total></white></gray><extra>",
                "<au><time></au> <dark_gray>·</dark_gray> <gray>Gate <white><n></white>/<white><total></white></gray><extra>");
        t("race.bar.penalty", " <dark_gray>·</dark_gray> <red>+<secs> s</red>", " <dark_gray>·</dark_gray> <red>+<secs> s</red>");
        t("race.bar.bob", "<au><time></au> <dark_gray>·</dark_gray> <gray>Kehre <white><n></white>/<white><total></white></gray>",
                "<au><time></au> <dark_gray>·</dark_gray> <gray>Bend <white><n></white>/<white><total></white></gray>");
        t("race.bar.watch", "<gray>Lauf <white><n></white>/<white><total></white> läuft · <white><left></white> noch auf der Strecke</gray>",
                "<gray>Heat <white><n></white>/<white><total></white> running · <white><left></white> still on course</gray>");

        // ---------- Biathlon ----------
        t("biathlon.intro", "<gray>Biathlon: <white><laps></white> Runden auf der Loipe (im Uhrzeigersinn, an den Fahnen entlang). Nach Runde 1 und 2 geht's an den <white>Schießstand</white>: fünf Schuss – jeder Fehler = eine <red>Strafrunde</red>.</gray>",
                "<gray>Biathlon: <white><laps></white> laps on the trail (clockwise, along the flags). After laps 1 and 2 you go to the <white>range</white>: five shots – every miss = one <red>penalty loop</red>.</gray>");
        t("biathlon.lap", "<gray>Runde <white><n></white> geschafft!</gray>", "<gray>Lap <white><n></white> done!</gray>");
        t("biathlon.shoot.title", "<au>Schießstand</au>", "<au>Shooting range</au>");
        t("biathlon.shoot.sub", "<gray>Bogen ganz spannen, auf die schwarzen Scheiben zielen</gray>",
                "<gray>Draw the bow fully, aim at the black targets</gray>");
        t("biathlon.hit", "<em>Treffer!</em>", "<em>Hit!</em>");
        t("biathlon.miss", "<red>Daneben!</red>", "<red>Miss!</red>");
        t("biathlon.result", "<gray><white><hits></white> von <white><total></white> getroffen</gray><extra>",
                "<gray>Hit <white><hits></white> of <white><total></white></gray><extra>");
        t("biathlon.result.penalty", " <gray>– <red><n> Strafrunde(n)</red> in der roten Schleife!</gray>",
                " <gray>– <red><n> penalty loop(s)</red> in the red loop!</gray>");
        t("biathlon.result.clean", " <em>– fehlerfrei!</em>", " <em>– clean!</em>");
        t("biathlon.penalty.done", "<gray>Strafrunden geschafft – weiter auf der Loipe!</gray>", "<gray>Penalty loops done – back on the trail!</gray>");
        t("biathlon.penalty.first", "<red>Erst die Strafrunden!</red>", "<red>Penalty loops first!</red>");
        t("biathlon.bar.run", "<au><time></au> <dark_gray>·</dark_gray> <gray>Runde <white><n></white>/<white><total></white></gray> <dark_gray>·</dark_gray> <gray>Treffer <white><hits></white></gray>",
                "<au><time></au> <dark_gray>·</dark_gray> <gray>Lap <white><n></white>/<white><total></white></gray> <dark_gray>·</dark_gray> <gray>Hits <white><hits></white></gray>");
        t("biathlon.bar.shoot", "<au><time></au> <dark_gray>·</dark_gray> <gray>Schuss <white><n></white>/<white><total></white> · noch <white><secs> s</white></gray>",
                "<au><time></au> <dark_gray>·</dark_gray> <gray>Shot <white><n></white>/<white><total></white> · <white><secs> s</white> left</gray>");
        t("biathlon.bar.penalty", "<au><time></au> <dark_gray>·</dark_gray> <red>Strafrunde – noch <n></red>",
                "<au><time></au> <dark_gray>·</dark_gray> <red>Penalty loop – <n> to go</red>");

        // ---------- Leiste rechts ----------
        t("sidebar.title", "<bl><b>❄ Wintersport</b></bl>", "<bl><b>❄ Winter Games</b></bl>");
        t("sidebar.village.players", "<gray>Im Dorf: <white><n></white></gray>", "<gray>In the village: <white><n></white></gray>");
        t("sidebar.village.next", "<gray>Nächstes: <bl><program></bl></gray>", "<gray>Next: <bl><program></bl></gray>");
        t("sidebar.village.count", "<gray>Start in <au><secs> s</au></gray>", "<gray>Starts in <au><secs> s</au></gray>");
        t("sidebar.village.running", "<gray>Läuft: <bl><discipline></bl></gray>", "<gray>Running: <bl><discipline></bl></gray>");
        t("sidebar.village.closed", "<gray>Sommerpause bis <white><date></white></gray>", "<gray>Summer break until <white><date></white></gray>");
        t("sidebar.medals", "<au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz> <gray>deine Medaillen</gray>",
                "<au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz> <gray>your medals</gray>");
        t("sidebar.address", "<au>morni.net</au>", "<au>morni.net</au>");
        t("sidebar.discipline", "<bl><discipline></bl> <gray>(<n>/<total>)</gray>", "<bl><discipline></bl> <gray>(<n>/<total>)</gray>");
        t("sidebar.heat", "<gray>Lauf <white><n></white> von <white><total></white></gray>", "<gray>Heat <white><n></white> of <white><total></white></gray>");
        t("sidebar.time", "<gray>Zeit: <au><time></au></gray>", "<gray>Time: <au><time></au></gray>");
        t("sidebar.table", "<white>Medaillenspiegel</white>", "<white>Medal table</white>");
        t("sidebar.row", "<white><place>.</white> <gray><name></gray> <au><ng></au><gray>/</gray><si><ns></si><gray>/</gray><bz><nb></bz>",
                "<white><place>.</white> <gray><name></gray> <au><ng></au><gray>/</gray><si><ns></si><gray>/</gray><bz><nb></bz>");
        t("sidebar.number", "<gray>Startnummer <au><n></au></gray>", "<gray>Bib <au><n></au></gray>");

        // ---------- Tafeln und Schilder im Dorf ----------
        t("board.medals", "<au><b>Ewiger Medaillenspiegel</b></au>", "<au><b>All-time medal table</b></au>");
        t("board.medals.row", "<white><place>.</white> <name> <au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz>",
                "<white><place>.</white> <name> <au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz>");
        t("board.empty", "<gray>Noch niemand – sei die oder der Erste!</gray>", "<gray>Nobody yet – be the first!</gray>");
        t("board.times", "<au><b>Bestzeiten</b></au>", "<au><b>Best times</b></au>");
        t("board.times.head", "<bl><discipline></bl>", "<bl><discipline></bl>");
        t("board.times.row", "<white><place>.</white> <name> <au><time></au>", "<white><place>.</white> <name> <au><time></au>");
        t("board.live", "<au><b><discipline></b></au>\n<gray><status></gray>", "<au><b><discipline></b></au>\n<gray><status></gray>");
        t("board.live.idle", "Gerade kein Rennen", "No race right now");
        t("board.live.heat", "Lauf <n> von <total>", "Heat <n> of <total>");
        t("board.live.row", "<white><place>.</white> <name> <au><time></au>", "<white><place>.</white> <name> <au><time></au>");
        t("board.live.running", "<gray><name> unterwegs …</gray>", "<gray><name> on course …</gray>");
        t("label.village", "<bl><b>❄ Bergdorf Morni-Kogel</b></bl>\n<gray>Wintersport auf morni.net</gray>", "<bl><b>❄ Morni-Kogel Mountain Village</b></bl>\n<gray>Winter Games on morni.net</gray>");
        t("label.podium", "<au><b>Siegertreppchen</b></au>", "<au><b>Winners' podium</b></au>");
        t("label.mulled", "<au>Glühwein & Kinderpunsch</au>\n<gray>heiß und süß</gray>", "<au>Mulled wine & hot punch</au>\n<gray>hot and sweet</gray>");
        t("label.rink", "<bl>Eisfläche</bl>\n<gray>Einfach drauflos rutschen!</gray>", "<bl>Ice rink</bl>\n<gray>Just slide around!</gray>");
        t("label.lift", "<bl>Sessellift</bl>\n<gray>Auf die rote Linie stellen –</gray>\n<gray>der nächste Sessel nimmt dich mit</gray>",
                "<bl>Chairlift</bl>\n<gray>Stand on the red line –</gray>\n<gray>the next chair picks you up</gray>");
        t("label.lift.top", "<bl>Bergstation</bl>\n<gray>Rodelbahn: links · Pisten und Bob: Starthäuser</gray>",
                "<bl>Top station</bl>\n<gray>Toboggan run: left · slopes and bob: start huts</gray>");
        t("label.rodel", "<au>Rodelbahn</au>\n<gray>Aufs Eis stellen – schon sitzt du im Schlitten!</gray>",
                "<au>Toboggan run</au>\n<gray>Step onto the ice – and you're on a sled!</gray>");
        t("label.sign", "<bl>⇦ Lift & Rodelbahn · Riesenslalom</bl>\n<bl>Bobbahn ⇨</bl>\n<bl>⇩ Biathlon-Stadion</bl>",
                "<bl>⇦ Lift & toboggan · giant slalom</bl>\n<bl>Bobsleigh ⇨</bl>\n<bl>⇩ Biathlon stadium</bl>");
        t("label.start", "<au>Start</au> <gray>· Bahn <white><n></white></gray>", "<au>Start</au> <gray>· lane <white><n></white></gray>");
        t("label.finish", "<au><b>Ziel</b></au>", "<au><b>Finish</b></au>");
        t("label.range", "<white><n></white>", "<white><n></white>");
        t("label.penalty", "<red><b>Strafrunde</b></red>", "<red><b>Penalty loop</b></red>");

        // ---------- Spaß im Dorf ----------
        t("rodel.go", "<gray>Ab geht's! <white>W</white> = Schwung, <white>Shift</white> = aussteigen.</gray>",
                "<gray>Off you go! <white>W</white> = push, <white>Shift</white> = get off.</gray>");
        t("rodel.time", "<gray>Rodelzeit: <au><time></au></gray>", "<gray>Toboggan time: <au><time></au></gray>");
        t("lift.board", "<gray>Gute Fahrt! Oben steigst du von selbst aus.</gray>", "<gray>Enjoy the ride! You get off at the top automatically.</gray>");
        t("lift.top", "<gray>Bergstation – willkommen auf dem Morni-Kogel!</gray>", "<gray>Top station – welcome to the Morni-Kogel!</gray>");

        // ---------- Statistik, Zuschauen ----------
        t("stats.header", "<au>━━ <name> ━━</au>", "<au>━━ <name> ━━</au>");
        t("stats.medals", "<au>●<ng></au> Gold · <si>●<ns></si> Silber · <bz>●<nb></bz> Bronze <gray>· <points> Punkte</gray>",
                "<au>●<ng></au> gold · <si>●<ns></si> silver · <bz>●<nb></bz> bronze <gray>· <points> points</gray>");
        t("stats.events", "<gray>Wettkämpfe: <white><events></white> · Rennen: <white><races></white> · Gesamtsiege: <white><wins></white></gray>",
                "<gray>Competitions: <white><events></white> · races: <white><races></white> · overall wins: <white><wins></white></gray>");
        t("stats.best", "<gray><discipline>: <au><time></au></gray>", "<gray><discipline>: <au><time></au></gray>");
        t("stats.hits", "<gray>Biathlon-Treffer: <white><hits></white>/<white><shots></white></gray>",
                "<gray>Biathlon hits: <white><hits></white>/<white><shots></white></gray>");
        t("stats.none", "<gray>Noch keine Werte.</gray>", "<gray>No stats yet.</gray>");
        t("top.header", "<au>━━ Bestzeiten <discipline> ━━</au>", "<au>━━ Best times <discipline> ━━</au>");
        t("spectate.start", "<gray>Du schaust zu: <bl><discipline></bl>. Mit dem roten Farbstoff geht's zurück ins Dorf.</gray>",
                "<gray>You're watching: <bl><discipline></bl>. Use the red dye to go back to the village.</gray>");
        t("spectate.none", "<gray>Gerade läuft kein Wettkampf.</gray>", "<gray>No competition is running right now.</gray>");
        t("leave.done", "<gray>Zurück im Dorf.</gray>", "<gray>Back in the village.</gray>");
        t("leave.nothing", "<gray>Du bist schon im Dorf.</gray>", "<gray>You're already in the village.</gray>");
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; für Gegenstände ohne kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „❄ Wintersport »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }

    /** Reiner Text (ohne Formatierung). */
    public static String plain(String lang, String key) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key)));
    }

    /** Text als Komponente aus rohem MiniMessage mit Platzhaltern (für zusammengesetzte Zeilen). */
    public static Component parse(String raw, TagResolver... resolvers) {
        return MM.deserialize(raw, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
