package org.example;


import org.example.ui.SimpleUi;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LocalFileTransfer {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
            String name = args.length > 1 ? args[1]:"LAN Transferrer instance";
            SimpleUi ui = new SimpleUi(port, name);
            JFrame frame = new JFrame("LAN file transferrer:" + port);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(ui.getMainPanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    ui.shutdown();
                }
            });
        });

    }
}