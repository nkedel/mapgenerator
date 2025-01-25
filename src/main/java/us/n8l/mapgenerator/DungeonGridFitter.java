package us.n8l.mapgenerator;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Maps the abstract Dungeon graph to a 2D grid of GridCells with:
 *  - Room placement ensuring no overlap
 *  - BFS corridor routing
 *  - Logging for progress
 *  - Some performance improvements (O(1) lookups, BFS short-circuit)
 */
public class DungeonGridFitter implements DungeonFitter {

    private static final Logger LOG = Logger.getLogger(DungeonGridFitter.class.getName());

    private final Map<Point, GridCell> gridMap;
    private final Map<Integer, List<Point>> roomBoundaryCache;  // Caches boundary squares for each room ID

    private Rectangle bounds;

    public DungeonGridFitter() {
        this.gridMap = new HashMap<>();
        this.roomBoundaryCache = new HashMap<>();

        // Optionally configure logger:
        // LOG.setLevel(Level.INFO); // or Level.FINE, etc.
    }

    /**
     * Fit the dungeon: place rooms, connect corridors, compute bounding rectangle.
     */
    public Rectangle fitDungeon(Dungeon dungeon) {
        long startTime = System.currentTimeMillis();
        LOG.info("Starting dungeon fit... Number of rooms: " + dungeon.getRooms().size()
                 + ", corridors: " + dungeon.getCorridors().size());

        // 1) Place rooms
        placeAllRooms(dungeon.getRooms());

        // 2) Connect corridors
        List<Corridor> corridors = dungeon.getCorridors();
        LOG.info("Connecting " + corridors.size() + " corridors via BFS...");
        int connectedCount = 0;

        for (Corridor c : corridors) {
            connectCorridor(c);
            connectedCount++;
            if (connectedCount % 5 == 0) {
                LOG.info("  ...connected " + connectedCount + " corridors so far");
            }
        }

        // 3) Compute bounding rectangle
        Rectangle rect = computeUsedBounds();
        long endTime = System.currentTimeMillis();
        LOG.info("Dungeon fit complete. Used area: ("
                 + rect.x + "," + rect.y + ") " + rect.width + "x" + rect.height
                 + ". Elapsed ms: " + (endTime - startTime));
        return rect;
    }

