package us.n8l.mapgenerator;

public class Corridor {
    private final Room fromRoom;
    private final Room toRoom;
    private final int lengthFeet; // how many feet is this corridor?
    private final String description; // e.g. “turn left 90°,” “stairs up 1 level,” etc.

    public Corridor(Room fromRoom, Room toRoom, int lengthFeet, String description) {
        this.fromRoom = fromRoom;
        this.toRoom = toRoom;
        this.lengthFeet = lengthFeet;
        this.description = description;
    }

    public Room getFromRoom() {
        return fromRoom;
    }

    public Room getToRoom() {
        return toRoom;
    }

    public int getLengthFeet() {
        return lengthFeet;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        String fromId = (fromRoom == null) ? "None" : ("Room#" + fromRoom.getId());
        String toId   = (toRoom   == null) ? "None" : ("Room#" + toRoom.getId());
        return "Corridor [" + fromId + " -> " + toId
               + ", length=" + lengthFeet + " ft, " + description + "]";
    }
}
