package de.moinmorni.wintersport.course;

/**
 * Sessellift vom Dorf auf das Gipfelplateau: zwei Seile (hinauf westlich, hinunter östlich), Stützen alle
 * {@link #PYLON_EVERY} Blöcke. Die Sessel sind Anzeige-Objekte und laufen im Kreis; wer sich auf den Einstieg der
 * Talstation stellt, wird vom nächsten Sessel mitgenommen.
 */
public final class Lift {

    public static final double UP_X = Places.LIFT_X - 2 + 0.5;
    public static final double DOWN_X = Places.LIFT_X + 2 + 0.5;
    public static final double VALLEY_Z = Places.LIFT_VALLEY_Z + 0.5;
    public static final double TOP_Z = Places.LIFT_TOP_Z + 0.5;
    public static final int PYLON_EVERY = 34;
    /** Sessel hängen so weit unter dem Seil. */
    public static final double HANG = 2.6;

    private Lift() {
    }

    /** Höhe des Seils (über dem Tal) an der Stelle z – gerade Linie zwischen den Stationen. */
    public static double cableY(double z) {
        double y0 = Terrain.height(Places.LIFT_VALLEY_Z) + Places.LIFT_CABLE;
        double y1 = Terrain.height(Places.LIFT_TOP_Z) + Places.LIFT_CABLE;
        double t = (z - VALLEY_Z) / (TOP_Z - VALLEY_Z);
        return y0 + (y1 - y0) * Math.max(0, Math.min(1, t));
    }

    /** Länge eines Seils (schräg). */
    public static double length() {
        double dz = VALLEY_Z - TOP_Z;
        double dy = cableY(TOP_Z) - cableY(VALLEY_Z);
        return Math.hypot(dz, dy);
    }

    /**
     * Lage auf dem Umlauf (0 … 2·Länge): erst hinauf am westlichen Seil, dann hinunter am östlichen.
     *
     * @return {x, y (Seil), z, Richtung: +1 hinauf / −1 hinunter}
     */
    public static double[] at(double s) {
        double len = length();
        double u = Math.floorMod((long) Math.floor(s * 1000), (long) Math.floor(2 * len * 1000)) / 1000.0;
        boolean up = u < len;
        double t = up ? u / len : (u - len) / len;
        double z = up ? VALLEY_Z + (TOP_Z - VALLEY_Z) * t : TOP_Z + (VALLEY_Z - TOP_Z) * t;
        return new double[] {up ? UP_X : DOWN_X, cableY(z), z, up ? 1 : -1};
    }

    /** z der Stützen. */
    public static int[] pylons() {
        int n = (Places.LIFT_VALLEY_Z - Places.LIFT_TOP_Z) / PYLON_EVERY;
        int[] out = new int[n - 1];
        for (int i = 1; i < n; i++) {
            out[i - 1] = Places.LIFT_VALLEY_Z - i * PYLON_EVERY;
        }
        return out;
    }
}
