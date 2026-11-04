package org.example.ui;

import org.example.transfer.dto.Connection;
import org.example.transfer.dto.Peer;
import org.example.transfer.dto.frame.Frame;
import org.example.transfer.dto.frame.FrameType;
import org.example.transfer.service.ConnectionServer;
import org.example.transfer.service.DiscoveryService;
import org.example.transfer.utils.ReceiveUtils;
import org.example.ui.element.DiscoveryPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static org.example.transfer.utils.ReceiveUtils.frameToMessage;
import static org.example.transfer.utils.SendUtils.sendFileAsFrames;
import static org.example.transfer.utils.SendUtils.sendMessageAsFrames;


/**
 * UI class for the app, it should work correctly since it's not using any dependencies and does not have
 * a lot of borders/ pixel offsets that could mess the GUI after resizing.
 * There are some commentaries, but it's better to just run app and see UI before trying to get it by code. UI stores some of the classes
 * inside as fields since we need link between UI and services that callback for UI updates upon any connection/disovery.
 * Those classes are {@link ConnectionServer} and {@link DiscoveryService}. There is also a small implementation for {@link org.example.transfer.dto.Connection.ConnectionListener}
 * which is used to trace back to UI any new connection and reflect it appropriately.
 * All the connections calls within UI are made in new threads to prevent clogging the UI's EDT.
 *
 */
public class SimpleUi {


    private final DiscoveryService discoveryService;
    private final ConnectionServer connectionServer;
    // Utils classes (1 for now as only recieving requires any states to keep going)
    ReceiveUtils receiveUtils;
    // Discovery service parts
    DiscoveryPanel discoveryPanel;
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
    private JTextArea chatBox;
    private JTextField messageInput;
    private JButton sendMessageButton;

    private final static Path DEFAULT_PATH = Path.of(System.getProperty("user.home"), "received");
    //connection related vars
    private Connection activeConnection;
    private final Connection.ConnectionListener connectionListener;


    public SimpleUi(int port, String name) {
        setupUi();

        discoveryService = new DiscoveryService(name, port, new DiscoveryService.Listener() {
            @Override
            public void onPeerDiscovered(Peer p) {
                SwingUtilities.invokeLater(() -> discoveryPanel.addPeer(p));
            }

            @Override
            public void onPeerLost(Peer p) {
                SwingUtilities.invokeLater(() -> discoveryPanel.removePeer(p));
            }
        });

        this.connectionListener = new Connection.ConnectionListener() {
            @Override
            public void onFrame(Frame f) throws IOException {
                if (f.getType() == FrameType.MESSAGE) {
                    SwingUtilities.invokeLater(() -> appendMessage("remote", frameToMessage(f)));
                } else {
                    receiveUtils.processFrame(f);
                }
            }

            @Override
            public void onClosed() {
                SwingUtilities.invokeLater(SimpleUi.this::onConnectionClosed);
            }
        };

        receiveUtils = new ReceiveUtils(DEFAULT_PATH, new ReceiveUtils.ReceiveListener() {
            @Override
            public void onFileStarted(String fileName, long expectedSize) {
                SwingUtilities.invokeLater(() -> appendMessage("system", "Receiving " + fileName + " (" + expectedSize + " bytes)..."));
            }

            @Override
            public void onFileFinished(String fileName) {
                SwingUtilities.invokeLater(() -> appendMessage("system", "File " + fileName + " received successfully"));
            }

            @Override
            public void onTransferError(String reason) {
                SwingUtilities.invokeLater(() -> appendMessage("system", "Transfer failed: " + reason));
            }
        });

        connectionServer = new ConnectionServer();
        try {
            connectionServer.start(port, socket -> {
                Connection c = new Connection(socket, connectionListener);
                SwingUtilities.invokeLater(() -> onInboundConnected(c));
                return c;
            });
            discoveryService.start();
        } catch (IOException ignored) {
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

        discoveryPanel = new DiscoveryPanel(this::connectPeer);
        sidePanel.add(discoveryPanel, BorderLayout.CENTER);

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
            if (activeConnection != null) {
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
                    activeConnection = new Connection(s, connectionListener);
                    activeConnection.start();
                    SwingUtilities.invokeLater(this::updateConnectionState);
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> onConnectFailed(ex));
                }
            }, "connect-thread").start();
        });
    }

    private void setupChat() {
        JPanel chatPanel;
        JPanel messageControls;

        chatBox = new JTextArea(". . .");
        chatBox.setEditable(false);
        chatBox.setLineWrap(true);
        chatBox.setWrapStyleWord(true);

        JScrollPane chatScroll = new JScrollPane(chatBox, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);

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
            String text = messageInput.getText();
            if (text.isEmpty()) return;
            if (activeConnection == null) {
                appendMessage("system", "not connected");
                return;
            }
            try {
                sendMessageAsFrames(activeConnection, text);
                appendMessage("me", text);
                messageInput.setText("");
            } catch (IOException exception) {
                appendMessage("system", "send failed: " + exception.getMessage());
            }
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

        sidePanel.add(fileTransferPanel, BorderLayout.SOUTH);

        setupFileTransferListeners();
    }

    private void setupFileTransferListeners() {
        selectButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(mainPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                selected = chooser.getSelectedFile();
                String name = selected.getName();
                long bytes = selected.length();

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
            connectionServer.stop();
            onConnectionClosed();
        } catch (IOException | InterruptedException ignored) {
        }
    }

    private void appendMessage(String user, String messageText) {
        chatBox.append(String.format("[%s]: %s%n", user, messageText));
    }

    private void sendFile() {
        if (selected == null || activeConnection == null) return;
        File f = selected;
        selected = null;
        sendFileButton.setEnabled(false);
        appendMessage("system", "sending " + f.getName() + " ...");
        Connection c = activeConnection;
        new Thread(() -> {
            try {
                sendFileAsFrames(c, f);
                SwingUtilities.invokeLater(() -> appendMessage("system", "sent " + f.getName()));
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> appendMessage("system", "send failed: " + ex.getMessage()));
            }
        }, "file-send").start();
    }

    private void updateConnectionState() {
        boolean connected = activeConnection != null && activeConnection.isOpen();
        connectButton.setText(connected ? "Disconnect" : "Connect");
        connectButton.setEnabled(true);
        messageInput.setEnabled(connected);
        sendMessageButton.setEnabled(connected);
        selectButton.setEnabled(connected);
        sendFileButton.setEnabled(connected && selected != null);
    }

    private void onInboundConnected(Connection c) {
        activeConnection = c;
        appendMessage("system", "connected");
        updateConnectionState();
    }

    private void onConnectFailed(IOException ex) {
        appendMessage("system", "connect failed: " + ex.getMessage());
        connectButton.setEnabled(true);
        connectButton.setText("Connect");
        updateConnectionState();
    }

    private void connectPeer(Peer p) {
        if (activeConnection != null) {
            appendMessage("system", "disconnect first");
            return;
        }
        new Thread(() -> {
            try {
                Socket s = new Socket(p.getAddress(), p.getTcpPort());
                Connection c = new Connection(s, connectionListener);
                c.start();
                SwingUtilities.invokeLater(() -> onInboundConnected(c));
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> onConnectFailed(ex));
            }
        }, "connect-thread").start();
    }

    private void onConnectionClosed() {
        if (receiveUtils.isReceiving()) {
            appendMessage("system", "file transfer was aborted, connection was lost");
        }
        Connection c = activeConnection;
        activeConnection = null;
        if (c != null) c.close();
        receiveUtils.reset();
        appendMessage("system", "connection closed");
        updateConnectionState();
    }
}