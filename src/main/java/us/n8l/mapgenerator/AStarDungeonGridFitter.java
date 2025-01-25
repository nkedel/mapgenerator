package us.n8l.mapgenerator;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.alg.shortestpath.AStarShortestPath;

import java.awt.Point;
import java.util.*;
import java.util.logging.Logger;

/**
 * A demonstration of how to:
 *  1) Scale large rooms
 *  2) Randomly offset room placement
 *  3) Use a short "stub" corridor from the room boundary
 *  4) Use JGraphT's A* for corridor pathfinding, with a penalty near rooms
 */
public class AStarDungeonGridFitter implements DungeonFitter {
    private static final Logger LOG = Logger.getLogger(AStarDungeonGridFitter.class.getName());

    // Let’s pick some constants:
    private static final int MAX_DIMENSION = 30; // clamp room width/height to 30 ft
    private static final int ROOM_OFFSET_RANGE = 5; // up to ±5 squares random offset
    private static final int STUB_LENGTH_MIN = 1; // short corridor stubs from boundary
    private static final int STUB_LENGTH_MAX = 3; // up to 3 squares

    // The map of cells in (x,y) => GridCell
    private final Map<Point, GridCell> gridMap;

    // We store bounding rectangle after we finish
    private us.n8l.mapgenerator.Rectangle bounds;

    public AStarDungeonGridFitter() {
        this.gridMap = new HashMap<>();
    }

    /**
     * Main entry point: place rooms, connect with corridors (via A*), compute bounding rect.
     */
    public us.n8l.mapgenerator.Rectangle fitDungeon(Dungeon dungeon) {
        long startTime = System.currentTimeMillis();
        LOG.info("Starting dungeon fit with A* pathfinding.");

        // 1) Place rooms with random offsets & scaling
        placeAllRooms(dungeon.getRooms());

        // 2) Connect corridors using a stub offset + A*
        int connectedCount = 0;
        for (Corridor c : dungeon.getCorridors()) {
            connectCorridor(c);
            connectedCount++;
            if (connectedCount % 5 == 0) {
                LOG.info("... connected " + connectedCount + " corridors so far");
            }
        }

        // 3) Compute bounding rectangle
        this.bounds = computeUsedBounds();
        long endTime = System.currentTimeMillis();
        LOG.info("Dungeon fit complete. Used area: (" + bounds.x + "," + bounds.y + ") "
                + bounds.width + "x" + bounds.height
                + ". Elapsed ms: " + (endTime - startTime));

        return bounds;
    }

