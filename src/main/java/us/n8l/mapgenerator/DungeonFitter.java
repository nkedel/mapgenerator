package us.n8l.mapgenerator;

import java.util.Collection;

/**
 * An interface for any "dungeon fitter" which places the abstract Dungeon graph
 * onto a 2D grid and returns the resulting cells.
 */
public interface DungeonFitter {

    /**
     * Perform the fitting algorithm on the given Dungeon, returning
     * a bounding rectangle that covers all placed squares.
     */
    Rectangle fitDungeon(Dungeon dungeon);

    /**
     * Returns an unmodifiable collection of all GridCells after fitting.
     */
    Collection<GridCell> getAllCells();

    /**
     * Returns the bounding rectangle of the fitted grid,
     * or null/empty if not yet fitted.
     */
    Rectangle getBounds();
}
