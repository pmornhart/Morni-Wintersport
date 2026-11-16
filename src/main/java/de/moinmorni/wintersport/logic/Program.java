package de.moinmorni.wintersport.logic;

import java.util.List;

/** Worüber im Dorf abgestimmt wird: der ganze Wettkampf (drei Disziplinen) oder eine Disziplin allein. */
public enum Program {
    WETTKAMPF("wettkampf", List.of(Discipline.SLALOM, Discipline.BIATHLON, Discipline.BOB)),
    SLALOM("slalom", List.of(Discipline.SLALOM)),
    BIATHLON("biathlon", List.of(Discipline.BIATHLON)),
    BOB("bob", List.of(Discipline.BOB));

    private final String id;
    private final List<Discipline> disciplines;

    Program(String id, List<Discipline> disciplines) {
        this.id = id;
        this.disciplines = disciplines;
    }

    public String id() {
        return id;
    }

    public List<Discipline> disciplines() {
        return disciplines;
    }

    /** Mehr als eine Disziplin: dann gibt es eine Gesamtwertung nach Punkten. */
    public boolean full() {
        return disciplines.size() > 1;
    }

    public static Program byId(String id) {
        if (id == null) {
            return null;
        }
        for (Program p : values()) {
            if (p.id.equalsIgnoreCase(id)) {
                return p;
            }
        }
        Discipline d = Discipline.byId(id);
        return d == null ? null : byId(d.id());
    }
}
