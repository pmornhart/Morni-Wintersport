package de.moinmorni.wintersport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.wintersport.data.Leaderboard;
import de.moinmorni.wintersport.data.PlayerData;
import de.moinmorni.wintersport.logic.Discipline;
import de.moinmorni.wintersport.logic.Medal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataTest {

    @Test
    void playerDataSurvivesTheFile() {
        UUID u = UUID.randomUUID();
        PlayerData d = new PlayerData(u, "Anna");
        assertTrue(d.addRace(Discipline.SLALOM, 21_000, Medal.GOLD, 10));
        assertFalse(d.addRace(Discipline.SLALOM, 25_000, Medal.SILVER, 8), "langsamer ist keine Bestzeit");
        assertTrue(d.addRace(Discipline.SLALOM, 19_500, null, 6));
        d.addRace(Discipline.BOB, -1, null, 0);
        d.addEvent(true);
        d.addShots(7, 10);
        PlayerData back = PlayerData.fromYaml(u, d.toYaml());
        assertEquals("Anna", back.name());
        assertEquals(19_500, back.best(Discipline.SLALOM));
        assertEquals(-1, back.best(Discipline.BOB));
        assertEquals(4, back.races());
        assertEquals(1, back.gold());
        assertEquals(1, back.silver());
        assertEquals(24, back.points());
        assertEquals(1, back.wins());
        assertEquals(7, back.hits());
        assertEquals(10, back.shots());
    }

    @Test
    void leaderboardKeepsOneBestTimePerPerson() {
        Leaderboard b = new Leaderboard();
        UUID a = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertEquals(1, b.submit(Discipline.BOB, a, "Anna", 30_000));
        assertEquals(1, b.submit(Discipline.BOB, c, "Carl", 25_000));
        assertEquals(0, b.submit(Discipline.BOB, a, "Anna", 31_000), "nicht besser");
        assertEquals(1, b.submit(Discipline.BOB, a, "Anna", 24_000));
        assertEquals(2, b.top(Discipline.BOB).size());
        for (int i = 0; i < 15; i++) {
            b.submit(Discipline.SLALOM, UUID.randomUUID(), "P" + i, 10_000 + i);
        }
        assertEquals(Leaderboard.KEEP, b.top(Discipline.SLALOM).size());
        b.medal(a, "Anna", Medal.GOLD, 10);
        b.medal(c, "Carl", null, 3);
        Leaderboard back = Leaderboard.fromYaml(b.toYaml());
        assertEquals(24_000, back.top(Discipline.BOB).get(0).ms());
        assertEquals(1, back.medalTable().size(), "ohne Medaille nicht im Spiegel");
        assertEquals("Anna", back.nameOf(back.medalTable().get(0).id()));
    }
}
