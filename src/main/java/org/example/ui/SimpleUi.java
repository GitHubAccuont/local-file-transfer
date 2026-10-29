package org.example.ui;

import org.example.transfer.connection.ConnectionService;
import org.example.transfer.connection.entity.frame.Frame;
import org.example.transfer.utils.FileReceiver;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static org.example.transfer.utils.FrameCodec.*;

public class SimpleUi {

    private final static int DEFAULT_PORT = 0;
    boolean reading;
    // Utils classes (1 for now)
    FileReceiver fileReceiver;
    private JPanel mainPanel;
    private JPanel sidePanel;
    // Create connection part
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;
    private File selected;
    // Send file GUI parts
    private JButton selectButton;
    private JLabel fileInfoLabel;
    private JButton sendFileButton;
    // Chat panel parts
    private JScrollPane chatScroll;
    private JTextArea chatBox;
    private JTextField messageInput;
    private JButton sendMessageButton;
    //connection related
    private Socket peerSocket;
    private ConnectionService connectionService;


    public SimpleUi(int port) {
        setupUi();

        fileReceiver = new FileReceiver();
        try {
            connectionService = new ConnectionService();
            connectionService.start(port, this::handleFrame);
        } catch (IOException e) {
        }
    }

    private void setupUi() {

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setPreferredSize(new Dimension(900, 600));

        setupChat();

        setupSidePanel();
        mainPanel.add(sidePanel, BorderLayout.EAST);

    }

    private void setupSidePanel() {


        sidePanel = new JPanel(new BorderLayout(10, 10));
        sidePanel.setPreferredSize(new Dimension(350, 0));

        setupConnPanel();

        setupFileTransferPanel();

    }

