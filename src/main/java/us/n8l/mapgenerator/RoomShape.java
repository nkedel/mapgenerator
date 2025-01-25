package us.n8l.mapgenerator;

public enum RoomShape {
    STARTER("Starter"),
    CORRIDOR_END("Corridor End"),

    SQUARE("Square"),
    RECTANGULAR("Rectangular"),
    CIRCULAR("Circular"),
    UNUSUAL("Unusual");

    private final String description;

    RoomShape(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
