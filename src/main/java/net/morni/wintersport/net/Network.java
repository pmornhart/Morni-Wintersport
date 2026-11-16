package net.morni.wintersport.net;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Verbindung zum Netzwerk morni.net. Läuft das Netzwerk-Plugin („MorniNet“), geht alles über dessen Schnittstelle
 * ({@code net.morni.api.MorniNet}); sonst läuft Wintersport allein: deutsche Texte, keine Netzwerk-Statistik, „zur
 * Lobby“ trennt die Verbindung mit einem Hinweis.
 *
 * <p>Nur {@link Connected} kennt die Netzwerk-Klassen – ohne Netzwerk-Plugin wird sie nie geladen.
 */
public interface Network {

    /** Name dieses Modus im Netzwerk (Statistiken, Zustand). */
    String MODE = "wintersport";

    /** Läuft das Netzwerk-Plugin? */
    boolean connected();

    /** „de“ oder „en“. */
    String language(UUID player);

    /**
     * Statistiken melden: {@code add} hochzählen, {@code bestTimes} Bestzeiten (kleiner ist besser), {@code bestHigh}
     * Bestwerte (größer ist besser). Das Netzwerk kennt pro Aufruf nur eine Richtung – darum bis zu zwei Aufrufe.
     */
    void stats(UUID player, Map<String, Long> add, Map<String, Long> bestTimes, Map<String, Long> bestHigh);

    /** Zustand für Lobby-Figur und Webseite: „waiting“, „running“ oder „closed“ (außerhalb der Saison). */
    void state(String state, int players);

    /** Zurück in die Lobby (ohne Netzwerk: Verbindung trennen mit {@code message}). */
    void toLobby(Player player, Component message);

    /** Party des Spielers (leer ohne Party oder ohne Netzwerk). Kommt auf irgendeinem Thread zurück. */
    CompletableFuture<Optional<Party>> party(UUID player);

    /** Party: Anführer und Mitglieder (Server = wo sie gerade sind, null = beim Wechseln). */
    record Party(UUID leader, List<Member> members) {
        public record Member(UUID uuid, String name, String server) {
        }
    }

    /** Name dieses Servers im Netzwerk (z. B. „wintersport“), ohne Netzwerk leer. */
    String serverName();

    /** Netzwerk suchen: mit Plugin „MorniNet“ die echte Verbindung, sonst allein. */
    static Network connect(Plugin plugin) {
        Logger log = plugin.getLogger();
        if (plugin.getServer().getPluginManager().getPlugin("MorniNet") != null) {
            try {
                Network n = Connected.create();
                if (n != null) {
                    return n;
                }
                log.warning("Netzwerk-Plugin gefunden, aber ohne Schnittstelle für Spielmodi – Wintersport läuft allein.");
            } catch (LinkageError | RuntimeException e) {
                log.warning("Netzwerk-Schnittstelle passt nicht (" + e + ") – Wintersport läuft allein.");
            }
        }
        return new Standalone();
    }
}