    private void setupConnPanel() {


        ipField = new JTextField(15);
        ipField.setToolTipText("Enter the ip you will be trying to connect to");

        portField = new JTextField(5);
        portField.setToolTipText("Enter the port of address you will be trying to connect to");

        connectButton = new JButton("Connect");


        JPanel connectionParamsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));

        connectionParamsPanel.add(ipField);
        connectionParamsPanel.add(portField);
        connectionParamsPanel.add(connectButton);

        sidePanel.add(connectionParamsPanel, BorderLayout.NORTH);
        addConnectionListeners();
    }

    private void addConnectionListeners() {
        connectButton.addActionListener(e -> {
            if (peerSocket != null) {
                onConnectionClosed();
                return;
            }
            String host = ipField.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                appendMessage("system", "invalid port");
                return;
            }

            connectButton.setEnabled(false);
            connectButton.setText("Connecting...");
            appendMessage("system", "connecting to " + host + ":" + port + " ...");

            new Thread(() -> {
                try {
                    Socket s = new Socket(host, port);
                    SwingUtilities.invokeLater(() -> onConnected(s));
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> onConnectFailed(ex));
                }
            }, "connect-thread").start();
        });
    }


    private void startRecieving(Socket s) {
        new Thread(() -> {
            try (InputStream in = s.getInputStream()) {
                while (reading) {
                    Frame f = readFrame(in);
                    handleFrame(f);          // see bug 2
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> onConnectionClosed());
            }
        }, "peer-reader").start();
    }


    private void setupChat() {

        JPanel chatPanel;

        JPanel messageControls;

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
        mainPanel.add(chatPanel, BorderLayout.CENTER);


        setupChatControls();
    }

    private void setupChatControls() {

        sendMessageButton.addActionListener(e -> {

            sendMessage(messageInput.getText());
            messageInput.setText("");
        });
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


        JPanel fileTransferPanel = new JPanel();
        fileTransferPanel.setLayout(new BoxLayout(fileTransferPanel, BoxLayout.Y_AXIS));
        fileTransferPanel.add(fileInfoLabel);
        fileTransferPanel.add(buttonRow);
        fileTransferPanel.add(Box.createVerticalGlue());

        sidePanel.add(fileTransferPanel, BorderLayout.CENTER);

        setupFileTransferListeners();
    }

    private void setupFileTransferListeners() {
        selectButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(mainPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                selected = chooser.getSelectedFile();
                String name = selected.getName();
                long bytes = selected.length(); // size in bytes

                String sizeText = bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB";
                fileInfoLabel.setText("<html>" + name + "<br>(" + sizeText + ")</html>");
                sendFileButton.setEnabled(true);
            } else {
                sendFileButton.setEnabled(false);
            }
        });

        sendFileButton.addActionListener(e -> {
            if (selected != null && selected.isFile()) {
                sendFile();
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void shutdown() {
        try {
            connectionService.stop();
            onConnectionClosed();
        } catch (IOException | InterruptedException e) {
        }
    }

    //Util methods
    private void appendMessage(String user, String messageText) {
        chatBox.append(String.format("[%s]:%s%n", user, messageText));
    }

    private void handleFrame(Frame f) {

        try {
            switch (f.getType()) {
                case MESSAGE:
                    String text = new String(f.getPayload(), StandardCharsets.UTF_8);
                    SwingUtilities.invokeLater(() -> appendMessage("remote", text));
                    break;
                case FILE_HEADER:
                    fileReceiver.startProcessing(f);
                    break;
                case FILE_CHUNK:
                    fileReceiver.processNextFrame(f);
                    break;
                case FILE_END:
                    fileReceiver.endProcessing(f);
                    SwingUtilities.invokeLater(() -> appendMessage("system", "file recieved"));
                    break;
                default:
                    throw new IOException("Incorrect frame was caught");
            }
        } catch (IOException e) {
            fileReceiver.reset();
            SwingUtilities.invokeLater(() -> appendMessage("system", "transfer failed: " + e.getMessage()));
        }
    }

    // Methods related to sending

    private void sendMessage(String text) {
        try {
            if (peerSocket != null && peerSocket.isBound()) {
                writeMessageAsFrame(text, peerSocket.getOutputStream());
                appendMessage("me", text);
            }
        } catch (IOException e) {
            appendMessage("system", "send failed: " + e.getMessage());
            onConnectionClosed();
        }
    }

    private void sendFile() {
        if (selected == null || peerSocket == null) return;
        File f = selected;
        selected = null;
        sendFileButton.setEnabled(false);
        appendMessage("system", "sending " + f.getName() + " ...");
        Socket s = peerSocket;
        new Thread(() -> {
            try {

                writeFileAsFrame(s.getOutputStream(), f);
                SwingUtilities.invokeLater(() -> appendMessage("system", "sent " + f.getName()));

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> appendMessage("system", "send failed: " + ex.getMessage()));
            }
        }, "file-send").start();
    }


    //Methods related to connection inside UI, will be moved and handled in separate class in future versions

    private void onConnectFailed(IOException ex) {
        appendMessage("system", "connect failed: " + ex.getMessage());
        connectButton.setEnabled(true);
        connectButton.setText("Connect");
        reading = false;
    }


    private void onConnected(Socket s) {
        peerSocket = s;
        reading = true;
        startRecieving(s);
        appendMessage("system", "connected");
        connectButton.setText("Disconnect");
        connectButton.setEnabled(true);
        messageInput.setEnabled(true);
        sendMessageButton.setEnabled(true);
        selectButton.setEnabled(true);
        sendFileButton.setEnabled(selected != null);
    }


    private void onConnectionClosed() {
        Socket s = peerSocket;
        peerSocket = null;
        reading = false;
        if (s != null) try {
            s.close();
        } catch (IOException ignored) {
        }
        fileReceiver.reset();
        appendMessage("system", "connection closed");
        connectButton.setText("Connect");
        connectButton.setEnabled(true);
        messageInput.setEnabled(false);
        sendMessageButton.setEnabled(false);
        selectButton.setEnabled(false);
        sendFileButton.setEnabled(false);
    }
}
