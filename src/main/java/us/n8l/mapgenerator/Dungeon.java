package us.n8l.mapgenerator;

import java.util.ArrayList;
import java.util.List;

public class Dungeon {
    private final List<Room> rooms;
    private final List<Corridor> corridors;

    public Dungeon() {
        this.rooms = new ArrayList<>();
        this.corridors = new ArrayList<>();
    }

    public void addRoom(Room room) {
        rooms.add(room);
    }

    public void addCorridor(Corridor corridor) {
        corridors.add(corridor);
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<Corridor> getCorridors() {
        return corridors;
    }
}
