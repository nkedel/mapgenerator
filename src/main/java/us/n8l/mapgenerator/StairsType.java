package us.n8l.mapgenerator;

public enum StairsType {
    DOWN_1("Down 1 level"),
    DOWN_2("Down 2 levels"),
    DOWN_3("Down 3 levels"),
    UP_1("Up 1 level"),
    UP_TO_DEAD_END("Up to dead end (possible chute trap)"),
    DOWN_TO_DEAD_END("Down to dead end (possible chute trap)"),
    CHIMNEY_UP_1("Chimney up 1 level, passage continues"),
    CHIMNEY_UP_2("Chimney up 2 levels, passage continues"),
    CHIMNEY_DOWN_2("Chimney down 2 levels, passage continues"),
    TRAP_DOOR_DOWN_1("Trap door down 1 level, passage continues"),
    TRAP_DOOR_DOWN_2("Trap door down 2 levels, passage continues"),
    UP_1_DOWN_2_CHAMBER("Up 1 level, then down 2 levels, ends in chamber");

    private final String description;

    StairsType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
