package net.morni.wintersport.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BiathlonProgressTest {

    private static void lap(BiathlonProgress p) {
        for (int i = 0; i < 3; i++) {
            assertTrue(p.checkpoint(i), "Kontrollpunkt " + i);
        }
    }

    @Test
    void sprintWithTwoShootingsAndPenaltyLaps() {
        BiathlonProgress p = new BiathlonProgress(3, 2, 5, 3);
        assertEquals(BiathlonProgress.LapResult.NOTHING, p.lapLine(), "Start: Linie zählt erst nach der Runde");
        assertFalse(p.checkpoint(1), "Reihenfolge");
        lap(p);
        assertEquals(BiathlonProgress.LapResult.SHOOT, p.lapLine());
        assertEquals(BiathlonProgress.Phase.SHOOTING, p.phase());
        p.shot(true);
        p.shot(false);
        p.shot(true);
        p.shot(true);
        assertFalse(p.allShot());
        p.shot(false);
        assertTrue(p.allShot());
        assertEquals(2, p.endShooting(), "zwei Fehler = zwei Strafrunden");
        assertEquals(BiathlonProgress.Phase.PENALTY, p.phase());
        assertFalse(p.checkpoint(0), "erst die Strafrunden");
        assertFalse(p.penaltyLine(), "ohne Wendepunkt zählt die Linie nicht");
        assertTrue(p.penaltyTurn());
        assertTrue(p.penaltyLine());
        assertEquals(1, p.penaltyLeft());
        p.penaltyTurn();
        p.penaltyLine();
        assertEquals(BiathlonProgress.Phase.RUNNING, p.phase());
        lap(p);
        assertEquals(BiathlonProgress.LapResult.SHOOT, p.lapLine());
        for (int i = 0; i < 5; i++) {
            p.shot(true);
        }
        p.shot(true);
        assertEquals(5, p.shotsFired(), "nicht mehr als fünf Schuss");
        assertEquals(0, p.endShooting());
        assertEquals(BiathlonProgress.Phase.RUNNING, p.phase());
        lap(p);
        assertEquals(BiathlonProgress.LapResult.FINISH, p.lapLine());
        assertEquals(BiathlonProgress.Phase.FINISHED, p.phase());
        assertEquals(8, p.hits());
        assertEquals(2, p.misses());
        assertEquals(3, p.lap());
    }

    @Test
    void timeUpCountsMissingShotsAsMisses() {
        BiathlonProgress p = new BiathlonProgress(2, 1, 5, 3);
        lap(p);
        p.lapLine();
        p.shot(true);
        assertEquals(4, p.endShooting());
        assertEquals(4, p.penaltyLeft());
    }

    @Test
    void neverMoreShootingsThanLapsAllow() {
        BiathlonProgress p = new BiathlonProgress(1, 3, 5, 3);
        lap(p);
        assertEquals(BiathlonProgress.LapResult.FINISH, p.lapLine());
        assertEquals(0, p.shootings());
    }
}