    private us.n8l.mapgenerator.Rectangle computeUsedBounds() {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
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
        if (maxX < minX || maxY < minY) {
            return new us.n8l.mapgenerator.Rectangle(0,0,0,0);
        }
        return new us.n8l.mapgenerator.Rectangle(minX, minY, (maxX - minX + 1), (maxY - minY + 1));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                        ROOM PLACEMENT
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * A naive approach: line up rooms in a row, but random offset them,
     * scale them down if they're too big, ensure no overlap by checking
     * collisions, etc. This is a demonstration, not a perfect packing.
     */
    private void placeAllRooms(List<Room> rooms) {
        LOG.info("Placing " + rooms.size() + " rooms with random offsets & scaling down large ones.");

        final int MAX_ROW_WIDTH = 100;
        int currentX = 0;
        int currentY = 0;
        int tallestRoomInRow = 0;

        for (Room room : rooms) {
            // Parse the dimension string
            Dimension dims = parseRoomDimensions(room.getDimensions());
            // scale if > MAX_DIMENSION
            if (dims.width > MAX_DIMENSION) {
                dims.width = MAX_DIMENSION;
            }
            if (dims.height > MAX_DIMENSION) {
                dims.height = MAX_DIMENSION;
            }

            // random offset (± ROOM_OFFSET_RANGE)
            int offsetX = (int) (Math.random() * (2 * ROOM_OFFSET_RANGE + 1)) - ROOM_OFFSET_RANGE;
            int offsetY = (int) (Math.random() * (2 * ROOM_OFFSET_RANGE + 1)) - ROOM_OFFSET_RANGE;

            int proposedX = currentX + offsetX;
            int proposedY = currentY + offsetY;
            // we can try multiple times if there's a collision, but let's do once for demo
            placeRoom(room.getId(), dims.width, dims.height, proposedX, proposedY);

            // move "cursor" for next room
            currentX += (dims.width + 5);
            if (dims.height > tallestRoomInRow) {
                tallestRoomInRow = dims.height;
            }
            if (currentX > MAX_ROW_WIDTH) {
                currentX = 0;
                currentY += (tallestRoomInRow + 5);
                tallestRoomInRow = 0;
            }
        }
        LOG.info("Done placing rooms.");
    }

    /**
     * Place a single room. If collision, we do a naive overwrite for demo,
     * but you can do a collision check or shuffle the position further.
     */
    private void placeRoom(int roomId, int w, int h, int startX, int startY) {
        for (int y = startY; y < startY + h; y++) {
            for (int x = startX; x < startX + w; x++) {
                GridCell cell = getOrCreateCell(x, y);
                cell.setCellType(GridCell.CellType.ROOM);
                cell.setRoomId(roomId);
            }
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                     CORRIDOR CONNECTIONS (A*)
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void connectCorridor(Corridor corridor) {
        Room from = corridor.getFromRoom();
        Room to   = corridor.getToRoom();
        if (from == null || to == null) return;

        List<Point> fromBound = findRoomBoundary(from.getId());
        List<Point> toBound   = findRoomBoundary(to.getId());

        if (fromBound.isEmpty() || toBound.isEmpty()) {
            LOG.fine("No boundary squares for corridor: " + from.getId() + "->" + to.getId());
            return;
        }

        // pick random boundary square from each (or the first for demo)
        Point startBoundary = fromBound.get(0);
        Point endBoundary   = toBound.get(0);

        // create short stubs from boundary squares
        Point startStub = createStub(startBoundary);
        Point endStub   = createStub(endBoundary);

        // Now use A* from startStub -> endStub
        List<Point> path = aStarPath(startStub, endStub);

        // Mark corridor squares
        for (Point p : path) {
            GridCell c = getOrCreateCell(p.x, p.y);
            if (c.getCellType() == GridCell.CellType.EMPTY
                || c.getCellType() == GridCell.CellType.CORRIDOR) {
                c.setCellType(GridCell.CellType.CORRIDOR);
            }
        }
    }

    /**
     * Make a short corridor "stub" (1..3 squares) outward from the boundary cell.
     * For simplicity, pick a random direction that doesn't immediately hit another room cell.
     * Return the last square in the stub.
     */
    private Point createStub(Point boundaryCell) {
        int stubLen = STUB_LENGTH_MIN + (int)(Math.random() * (STUB_LENGTH_MAX - STUB_LENGTH_MIN + 1));
        // pick a random direction (N, S, E, W)
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        int[] chosen = dirs[(int)(Math.random() * dirs.length)];

        Point current = boundaryCell;
        for (int i = 0; i < stubLen; i++) {
            int nx = current.x + chosen[0];
            int ny = current.y + chosen[1];
            GridCell nc = getOrCreateCell(nx, ny);
            // if we ran into a room cell (not the original boundary?), we break
            if (nc.getCellType() == GridCell.CellType.ROOM && i > 0) {
                break;
            } else {
                // mark corridor
                if (nc.getCellType() != GridCell.CellType.ROOM) {
                    nc.setCellType(GridCell.CellType.CORRIDOR);
                }
                current = new Point(nx, ny);
            }
        }
        return current;
    }

    /**
     * Use JGraphT's A* to find a path from start->goal with a custom "penalty near rooms".
     * This requires building a graph on-the-fly or caching. We do it on the fly for demo.
     */
    private List<Point> aStarPath(Point start, Point goal) {
        if (start.equals(goal)) {
            return List.of(start);
        }

        // 1) Build a directed weighted graph
        Graph<Point, DefaultWeightedEdge> graph =
                new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        // We can limit the search area to a bounding box around start & goal
        // to avoid huge expansions. For demo, we do a simple approach:
        // build a region from (minX..maxX), (minY..maxY) where minX= Math.min(start.x,goal.x)-someMargin, etc.
        int minX = Math.min(start.x, goal.x) - 20;
        int maxX = Math.max(start.x, goal.x) + 20;
        int minY = Math.min(start.y, goal.y) - 20;
        int maxY = Math.max(start.y, goal.y) + 20;

        // 2) Create vertices
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Point p = new Point(x,y);
                // We'll allow corridor or empty or the start/goal if it's a room boundary
                GridCell c = getCell(x, y);
                if (canTraverseCell(p, c, start, goal)) {
                    graph.addVertex(p);
                }
            }
        }

        // 3) Add edges (4 directions). Weighted with a "penalty near room."
        for (Point v : graph.vertexSet()) {
            for (int[] d : new int[][] {{1,0},{-1,0},{0,1},{0,-1}}) {
                Point w = new Point(v.x + d[0], v.y + d[1]);
                if (graph.containsVertex(w)) {
                    double cost = costForCell(w);
                    DefaultWeightedEdge e = graph.addEdge(v, w);
                    if (e != null) {
                        graph.setEdgeWeight(e, cost);
                    }
                }
            }
        }

        // 4) A* Shortest Path
        AStarShortestPath<Point, DefaultWeightedEdge> astar =
                new AStarShortestPath<>(graph, (p1, p2) -> manhattanDistance(p1, p2));

        var sp = astar.getPath(start, goal);
        if (sp == null) {
            return Collections.emptyList();
        } else {
            return sp.getVertexList();
        }
    }

    /**
     * Return true if we can traverse this cell. We'll allow corridor or empty squares,
     * plus the start/goal squares if they happen to be "corridor stubs" next to a room.
     */
    private boolean canTraverseCell(Point p, GridCell c, Point start, Point goal) {
        // If cell is null => treat as empty => passable
        if (c == null) return true;
        if (p.equals(start) || p.equals(goal)) return true;

        // If it's a room, no, except for the start/goal boundary
        if (c.getCellType() == GridCell.CellType.ROOM) {
            return false;
        }
        // corridor or empty => passable
        return true;
    }

    /**
     * Weighted cost for a cell. If adjacent to a room, cost is higher to push corridors away from rooms.
     * Otherwise cost = 1.0.
     */
    private double costForCell(Point p) {
        // Check if adjacent to room
        for (int[] d : new int[][] {{1,0},{-1,0},{0,1},{0,-1}}) {
            GridCell nc = getCell(p.x + d[0], p.y + d[1]);
            if (nc != null && nc.getCellType() == GridCell.CellType.ROOM) {
                // penalize adjacency
                return 5.0;
            }
        }
        // default cost = 1
        return 1.0;
    }

    // Manhattan distance heuristic for A*
    private double manhattanDistance(Point p1, Point p2) {
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                 ROOM BOUNDARY DETECTION
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private List<Point> findRoomBoundary(int roomId) {
        // squares in the map with roomId that have at least one neighbor not in that room
        List<Point> boundary = new ArrayList<>();
        for (GridCell cell : gridMap.values()) {
            if (cell.getRoomId() == roomId) {
                if (isBoundary(cell.getCoordinate(), roomId)) {
                    boundary.add(cell.getCoordinate());
                }
            }
        }
        return boundary;
    }

    private boolean isBoundary(Point p, int roomId) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = p.x + d[0];
            int ny = p.y + d[1];
            GridCell ncell = getCell(nx, ny);
            if (ncell == null) {
                return true; // edge => boundary
            }
            if (ncell.getRoomId() != roomId) {
                // either another room or corridor/empty
                return true;
            }
        }
        return false;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                       GRID ACCESS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private GridCell getOrCreateCell(int x, int y) {
        Point pt = new Point(x,y);
        GridCell c = gridMap.get(pt);
        if (c == null) {
            c = new GridCell(x, y);
            gridMap.put(pt, c);
        }
        return c;
    }

    private GridCell getCell(int x, int y) {
        return gridMap.get(new Point(x,y));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                    ROOM DIMENSION PARSING
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private Dimension parseRoomDimensions(String dims) {
        // same as before, parse "20' x 30'" => (20,30)
        try {
            String[] tokens = dims.split("x");
            if (tokens.length == 2) {
                int w = Integer.parseInt(tokens[0].replaceAll("[^0-9]", ""));
                int h = Integer.parseInt(tokens[1].replaceAll("[^0-9]", ""));
                return new Dimension(w, h);
            }
        } catch (Exception e) {
            // fallback
        }
        return new Dimension(5, 5);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                  EXPOSED DATA & CLASSES
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Returns an unmodifiable view of all cells in the fitted grid.
     */
    public Collection<GridCell> getAllCells() {
        return Collections.unmodifiableCollection(gridMap.values());
    }

    public us.n8l.mapgenerator.Rectangle getBounds() {
        return bounds;
    }

    /**
     * Basic integer dimension
     */
    private static class Dimension {
        public int width, height;
        public Dimension(int w, int h) {
            this.width = w; this.height = h;
        }
    }

}
