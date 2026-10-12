package org.example.ui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleUi {

    private JPanel MainPanel;

    private ConcurrentHashMap<String, String> chat;

    public SimpleUi(){
        setupUi();
    }

    private void setupUi() {

        MainPanel = new JPanel();
        MainPanel.setLayout(new BorderLayout(0, 0));

        setupConnectioPanel();
//        setupChat();
        setupInputBox();
    }


    private void setupConnectioPanel() {
        JPanel connectionPanel = new JPanel();
        JTextField adressField;
        JTextField portField;
        JButton connectButton;
    }
    private void setupChat() {
    }


    private void setupInputBox() {
    }

    public JPanel getMainPanel() {
        return MainPanel;
    }
}
