package org.example;


import org.example.ui.SimpleUi;

import javax.swing.*;

public class LocalFileTransfer {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("LAN file transferrer");

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(new SimpleUi().getMainPanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

    }
}