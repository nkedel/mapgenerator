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
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class DungeonGridViewer extends JFrame {

    private static final Logger LOG = Logger.getLogger(DungeonGridViewer.class.getName());

    // The random dungeon generator
    private final AdvancedDungeonGenerator generator;

    // The current in-memory dungeon object
    private Dungeon dungeon;

    // The current fitter (BFS or A*), and bounding rectangle
    private DungeonFitter fitter;
    private Rectangle bounds;

    // Panel to display the cells
    private final DungeonPanel dungeonPanel;

    // Jackson for JSON load/save
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Each cell is drawn as a square of this many pixels
    private static final int CELL_SIZE = 16;

    public DungeonGridViewer(AdvancedDungeonGenerator generator) {
        this.generator = generator;

        setTitle("Dungeon Grid Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Center: scrollable panel for dungeon
        dungeonPanel = new DungeonPanel();
        JScrollPane scrollPane = new JScrollPane(dungeonPanel);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: panel with buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // 1) Regenerate button
        JButton regenerateBtn = new JButton("Regenerate");
        regenerateBtn.addActionListener(this::onRegenerate);
        buttonPanel.add(regenerateBtn);

        // 2) Fit with BFS
        JButton fitBFSBtn = new JButton("Fit with BFS");
        fitBFSBtn.addActionListener(this::onFitWithBFS);
        buttonPanel.add(fitBFSBtn);

        // 3) Fit with AStar
        JButton fitAStarBtn = new JButton("Fit with AStar");
        fitAStarBtn.addActionListener(this::onFitWithAStar);
        buttonPanel.add(fitAStarBtn);

        // 4) Save PNG
        JButton savePngBtn = new JButton("Save as PNG");
        savePngBtn.addActionListener(this::onSaveAsPNG);
        buttonPanel.add(savePngBtn);

        // 5) Save JSON
        JButton saveJsonBtn = new JButton("Save JSON");
        saveJsonBtn.addActionListener(this::onSaveAsJSON);
        buttonPanel.add(saveJsonBtn);

        // 6) Load JSON
        JButton loadJsonBtn = new JButton("Load JSON");
        loadJsonBtn.addActionListener(this::onLoadJSON);
        buttonPanel.add(loadJsonBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        // Initially, generate a dungeon and fit with BFS by default (or none)
        regenerateDungeon(); // creates a new dungeon
        // (You could choose not to fit until user clicks BFS or AStar.)

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //               REGENERATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onRegenerate(ActionEvent e) {
        regenerateDungeon();
    }

    private void regenerateDungeon() {
        LOG.info("Regenerating a new dungeon...");
        dungeon = generator.generateDungeon();
        // Let's not fit automatically here, so the user can choose BFS or A*
        // If you prefer an immediate fit, call onFitWithBFS(null) or onFitWithAStar(null).
        clearFitterData();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //               FIT WITH BFS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onFitWithBFS(ActionEvent e) {
        if (dungeon == null) {
            LOG.warning("No dungeon in memory to fit!");
            return;
        }
        LOG.info("Fitting dungeon with BFS approach...");
        fitter = new DungeonGridFitter();
        bounds = fitter.fitDungeon(dungeon);
        updatePanelSizeAndRepaint();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //               FIT WITH A*
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onFitWithAStar(ActionEvent e) {
        if (dungeon == null) {
            LOG.warning("No dungeon in memory to fit!");
            return;
        }
        LOG.info("Fitting dungeon with A* approach...");
        fitter = new AStarDungeonGridFitter();
        bounds = fitter.fitDungeon(dungeon);
        updatePanelSizeAndRepaint();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //               SAVE PNG
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onSaveAsPNG(ActionEvent e) {
        if (fitter == null) {
            JOptionPane.showMessageDialog(this,
                    "No fitted layout to save. Fit the dungeon first.",
                    "No Fitter",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("dungeon.png"));
        int choice = fc.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            savePanelAsPNG(dungeonPanel, fc.getSelectedFile());
        }
    }

    private void savePanelAsPNG(JPanel panel, File outFile) {
        BufferedImage image = new BufferedImage(
                panel.getWidth(),
                panel.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        try {
            ImageIO.write(image, "png", outFile);
            JOptionPane.showMessageDialog(this,
                    "Saved to " + outFile.getAbsolutePath(),
                    "PNG Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //               SAVE JSON
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onSaveAsJSON(ActionEvent e) {
        if (fitter == null) {
            JOptionPane.showMessageDialog(this,
                    "No fitted layout to save. Fit the dungeon first.",
                    "No Fitter",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("dungeon.json"));
        int choice = fc.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File outFile = fc.getSelectedFile();
            saveDungeonAsJSON(outFile);
        }
    }

    private void saveDungeonAsJSON(File outFile) {
        try {
            DungeonGridData data = buildDungeonGridData();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outFile, data);

            JOptionPane.showMessageDialog(this,
                    "Saved JSON to " + outFile.getAbsolutePath(),
                    "JSON Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Gathers the bounding rect and all cells from the fitter
    // plus a list of rooms from the dungeon.
    private DungeonGridData buildDungeonGridData() {
        DungeonGridData data = new DungeonGridData();
        if (bounds != null) {
            data.rect = new RectDto(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        if (fitter != null) {
            List<GridCellDto> cellDtos = new ArrayList<>();
            for (GridCell c : fitter.getAllCells()) {
                GridCellDto dto = new GridCellDto();
                dto.x = c.getCoordinate().x;
                dto.y = c.getCoordinate().y;
                dto.roomId = c.getRoomId();
                dto.cellType = c.getCellType().name();
                cellDtos.add(dto);
            }
            data.cells = cellDtos;
        }
        if (dungeon != null) {
            List<RoomDto> roomDtos = new ArrayList<>();
            for (Room r : dungeon.getRooms()) {
                RoomDto rd = new RoomDto();
                rd.id = r.getId();
                rd.shape = (r.getShape() != null) ? r.getShape().name() : null;
                rd.dimensions = r.getDimensions();
                roomDtos.add(rd);
            }
            data.rooms = roomDtos;
        }
        return data;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //               LOAD JSON
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void onLoadJSON(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("dungeon.json"));
        int choice = fc.showOpenDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            loadDungeonFromJSON(fc.getSelectedFile());
        }
    }

    private void loadDungeonFromJSON(File inFile) {
        try {
            DungeonGridData data = objectMapper.readValue(inFile, DungeonGridData.class);

            // rebuild dungeon: new memory
            dungeon = new Dungeon();
            // Rebuild rooms
            if (data.rooms != null) {
                for (RoomDto rd : data.rooms) {
                    RoomShape shape;
                    try {
                        shape = (rd.shape != null) ? RoomShape.valueOf(rd.shape) : RoomShape.UNUSUAL;
                    } catch (Exception ex) {
                        shape = RoomShape.UNUSUAL;
                    }
                    // We'll create a new Room, hack the id if needed
                    Room newRoom = new Room(shape, rd.dimensions) {
                        @Override
                        public int getId() {
                            return rd.id;
                        }
                    };
                    dungeon.addRoom(newRoom);
                }
            }

            // We'll keep the cell data for the fitter, but to unify with BFS/AStar approach,
            // let's just store them in a separate map so we can display them if we want
            // OR we can choose to re-fit.
            // If you want to load the exact cell layout, you'd need a specialized approach
            // (like we did before), because BFS or AStar might recalc.
            // For now, let's just store them in memory to display:
            //
            loadedCells.clear();
            if (data.cells != null) {
                for (GridCellDto dto : data.cells) {
                    loadedCells.add(dto);
                }
            }

            // bounding rect from file
            if (data.rect != null) {
                loadedBounds = new Rectangle(
                        data.rect.x, data.rect.y, data.rect.width, data.rect.height);
            } else {
                loadedBounds = null;
            }

            // Clear the current fitter data => we have a new dungeon
            // (the user can now choose BFS or AStar if they want to re-fit)
            clearFitterData();

            JOptionPane.showMessageDialog(this,
                    "Loaded JSON from: " + inFile.getAbsolutePath(),
                    "Load Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading: " + ex.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // If you want to display the loaded cells exactly, you'd do it in the panel
    // if there's no new fitter. We'll keep them in memory and draw them if no fitter is chosen.
    private final List<GridCellDto> loadedCells = new ArrayList<>();
    private Rectangle loadedBounds = null;

    /**
     * Clears the current fitter, so we revert to no fitted data.
     */
    private void clearFitterData() {
        fitter = null;
        bounds = null;
        updatePanelSizeAndRepaint();
    }

    /**
     * Adjust dungeonPanel's size and repaint.
     */
    private void updatePanelSizeAndRepaint() {
        // If we have a fitter & bounds, size accordingly
        if (fitter != null && bounds != null) {
            int w = bounds.width * CELL_SIZE + 1;
            int h = bounds.height * CELL_SIZE + 1;
            dungeonPanel.setPreferredSize(new Dimension(w, h));
        }
        // else if we only have loadedCells, we might adapt to loadedBounds
        else if (!loadedCells.isEmpty() && loadedBounds != null) {
            int w = loadedBounds.width * CELL_SIZE + 1;
            int h = loadedBounds.height * CELL_SIZE + 1;
            dungeonPanel.setPreferredSize(new Dimension(w, h));
        } else {
            // default
            dungeonPanel.setPreferredSize(new Dimension(400, 300));
        }
        dungeonPanel.revalidate();
        dungeonPanel.repaint();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //               RENDER PANEL
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private class DungeonPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (fitter != null && bounds != null) {
                // We have an actively fitted layout. Draw from the fitter's cells.
                drawFitterCells(g);
            } else if (!loadedCells.isEmpty() && loadedBounds != null) {
                // We have a loaded layout from JSON, but haven't re-fitted.
                // Draw from loaded cells.
                drawLoadedCells(g);
            } else {
                // Nothing to show
                g.setColor(Color.DARK_GRAY);
                g.drawString("No layout to display. Generate or load + fit the dungeon.", 20, 20);
            }
        }

        private void drawFitterCells(Graphics g) {
            // We'll just loop over the bounding rect
            for (int row = 0; row < bounds.height; row++) {
                for (int col = 0; col < bounds.width; col++) {
                    int xGrid = bounds.x + col;
                    int yGrid = bounds.y + row;

                    // Find cell if it exists
                    GridCell cell = findCell(fitter.getAllCells(), xGrid, yGrid);
                    GridCell.CellType cellType = (cell != null)
                            ? cell.getCellType()
                            : GridCell.CellType.EMPTY;

                    int px = col * CELL_SIZE;
                    int py = row * CELL_SIZE;

                    Color fill = switch (cellType) {
                        case ROOM -> new Color(220, 220, 220);
                        case CORRIDOR -> new Color(200, 200, 255);
                        default -> new Color(48, 48, 48);
                    };
                    g.setColor(fill);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);

                    g.setColor(Color.BLACK);
                    g.drawRect(px, py, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        private void drawLoadedCells(Graphics g) {
            // We'll loop over loadedBounds
            for (int row = 0; row < loadedBounds.height; row++) {
                for (int col = 0; col < loadedBounds.width; col++) {
                    int xGrid = loadedBounds.x + col;
                    int yGrid = loadedBounds.y + row;

                    // find a matching loaded cell if it exists
                    GridCellDto dto = findLoadedCell(xGrid, yGrid);
                    String cellType = (dto != null) ? dto.cellType : "EMPTY";

                    int px = col * CELL_SIZE;
                    int py = row * CELL_SIZE;

                    Color fill;
                    switch (cellType) {
                        case "ROOM" -> fill = new Color(220, 220, 220);
                        case "CORRIDOR" -> fill = new Color(200, 200, 255);
                        default -> fill = new Color(48, 48, 48);
                    }
                    g.setColor(fill);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);

                    g.setColor(Color.BLACK);
                    g.drawRect(px, py, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        private GridCell findCell(Collection<GridCell> cells, int x, int y) {
            for (GridCell c : cells) {
                if (c.getCoordinate().x == x && c.getCoordinate().y == y) {
                    return c;
                }
            }
            return null;
        }

        private GridCellDto findLoadedCell(int x, int y) {
            for (GridCellDto dto : loadedCells) {
                if (dto.x == x && dto.y == y) {
                    return dto;
                }
            }
            return null;
        }
    }

    // Example main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create the generator
            AdvancedDungeonGenerator generator = new AdvancedDungeonGenerator();
            // Launch viewer
            new DungeonGridViewer(generator);
        });
    }
}
