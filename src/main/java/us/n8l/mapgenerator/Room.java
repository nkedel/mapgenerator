package us.n8l.mapgenerator;

import java.util.concurrent.atomic.AtomicInteger;

public class Room {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    private final int id;
    private final RoomShape shape;       // enum instead of string
    private final String dimensions;     // e.g. "20' x 30'"

    public Room(RoomShape shape, String dimensions) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.shape = shape;
        this.dimensions = dimensions;
    }

    public int getId() {
        return id;
    }

    public RoomShape getShape() {
        return shape;
    }

    public String getDimensions() {
        return dimensions;
    }

    @Override
    public String toString() {
        return "Room #" + id + " [" + shape.getDescription()
               + " | " + dimensions + "]";
    }
}
