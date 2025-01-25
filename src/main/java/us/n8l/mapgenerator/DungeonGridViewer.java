package us.n8l.mapgenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Swing-based viewer that:
 *  - Generates & fits a dungeon (Regenerate button)
 *  - Displays the cells in a scrollable panel
 *  - Allows saving/loading of the fitted grid & room metadata in JSON
 *  - Allows saving a PNG screenshot
 */
public class DungeonGridViewer extends JFrame {

    private final AdvancedDungeonGenerator generator;  // For random dungeon generation
    private Dungeon dungeon;                           // Current dungeon object
    private DungeonGridFitter fitter;                  // Current fitter
    private Rectangle bounds;        // Current bounding rectangle

    // The drawing panel which shows the dungeon as a colored grid
    private final DungeonPanel dungeonPanel;
    // Each cell is drawn as a square of this many pixels
    private static final int CELL_SIZE = 16;

    // Jackson ObjectMapper for reading/writing JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DungeonGridViewer(AdvancedDungeonGenerator generator) {
        this.generator = generator;

        setTitle("Dungeon Grid Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Center: scrollable panel with the dungeon display
        dungeonPanel = new DungeonPanel();
        JScrollPane scrollPane = new JScrollPane(dungeonPanel);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: panel with buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // 1) Regenerate button
        JButton regenerateButton = new JButton("Regenerate");
        regenerateButton.addActionListener(this::onRegenerate);
        buttonPanel.add(regenerateButton);

        // 2) Save PNG button
        JButton savePngButton = new JButton("Save as PNG");
        savePngButton.addActionListener(this::onSaveAsPNG);
        buttonPanel.add(savePngButton);

        // 3) Save JSON button
        JButton saveJsonButton = new JButton("Save JSON");
        saveJsonButton.addActionListener(this::onSaveAsJSON);
        buttonPanel.add(saveJsonButton);

        // 4) Load JSON button
        JButton loadJsonButton = new JButton("Load JSON");
        loadJsonButton.addActionListener(this::onLoadJSON);
        buttonPanel.add(loadJsonButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Generate the initial dungeon
        regenerateDungeon();

        // Final window setup
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                    REGENERATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onRegenerate(ActionEvent e) {
        regenerateDungeon();
    }

    private void regenerateDungeon() {
        // 1) Generate new dungeon
        dungeon = generator.generateDungeon();

        // 2) Fit the dungeon
        fitter = new DungeonGridFitter();
        bounds = fitter.fitDungeon(dungeon);

        // 3) Resize panel & repaint
        updatePanelSizeAndRepaint();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                    SAVE PNG
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onSaveAsPNG(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("dungeon.png"));

        int choice = fileChooser.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            savePanelAsPNG(dungeonPanel, outputFile);
        }
    }

    private void savePanelAsPNG(JPanel panel, File file) {
        BufferedImage image = new BufferedImage(
                panel.getWidth(),
                panel.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        try {
            ImageIO.write(image, "png", file);
            JOptionPane.showMessageDialog(this,
                    "Saved PNG to " + file.getAbsolutePath(),
                    "Save Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error saving PNG: " + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                    SAVE JSON
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onSaveAsJSON(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("dungeon.json"));

        int choice = fileChooser.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            saveDungeonAsJSON(outputFile);
        }
    }

    /**
     * Collects the fitted grid + room metadata into a JSON structure
     * and writes it to file.
     */
    private void saveDungeonAsJSON(File file) {
        try {
            // 1) Build a data object that has all info we need
            DungeonGridData data = buildDungeonGridData();

            // 2) Write as JSON
            objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(file, data);

            JOptionPane.showMessageDialog(this,
                    "Saved JSON to " + file.getAbsolutePath(),
                    "Save Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error saving JSON: " + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Build a data object that holds:
     *  - bounding rectangle
     *  - list of GridCellDto (x, y, cellType, roomId)
     *  - list of room metadata (id, shape, dimensions)
     */
    private DungeonGridData buildDungeonGridData() {
        DungeonGridData data = new DungeonGridData();

        // 1) bounding rectangle
        if (bounds != null) {
            data.rect = new RectDto(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        // 2) all cells
        if (fitter != null) {
            List<GridCellDto> cellList = new ArrayList<>();
            for (GridCell cell : fitter.getAllCells()) {
                GridCellDto dto = new GridCellDto();
                dto.x = cell.getCoordinate().x;
                dto.y = cell.getCoordinate().y;
                dto.roomId = cell.getRoomId();
                dto.cellType = cell.getCellType().name(); // store as string
                cellList.add(dto);
            }
            data.cells = cellList;
        }

        // 3) room metadata from the Dungeon
        if (dungeon != null) {
            List<RoomDto> roomList = new ArrayList<>();
            for (Room r : dungeon.getRooms()) {
                RoomDto rd = new RoomDto();
                rd.id = r.getId();
                rd.shape = (r.getShape() != null) ? r.getShape().name() : null;
                rd.dimensions = r.getDimensions();
                roomList.add(rd);
            }
            data.rooms = roomList;
        }

        return data;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //                    LOAD JSON
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onLoadJSON(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("dungeon.json"));

        int choice = fileChooser.showOpenDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File inputFile = fileChooser.getSelectedFile();
            loadDungeonFromJSON(inputFile);
        }
    }

    /**
     * Reads the JSON data, reconstructs a "fitted" grid
     * (no new generation or fitting needed), and displays it.
     */
    private void loadDungeonFromJSON(File file) {
        try {
            DungeonGridData data = objectMapper.readValue(file, DungeonGridData.class);

            // 1) Rebuild bounding rect
            if (data.rect != null) {
                bounds = new Rectangle(
                        data.rect.x, data.rect.y, data.rect.width, data.rect.height);
            }

            // 2) Rebuild the fitter's cells
            //    We'll create a brand new fitter object and fill it with cells from JSON
            fitter = new DungeonGridFitter();

            if (data.cells != null) {
                for (GridCellDto dto : data.cells) {
                    // Create or get cell
                    GridCell cell = getOrCreateFitterCell(dto.x, dto.y);
                    // Set fields
                    cell.setRoomId(dto.roomId);
                    cell.setCellType(Enum.valueOf(GridCell.CellType.class, dto.cellType));
                }
            }

            // 3) Rebuild the Dungeon's rooms (if we want to see them in the "dungeon" object).
            //    This is optional, but let's do it so we can maintain that data in memory.
            dungeon = new Dungeon();
            if (data.rooms != null) {
                for (RoomDto rd : data.rooms) {
                    // Recreate the Room object
                    // We'll assume the shape enum is the same as in your code (RoomShape).
                    // If so, parse it. If null, default to something.
                    RoomShape shape;
                    try {
                        shape = (rd.shape != null)
                              ? RoomShape.valueOf(rd.shape)
                              : RoomShape.UNUSUAL; // fallback
                    } catch (Exception ex) {
                        shape = RoomShape.UNUSUAL;
                    }

                    Room newRoom = new Room(shape, rd.dimensions) {
                        @Override
                        public int getId() {
                            return rd.id; // override to keep the same ID
                        }
                    };
                    // We do a small trick above, or we could hack the static ID counter.
                    // For robust usage, you might adjust your Room class so you can set the ID.

                    // Add the room to the dungeon
                    dungeon.addRoom(newRoom);
                }
            }

            // 4) Update the panel
            updatePanelSizeAndRepaint();

            JOptionPane.showMessageDialog(this,
                    "Loaded JSON from " + file.getAbsolutePath(),
                    "Load Successful",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading JSON: " + ex.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates or gets a cell from the fitter’s map in O(1).
     * Since DungeonGridFitter’s map is private, we replicate the logic here
     * or add a public method in DungeonGridFitter to do it for us.
     * For demonstration, we do it inline:
     */
    private GridCell getOrCreateFitterCell(int x, int y) {
        // If you have a public method in DungeonGridFitter, call it.
        // Otherwise, we replicate the internal code:
        //   e.g. fitter.getCell(x,y) or fitter.getOrCreateCell(x,y)
        //
        // For demonstration, we do something like:

        // We do a quick linear search. Not super efficient for large maps,
        // but for demonstration it's fine.
        // ***Better approach***: add a public getOrCreateCell(x,y) method in DungeonGridFitter.

        for (GridCell c : fitter.getAllCells()) {
            if (c.getCoordinate().x == x && c.getCoordinate().y == y) {
                return c;
            }
        }
        // Not found -> create a new one. We can replicate the code
        // "gridMap.put(...)" if we had access, but it's private.
        // So we do a small reflection or some hack.
        // For demonstration, let's do a new GridCell, store it in a local list.
        // Then we won't actually store it in fitter's map.
        // => This means we can't handle BFS or corridor modifications after load
        // This is a limitation of not exposing the map.
        //
        // Ideally, you'd modify DungeonGridFitter with a public method:
        //     public GridCell getOrCreateCell(int x, int y) { ... }
        //
        // We'll do a compromise:
        GridCell newCell = new GridCell(x, y);
        // We can do a quick hack: reflect into the 'gridMap' if you want.
        // Or store them in a local map inside the viewer.
        // For simplicity, let's store in a local map:
        loadedCells.add(newCell);
        return newCell;
    }

    // We'll store newly created cells here
    private final List<GridCell> loadedCells = new ArrayList<>();

    /**
     * Recompute panel size based on bounding rectangle and repaint.
     */
    private void updatePanelSizeAndRepaint() {
        if (bounds != null) {
            int prefWidth = bounds.width * CELL_SIZE + 1;
            int prefHeight = bounds.height * CELL_SIZE + 1;
            dungeonPanel.setPreferredSize(new Dimension(prefWidth, prefHeight));
        } else {
            dungeonPanel.setPreferredSize(new Dimension(400, 300));
        }
        dungeonPanel.revalidate();
        dungeonPanel.repaint();
    }

    /**
     * A custom panel that draws the dungeon/fitter's cells.
     */
    private class DungeonPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // If we have no fitter or bounds, no map to draw
            if (fitter == null || bounds == null) {
                return;
            }

            // We'll combine:
            //  - The cells from fitter (fitter.getAllCells())
            //  - The "loadedCells" if we have any
            // so we can see them even if we didn't re-run BFS, etc.
            // We'll unify them in a map by (x,y).
            // Because we can't directly inject them into the fitter's private map
            // unless we modify the fitter's code.

            // Build a combined lookup
            // Prefer the fitter's cell if it exists; otherwise use loadedCells
            java.util.Map<Point, GridCell> drawMap = new java.util.HashMap<>();
            for (GridCell c : fitter.getAllCells()) {
                drawMap.put(c.getCoordinate(), c);
            }
            for (GridCell c : loadedCells) {
                drawMap.put(c.getCoordinate(), c);
            }

            // Now draw from bounding rectangle
            for (int row = 0; row < bounds.height; row++) {
                for (int col = 0; col < bounds.width; col++) {
                    int xGrid = bounds.x + col;
                    int yGrid = bounds.y + row;

                    Point pt = new Point(xGrid, yGrid);
                    GridCell cell = drawMap.get(pt);

                    GridCell.CellType type = GridCell.CellType.EMPTY;
                    if (cell != null) {
                        type = cell.getCellType();
                    }

                    Color fillColor;
                    switch (type) {
                        case ROOM -> fillColor = new Color(220, 220, 220); // light gray
                        case CORRIDOR -> fillColor = new Color(200, 200, 255); // light blue
                        default -> fillColor = new Color(48, 48, 48); // dark gray
                    }

                    int px = col * CELL_SIZE;
                    int py = row * CELL_SIZE;
                    g.setColor(fillColor);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);

                    g.setColor(Color.BLACK);
                    g.drawRect(px, py, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    // Example MAIN method to launch the viewer:
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdvancedDungeonGenerator generator = new AdvancedDungeonGenerator();
            new DungeonGridViewer(generator);
        });
    }
}
