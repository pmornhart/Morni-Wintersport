package net.morni.wintersport.course;

/**
 * Wo im Bergdorf was steht (Blockkoordinaten relativ zur Talsohle, y = 0 ist der Boden): Dorfplatz mit Christbaum,
 * Siegertreppchen, Tafeln, Eisfläche, Glühweinstand, Sessellift, Rodelbahn und die Zuschauertribünen an den Zielen.
 */
public final class Places {

    /** Ankunft im Dorf (Blick nach Norden zum Christbaum und zum Berg). */
    public static final double[] SPAWN = {0.5, 1, 24.5};
    public static final float SPAWN_YAW = 180f;
    /** Dorfplatz (Mitte) und Christbaum. */
    public static final int PLAZA_Z = 8;
    public static final int PLAZA_R = 15;
    public static final int[] TREE = {0, 6};
    /** Siegertreppchen: Mitte, Blick der Sieger nach Süden auf den Platz. */
    public static final int[] PODIUM = {0, -9};
    /** Tafeln links und rechts vom Treppchen. */
    public static final double[] BOARD_MEDALS = {-11.5, 4.2, -7.5};
    public static final double[] BOARD_TIMES = {12.5, 4.2, -7.5};
    /** Eisfläche (Weiher). */
    public static final int RINK_X1 = -44;
    public static final int RINK_X2 = -22;
    public static final int RINK_Z1 = 12;
    public static final int RINK_Z2 = 30;
    /** Glühweinstand. */
    public static final int[] MULLED = {16, 24};
    /** Sessellift: Spalte der Mitte, Tal- und Bergstation. */
    public static final int LIFT_X = -70;
    public static final int LIFT_VALLEY_Z = -33;
    public static final int LIFT_TOP_Z = -290;
    /** Seilhöhe über dem Boden an den Stationen. */
    public static final int LIFT_CABLE = 8;
    /** Rodelbahn: Mitte, halbe Breite, Start oben, Ende unten. */
    public static final int RODEL_X = -86;
    public static final int RODEL_HALF = 4;
    public static final int RODEL_START_Z = -287;
    public static final int RODEL_FINISH_Z = -36;
    public static final int RODEL_RUNOUT_Z = -27;
    /** Tribünen (Mitte, z der vordersten Reihe) – Blick nach Norden. */
    public static final int[] STAND_SLALOM = {-131, -15};
    public static final int[] STAND_BOB = {120, -15};
    public static final int[] STAND_BIATHLON = {104, 66};
    /** Grenzen der ganzen Welt (darüber hinaus unsichtbare Wand). */
    public static final int MIN_X = -184;
    public static final int MAX_X = 192;
    public static final int MIN_Z = -320;
    public static final int MAX_Z = 124;

    private Places() {
    }

    /** Welt-y für Planhöhe y. */
    public static double worldY(double y) {
        return Terrain.FLOOR_Y + y;
    }
}
