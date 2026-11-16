package de.moinmorni.wintersport.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RulesTest {

    @Test
    void heatsAreBalancedAndKeepTheOrder() {
        assertEquals(List.of(List.of(1, 2, 3)), Heats.split(List.of(1, 2, 3), 4));
        assertEquals(List.of(List.of(1, 2, 3, 4), List.of(5, 6, 7)), Heats.split(List.of(1, 2, 3, 4, 5, 6, 7), 4));
        assertEquals(List.of(List.of(1, 2, 3), List.of(4, 5)), Heats.split(List.of(1, 2, 3, 4, 5), 4));
        assertEquals(3, Heats.split(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), 4).size());
        assertEquals(List.of(), Heats.split(List.<Integer>of(), 4));
    }

    @Test
    void partiesShareABobStrangersDont() {
        var bobs = Teams.bobs(List.of(List.of("anna", "carl", "eva"), List.of("bert"), List.of("dora")), true);
        assertEquals(List.of(List.of("anna", "carl"), List.of("eva"), List.of("bert"), List.of("dora")), bobs);
        var solo = Teams.bobs(List.of(List.of("anna", "carl"), List.of("bert")), false);
        assertEquals(3, solo.size());
    }

    @Test
    void partiesStayTogetherWhenPicking() {
        assertEquals(List.of(1, 2, 3, 6), Groups.pick(List.of(List.of(1, 2, 3), List.of(4, 5), List.of(6)), 4));
        assertEquals(List.of(1, 2), Groups.pick(List.of(List.of(1, 2, 3)), 2), "Party größer als erlaubt: die Ersten");
    }

    @Test
    void votesPickTheFavouriteOtherwiseTheLongestUnplayed() {
        Votes<Program> v = new Votes<>(Program.class);
        Set<Program> all = Set.of(Program.values());
        assertEquals(Program.WETTKAMPF, v.winner(all), "ohne Stimmen: der ganze Wettkampf");
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        v.vote(a, Program.BOB);
        v.vote(b, Program.BOB);
        assertEquals(Program.BOB, v.winner(all));
        v.retain(List.of(a));
        assertEquals(1, v.count(Program.BOB));
        v.forget(a);
        v.played(Program.WETTKAMPF, 10);
        assertEquals(Program.SLALOM, v.winner(all));
    }

    @Test
    void programs() {
        assertEquals(3, Program.WETTKAMPF.disciplines().size());
        assertTrue(Program.WETTKAMPF.full());
        assertFalse(Program.BOB.full());
        assertEquals(Program.BIATHLON, Program.byId("biathlon"));
        assertEquals(Discipline.SLALOM, Discipline.byId("SLALOM"));
        assertNull(Program.byId("curling"));
    }

    @Test
    void seasonSwitch() {
        assertTrue(Season.ALWAYS.open(LocalDate.of(2026, 7, 1)));
        assertFalse(Season.WINTER.open(LocalDate.of(2026, 11, 16)));
        assertTrue(Season.WINTER.open(LocalDate.of(2026, 12, 1)));
        assertTrue(Season.WINTER.open(LocalDate.of(2027, 1, 15)));
        assertTrue(Season.WINTER.open(LocalDate.of(2027, 3, 31)));
        assertFalse(Season.WINTER.open(LocalDate.of(2027, 4, 1)));
        assertEquals(LocalDate.of(2026, 12, 1), Season.WINTER.nextOpening(LocalDate.of(2026, 11, 16)));
        assertEquals(LocalDate.of(2027, 12, 1), Season.WINTER.nextOpening(LocalDate.of(2027, 4, 2)));
        assertEquals(LocalDate.of(2027, 3, 31), Season.WINTER.closing(LocalDate.of(2026, 12, 24)));
        assertEquals(LocalDate.of(2027, 3, 31), Season.WINTER.closing(LocalDate.of(2027, 2, 1)));
        assertNull(Season.ALWAYS.closing(LocalDate.of(2027, 2, 1)));
        assertEquals(Season.WINTER, Season.parse(" Winter "));
        assertEquals(Season.ALWAYS, Season.parse("immer"));
        assertEquals(Season.ALWAYS, Season.parse(null));
    }

    @Test
    void times() {
        assertEquals("0:14,32", TimeFormat.format(14_321, "de"));
        assertEquals("1:02.05", TimeFormat.format(62_050, "en"));
        assertEquals("+1,23", TimeFormat.gap(1_234, "de"));
        assertEquals("+3 s", TimeFormat.penalty(3_000));
        RaceClock c = new RaceClock(100);
        assertEquals(50, c.at(101, 1.0));
        assertEquals(1025, c.at(121, 0.5), "Hundertstel aus dem Bruchteil des Ticks");
        assertEquals(500, c.now(110));
    }

    @Test
    void crossingALineAndAGate() {
        Line l = Line.acrossZ(10.5, 0, 10);
        Line.Hit h = l.cross(3, 8, 3, 12);
        assertNotNull(h);
        assertEquals(0.625, h.t(), 1e-9);
        assertEquals(0.3, h.u(), 1e-9);
        assertNull(l.cross(3, 8, 3, 10), "noch nicht drüber");
        assertNull(l.cross(3, 10.5, 3, 12), "vom Strich weg zählt nicht noch einmal");
        assertNull(l.cross(12, 8, 12, 12), "neben der Linie vorbei");
        Gate g = new Gate(l, 0.2, 0.5, true);
        assertEquals(Gate.Pass.PASSED, g.check(3, 8, 3, 12));
        assertEquals(Gate.Pass.MISSED, g.check(8, 8, 8, 12));
        assertEquals(Gate.Pass.NONE, g.check(3, 1, 3, 2));
        assertEquals(Gate.Pass.PASSED, g.check(1, 9, 4, 13), "auch schräg und schnell");
    }
}
