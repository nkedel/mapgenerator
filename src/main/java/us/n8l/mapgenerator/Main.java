package us.n8l.mapgenerator;

import javax.swing.*;

public class Main {
    // Example "main" to launch viewer:
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdvancedDungeonGenerator generator = new AdvancedDungeonGenerator();
            new DungeonGridViewer(generator);
        });
    }
}
