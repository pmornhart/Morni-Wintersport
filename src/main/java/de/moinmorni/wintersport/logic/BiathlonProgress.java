package de.moinmorni.wintersport.logic;

/**
 * Ablauf eines Biathlon-Rennens für eine Person: Runden laufen (Kontrollpunkte der Reihe nach, dann über die
 * Rundenlinie), nach den ersten Runden an den Schießstand (fünf Schuss), jede verfehlte Scheibe = eine Strafrunde,
 * nach der letzten Runde ins Ziel. Ohne Minecraft – das Spiel meldet nur, welche Linie gekreuzt wurde.
 */
public final class BiathlonProgress {

    public enum Phase { RUNNING, SHOOTING, PENALTY, FINISHED }

    /** Was nach dem Überqueren der Rundenlinie passiert. */
    public enum LapResult { NOTHING, LAP, SHOOT, FINISH }

    private final int laps;
    private final int shootings;
    private final int shots;
    private final int checkpoints;

    private Phase phase = Phase.RUNNING;
    private int lap;
    private int nextCheckpoint;
    private int shotsFired;
    private int roundHits;
    private int hits;
    private int misses;
    private int penaltyLeft;
    private boolean penaltyHalf;
    private int shootingsDone;

    /**
     * @param laps        Runden insgesamt
     * @param shootings   so oft wird geschossen (nach Runde 1, 2 …; höchstens Runden − 1)
     * @param shots       Schuss (= Scheiben) je Schießen
     * @param checkpoints Kontrollpunkte je Runde (vor der Rundenlinie)
     */
    public BiathlonProgress(int laps, int shootings, int shots, int checkpoints) {
        this.laps = Math.max(1, laps);
        this.shootings = Math.max(0, Math.min(shootings, this.laps - 1));
        this.shots = Math.max(1, shots);
        this.checkpoints = Math.max(0, checkpoints);
    }

    public Phase phase() {
        return phase;
    }

    /** Fertige Runden. */
    public int lap() {
        return lap;
    }

    public int laps() {
        return laps;
    }

    public int nextCheckpoint() {
        return nextCheckpoint;
    }

    public int hits() {
        return hits;
    }

    public int misses() {
        return misses;
    }

    public int shotsFired() {
        return shotsFired;
    }

    public int roundHits() {
        return roundHits;
    }

    public int shots() {
        return shots;
    }

    public int penaltyLeft() {
        return penaltyLeft;
    }

    public int shootingsDone() {
        return shootingsDone;
    }

    public int shootings() {
        return shootings;
    }

    /** Kontrollpunkt {@code index} gekreuzt – zählt nur in der richtigen Reihenfolge und ohne offene Strafrunden. */
    public boolean checkpoint(int index) {
        if (phase != Phase.RUNNING || index != nextCheckpoint || nextCheckpoint >= checkpoints) {
            return false;
        }
        nextCheckpoint++;
        return true;
    }

    /** Rundenlinie gekreuzt. Zählt nur, wenn alle Kontrollpunkte der Runde geschafft sind. */
    public LapResult lapLine() {
        if (phase != Phase.RUNNING || nextCheckpoint < checkpoints) {
            return LapResult.NOTHING;
        }
        lap++;
        nextCheckpoint = 0;
        if (lap >= laps) {
            phase = Phase.FINISHED;
            return LapResult.FINISH;
        }
        if (shootingsDone < shootings) {
            phase = Phase.SHOOTING;
            shotsFired = 0;
            roundHits = 0;
            return LapResult.SHOOT;
        }
        return LapResult.LAP;
    }

    /** Ein Schuss am Schießstand. */
    public void shot(boolean hit) {
        if (phase != Phase.SHOOTING || shotsFired >= shots) {
            return;
        }
        shotsFired++;
        if (hit) {
            roundHits++;
            hits++;
        }
    }

    public boolean allShot() {
        return phase == Phase.SHOOTING && shotsFired >= shots;
    }

    /**
     * Schießen vorbei (alle Schüsse oder Zeit um): nicht abgegebene Schüsse zählen als Fehler.
     *
     * @return Strafrunden
     */
    public int endShooting() {
        if (phase != Phase.SHOOTING) {
            return 0;
        }
        int missed = shots - roundHits;
        misses += missed;
        shootingsDone++;
        penaltyLeft = missed;
        penaltyHalf = false;
        phase = missed > 0 ? Phase.PENALTY : Phase.RUNNING;
        return missed;
    }

    /** Halbe Strafrunde (Wendepunkt der Strafrunde) gekreuzt. */
    public boolean penaltyTurn() {
        if (phase != Phase.PENALTY || penaltyHalf) {
            return false;
        }
        penaltyHalf = true;
        return true;
    }

    /**
     * Linie der Strafrunde gekreuzt: zählt eine Strafrunde, wenn vorher der Wendepunkt kam.
     *
     * @return true, wenn eine Strafrunde fertig ist
     */
    public boolean penaltyLine() {
        if (phase != Phase.PENALTY || !penaltyHalf) {
            return false;
        }
        penaltyHalf = false;
        penaltyLeft--;
        if (penaltyLeft <= 0) {
            penaltyLeft = 0;
            phase = Phase.RUNNING;
        }
        return true;
    }
}
