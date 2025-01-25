package us.n8l.mapgenerator;

import java.awt.*;

public class GridCell {
    public enum CellType {
        EMPTY,
        ROOM,
        CORRIDOR
    }

    private final Point coordinate; // (x, y)
    private CellType cellType;
    private int roomId; // 0 if not in a room

    public GridCell(int x, int y) {
        this.coordinate = new Point(x, y);
        this.cellType = CellType.EMPTY;
        this.roomId = 0;
    }

    public Point getCoordinate() {
        return coordinate;
    }

    public CellType getCellType() {
        return cellType;
    }

    public void setCellType(CellType cellType) {
        this.cellType = cellType;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    @Override
    public String toString() {
        return "GridCell[" + coordinate.x + "," + coordinate.y
               + " " + cellType + " R" + roomId + "]";
    }
}
