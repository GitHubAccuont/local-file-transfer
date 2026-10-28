package org.example.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

public class SimpleUi {

    private JPanel mainPanel;

    private ConcurrentHashMap<String, String> chat;

    // Create connection part
    private JTextField ipField;
    private JTextField portField;
    private JPanel connectionParamsPanel;
    private JButton connectButton;


    // splits for UI
    private JPanel sidePanel;

    // Send file GUI parts
    private JPanel fileTransferPanel;
    private JButton selectButton; // Should reset selection on repeat
    private JLabel fileInfoLabel;
    private JButton sendFileButton;

    // Chat panel parts
    private JPanel chatPanel;
    private JScrollPane chatScroll;
    private JTextArea chatBox;
    private JTextField messageInput;
    private JButton sendMessageButton;
    private JPanel messageControls;

    //connection vars
    private Socket peerSocket;


    public SimpleUi() {
        setupUi();
    }

    private void setupUi() {

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setPreferredSize(new Dimension(900, 600));

        setupChat();
        mainPanel.add(chatPanel, BorderLayout.CENTER);

        setupSidePanel();
        mainPanel.add(sidePanel, BorderLayout.EAST);
        
    }

    private void setupSidePanel() {


        sidePanel = new JPanel(new BorderLayout(10, 10));
        sidePanel.setPreferredSize(new Dimension(350, 0));

        setupConnPanel();
        sidePanel.add(connectionParamsPanel, BorderLayout.NORTH);

        setupFileTransferPanel();
        sidePanel.add(fileTransferPanel, BorderLayout.CENTER);
    }

    private void setupConnPanel() {


        ipField = new JTextField(15);
        ipField.setToolTipText("Enter the ip you will be trying to connect to");

        portField = new JTextField(5);
        portField.setToolTipText("Enter the port of address you will be trying to connect to");

        connectButton = new JButton("Connect");

        connectionParamsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        connectionParamsPanel.add(ipField);
        connectionParamsPanel.add(portField);
        connectionParamsPanel.add(connectButton);
        addConnectionListeners();
    }

    private void addConnectionListeners() {
        connectButton.addActionListener(e -> {

        });
    }


    private void setupChat() {


        chatBox = new JTextArea(". . .");
        chatBox.setEditable(false);
        chatBox.setLineWrap(true);
        chatBox.setWrapStyleWord(true);

        chatScroll = new JScrollPane(chatBox, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);

        messageInput = new JTextField();
        messageInput.setToolTipText("Input your messages here");

        sendMessageButton = new JButton("Send");
        sendMessageButton.setToolTipText("Click here when connected to someone to send message");

        messageControls = new JPanel(new BorderLayout(6, 0));
        messageControls.add(messageInput, BorderLayout.CENTER);
        messageControls.add(sendMessageButton, BorderLayout.EAST);

        chatPanel = new JPanel(new BorderLayout(10, 10));
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(messageControls, BorderLayout.SOUTH);
    }

    private void setupFileTransferPanel() {

        selectButton = new JButton("Select Files");
        selectButton.setToolTipText("Click to select file for transfer");

        sendFileButton = new JButton("Send file");
        sendFileButton.setEnabled(false);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        buttonRow.add(selectButton);
        buttonRow.add(sendFileButton);

        fileInfoLabel = new JLabel("No files selected currently");

        fileTransferPanel = new JPanel();
        fileTransferPanel.setLayout(new BoxLayout(fileTransferPanel, BoxLayout.Y_AXIS));
        fileTransferPanel.add(fileInfoLabel);
        fileTransferPanel.add(buttonRow);
        fileTransferPanel.add(Box.createVerticalGlue());

        setupFileTransferListeners();
    }

    private void setupFileTransferListeners() {
        selectButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(mainPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                String name = selected.getName();
                long bytes = selected.length(); // size in bytes

                String sizeText = bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB";

                fileInfoLabel.setText("<html>" + name + "<br>(" + sizeText + ")</html>");
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void shutdown() {

    }

    private void appendMessage(String user, String messageText) {
        chatBox.append(String.format("[%s]:%s%n", user, messageText));
    }
}