    private Rectangle computeUsedBounds() {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (GridCell cell : gridMap.values()) {
            if (cell.getCellType() != GridCell.CellType.EMPTY) {
                int x = cell.getCoordinate().x;
                int y = cell.getCoordinate().y;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
        if (minX > maxX || minY > maxY) {
            // No used cells
            return new Rectangle(0, 0, 0, 0);
        }
        return new Rectangle(minX, minY, (maxX - minX + 1), (maxY - minY + 1));
    }

    /**
     * Place all rooms in a simple row-based layout.
     * Enhanced with logging to see progress.
     */
    private void placeAllRooms(List<Room> rooms) {
        final int MAX_ROW_WIDTH = 80;
        int currentX = 0;
        int currentY = 0;
        int tallestRoomInRow = 0;

        LOG.info("Placing " + rooms.size() + " rooms...");
        int placedCount = 0;

        for (Room room : rooms) {
            Dimension dims = parseRoomDimensions(room.getDimensions());

            if (currentX + dims.width > MAX_ROW_WIDTH) {
                currentX = 0;
                currentY += (tallestRoomInRow + 3);
                tallestRoomInRow = 0;
            }

            // In real code, do a more robust collision check or attempt multiple positions.
            placeRoom(room.getId(), dims.width, dims.height, currentX, currentY);

            currentX += dims.width + 2;
            if (dims.height > tallestRoomInRow) {
                tallestRoomInRow = dims.height;
            }
            placedCount++;
            LOG.fine("Placed room#" + room.getId() + " at (" + currentX + "," + currentY + "), size "
                    + dims.width + "x" + dims.height);

            if (placedCount % 5 == 0) {
                LOG.info("  ...placed " + placedCount + " rooms so far");
            }
        }
        LOG.info("All rooms placed.");
    }

    private void placeRoom(int roomId, int width, int height, int startX, int startY) {
        for (int y = startY; y < startY + height; y++) {
            for (int x = startX; x < startX + width; x++) {
                GridCell cell = getOrCreateCell(x, y);
                cell.setCellType(GridCell.CellType.ROOM);
                cell.setRoomId(roomId);
            }
        }
        // We can eagerly compute boundary squares here if we want:
        // (Or we can do it lazily in findRoomBoundary)
        // We'll do lazy for demonstration.
    }

    /**
     * Connect two rooms with a BFS corridor.
     */
    private void connectCorridor(Corridor corridor) {
        Room from = corridor.getFromRoom();
        Room to   = corridor.getToRoom();
        if (from == null || to == null) {
            // Probably a dead-end corridor or something. Skip.
            return;
        }

        int fromId = from.getId();
        int toId   = to.getId();

        List<Point> fromBoundary = findRoomBoundaryCached(fromId);
        List<Point> toBoundary   = findRoomBoundaryCached(toId);

        if (fromBoundary.isEmpty() || toBoundary.isEmpty()) {
            LOG.fine("No boundary squares found for corridor: "
                     + fromId + "->" + toId);
            return;
        }

        // For demonstration, pick the first boundary in each list
        // (or you might pick best pair via Manhattan distance)
        Point start = fromBoundary.get(0);
        Point goal  = toBoundary.get(0);

        // BFS
        List<Point> path = bfsPath(start, goal);

        // Mark corridor squares (except if it's a room)
        if (!path.isEmpty()) {
            for (Point p : path) {
                GridCell cell = getCell(p.x, p.y);
                if (cell == null) {
                    cell = getOrCreateCell(p.x, p.y);
                }
                if (cell.getCellType() == GridCell.CellType.EMPTY
                        || cell.getCellType() == GridCell.CellType.CORRIDOR) {
                    cell.setCellType(GridCell.CellType.CORRIDOR);
                }
                // If it's ROOM, we assume it's a boundary square
                // that you can pass through as the "door."
            }
            LOG.fine("Corridor connected rooms " + fromId + " -> " + toId
                     + " with path length: " + path.size());
        } else {
            LOG.fine("No path found for corridor: " + fromId + "->" + toId);
        }
    }

    /**
     * BFS path from start->goal, ignoring room squares except for
     * start and goal (which might be room boundary).
     */
    private List<Point> bfsPath(Point start, Point goal) {
        // If start==goal, trivial path
        if (start.equals(goal)) {
            return Collections.singletonList(start);
        }

        Queue<Point> queue = new ArrayDeque<>();
        Map<Point, Point> cameFrom = new HashMap<>();
        Set<Point> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);
        cameFrom.put(start, null);

        while (!queue.isEmpty()) {
            Point current = queue.remove();
            if (current.equals(goal)) {
                // Found the goal => reconstruct path
                return reconstructPath(cameFrom, goal);
            }

            // Expand neighbors
            for (Point neighbor : getNeighbors(current, goal)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        // no path
        return Collections.emptyList();
    }

    /**
     * Return neighbors that are passable: either corridor/empty
     * or the goal cell if it’s a boundary room cell.
     */
    private List<Point> getNeighbors(Point current, Point goal) {
        List<Point> result = new ArrayList<>(4);
        int[][] deltas = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : deltas) {
            int nx = current.x + d[0];
            int ny = current.y + d[1];

            // If neighbor is out of known map, treat as empty
            // (since we can expand the map arbitrarily).
            GridCell neighborCell = getCell(nx, ny);

            if (neighborCell == null) {
                // "Empty" => passable
                result.add(new Point(nx, ny));
            } else {
                // If it's corridor or empty, passable
                if (neighborCell.getCellType() == GridCell.CellType.CORRIDOR
                 || neighborCell.getCellType() == GridCell.CellType.EMPTY) {
                    result.add(neighborCell.getCoordinate());
                }
                // If it's ROOM, only pass if it's the goal cell
                // (meaning the boundary of the target room).
                else if (neighborCell.getCellType() == GridCell.CellType.ROOM) {
                    if (neighborCell.getCoordinate().equals(goal)) {
                        result.add(goal);
                    }
                }
            }
        }
        return result;
    }

    private List<Point> reconstructPath(Map<Point, Point> cameFrom, Point goal) {
        List<Point> path = new ArrayList<>();
        Point current = goal;
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ROOM BOUNDARIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Lazily find boundary squares for the given room ID and cache them.
     * This avoids scanning all cells repeatedly for the same room.
     */
    private List<Point> findRoomBoundaryCached(int roomId) {
        List<Point> cached = roomBoundaryCache.get(roomId);
        if (cached != null) {
            return cached;
        }
        List<Point> boundary = findRoomBoundary(roomId);
        roomBoundaryCache.put(roomId, boundary);
        return boundary;
    }

    /**
     * Find boundary squares for a room: squares that are ROOM but
     * have at least one neighbor that is not in the same room.
     */
    private List<Point> findRoomBoundary(int roomId) {
        List<Point> boundary = new ArrayList<>();
        for (GridCell cell : gridMap.values()) {
            if (cell.getRoomId() == roomId) {
                Point p = cell.getCoordinate();
                if (isBoundary(p.x, p.y, roomId)) {
                    boundary.add(p);
                }
            }
        }
        return boundary;
    }

    private boolean isBoundary(int x, int y, int roomId) {
        // Check 4 directions
        int[][] deltas = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : deltas) {
            int nx = x + d[0];
            int ny = y + d[1];
            GridCell nc = getCell(nx, ny);
            // If no cell => treat as empty => boundary
            if (nc == null) {
                return true;
            }
            // If the neighbor is not the same room => boundary
            if (nc.getRoomId() != roomId) {
                return true;
            }
        }
        return false;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ GRID ACCESS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Get or create cell in O(1).
     */
    private GridCell getOrCreateCell(int x, int y) {
        Point key = new Point(x, y);
        GridCell cell = gridMap.get(key);
        if (cell == null) {
            cell = new GridCell(x, y);
            gridMap.put(key, cell);
        }
        return cell;
    }

    /**
     * Get cell in O(1), or null if it doesn't exist.
     */
    private GridCell getCell(int x, int y) {
        return gridMap.get(new Point(x, y));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ROOM DIMENSION PARSING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private Dimension parseRoomDimensions(String dims) {
        try {
            // e.g. "20' x 30'" => split on 'x'
            String[] tokens = dims.split("x");
            if (tokens.length == 2) {
                String left  = tokens[0].replaceAll("[^0-9]", "");
                String right = tokens[1].replaceAll("[^0-9]", "");
                int w = Integer.parseInt(left.trim());
                int h = Integer.parseInt(right.trim());
                return new Dimension(w, h);
            }
        } catch (Exception e) {
            // ignore
        }
        return new Dimension(5, 5);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ UTILS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Collection<GridCell> getAllCells() {
        return Collections.unmodifiableCollection(gridMap.values());
    }

    @Override
    public Rectangle getBounds() {
        return null;
    }


    /**
     * Simple integer dimension for (width, height).
     */
    private static class Dimension {
        final int width;
        final int height;
        Dimension(int w, int h) {
            this.width = w;
            this.height = h;
        }
    }
}
