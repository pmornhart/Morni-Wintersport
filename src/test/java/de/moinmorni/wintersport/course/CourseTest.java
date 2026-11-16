package de.moinmorni.wintersport.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.wintersport.logic.Gate;
import de.moinmorni.wintersport.logic.Line;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseTest {

    @Test
    void terrainRisesInTerracesOfTwoSingleSteps() {
        assertEquals(0, Terrain.height(-40));
        assertEquals(0, Terrain.height(50));
        assertEquals(1, Terrain.height(-41));
        assertEquals(2, Terrain.height(-42));
        assertEquals(2, Terrain.height(-60));
        assertEquals(3, Terrain.height(-61));
        assertEquals(Terrain.TOP, Terrain.height(-280));
        assertEquals(Terrain.TOP, Terrain.height(-320));
        for (int z = -330; z < 0; z++) {
            int diff = Terrain.height(z) - Terrain.height(z + 1);
            assertTrue(diff == 0 || diff == 1, "nach Süden nie mehr als ein Block auf einmal: z=" + z);
        }
    }

    /** Ein Boot darf bergab nie mehr als zwei Blöcke am Stück fallen (sonst geht es kaputt). */
    @Test
    void neverMoreThanTwoBlocksDownInARow() {
        int run = 0;
        for (int z = -330; z < 0; z++) {
            int diff = Terrain.height(z) - Terrain.height(z + 1);
            run = diff > 0 ? run + diff : 0;
            assertTrue(run <= 2, "z=" + z);
        }
    }

    @Test
    void slalomGatesLieOnFlatGroundAndAlternate() {
        for (int lane = 0; lane < Slalom.lanes(); lane++) {
            List<Gate> gates = Slalom.gates(lane);
            assertEquals(12, gates.size());
            for (int g = 0; g < gates.size(); g++) {
                Gate gate = gates.get(g);
                int row = Slalom.gateRow(g);
                for (int dz = -4; dz <= 4; dz++) {
                    assertEquals(Terrain.height(row), Terrain.height(row + dz), "Tor " + g + " steht auf flachem Eis");
                }
                double[] p = gate.poles();
                assertTrue(Math.abs(p[0] - Slalom.centerX(lane)) <= Slalom.HALF + 0.5, "Stange auf der Piste");
                assertEquals(g % 2 == 0, gate.red());
                if (g > 0) {
                    assertTrue(Slalom.gateRow(g) > Slalom.gateRow(g - 1), "Tore von oben nach unten");
                }
            }
            double[] mid = gates.get(0).middle();
            assertEquals(Gate.Pass.PASSED, gates.get(0).check(mid[0], mid[1] - 1, mid[0], mid[1] + 1));
            double other = 2 * Slalom.centerX(lane) - mid[0];
            assertEquals(Gate.Pass.MISSED, gates.get(0).check(other, mid[1] - 1, other, mid[1] + 1));
        }
        assertTrue(Slalom.GATE_ROW_Z > Slalom.START_Z && Slalom.FINISH_Z > Slalom.gateRow(11));
    }

    @Test
    void lanesDoNotOverlap() {
        for (int i = 1; i < Slalom.lanes(); i++) {
            assertTrue(Slalom.LANE_X[i] - Slalom.LANE_X[i - 1] > 2 * Slalom.HALF + 4);
        }
        for (int i = 1; i < Bob.lanes(); i++) {
            assertTrue(Bob.LANE_X[i] - Bob.LANE_X[i - 1] >= Bob.SWING + 2 * Bob.WALL + 4);
        }
        assertTrue(Slalom.LANE_X[Slalom.lanes() - 1] + Slalom.HALF + 4 < Places.RODEL_X - Places.RODEL_HALF);
        assertTrue(Places.RODEL_X + Places.RODEL_HALF + 4 < Places.LIFT_X - 3);
    }

    /** Der Bob fährt nie bergauf: Absätze nur auf den Geraden nach Süden, Kurven auf flachem Eis. */
    @Test
    void bobOnlyGoesDownhillAndCurvesAreFlat() {
        for (int lane = 0; lane < Bob.lanes(); lane++) {
            Path p = Bob.path(lane);
            int lastH = Integer.MAX_VALUE;
            Path.Sample prev = null;
            for (Path.Sample s : p.samples()) {
                int h = Terrain.height(s.z());
                assertTrue(h <= lastH, "bergauf bei " + s);
                if (prev != null && h < lastH) {
                    assertEquals(0f, s.yaw(), 0.01f, "Absatz nur auf der Geraden nach Süden: " + s);
                }
                lastH = h;
                prev = s;
            }
            assertTrue(p.length() > 380, "lang genug: " + p.length());
            assertEquals(Bob.x(lane, 0), p.samples().get(p.samples().size() - 1).x(), 1e-6);
            assertTrue(Bob.checkpoints(lane).size() > 20);
        }
    }

    @Test
    void bobFinishCrossesTheChannel() {
        for (int lane = 0; lane < Bob.lanes(); lane++) {
            Line f = Bob.finish(lane);
            double x = Bob.finishX(lane);
            assertTrue(f.cross(x, Bob.FINISH_Z - 1, x, Bob.FINISH_Z + 1) != null);
            assertTrue(Bob.path(lane).distance(x, Bob.FINISH_Z) < 0.01);
        }
    }

    @Test
    void biathlonLoopHasCheckpointsInOrderAndARangePerRacer() {
        Path loop = Biathlon.loop();
        List<Line> cps = Biathlon.checkpoints();
        double[] at = new double[cps.size()];
        for (int i = 0; i < cps.size(); i++) {
            Line l = cps.get(i);
            int idx = loop.nearest(l.x(0.5), l.z(0.5), -1);
            at[i] = loop.samples().get(idx).s();
            assertTrue(loop.distance(l.x(0.5), l.z(0.5)) < 0.5, "Kontrollpunkt auf der Loipe");
            if (i > 0) {
                assertTrue(at[i] > at[i - 1], "Reihenfolge im Uhrzeigersinn");
            }
        }
        assertTrue(loop.length() > 180 && loop.length() < 260, "Runde: " + loop.length());
        assertTrue(Biathlon.laneX(Biathlon.LANES - 1) + 3 < Biathlon.EAST_X);
        for (int i = 0; i < Biathlon.LANES; i++) {
            double[] g = Biathlon.grid(i);
            assertTrue(g[0] < Biathlon.START_GATE_X && Biathlon.onTrack(g[0], g[1]), "Startplatz " + i);
        }
        assertTrue(Biathlon.onTrack(Biathlon.afterRange()[0], Biathlon.afterRange()[1]));
        assertTrue(Biathlon.onTrack(Biathlon.penaltyStart()[0], Biathlon.penaltyStart()[1]));
        double[] ps = Biathlon.penaltyStart();
        assertTrue(Biathlon.penaltyTurn().length() > 2 && Biathlon.penaltyLine().cross(ps[0], ps[1], ps[0] + 3, ps[1]) != null);
    }
}
