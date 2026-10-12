package org.example.transfer.connection;

import org.example.transfer.connection.entity.frame.Frame;
import org.example.transfer.connection.entity.frame.FrameType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DataFormatException;

import static org.example.transfer.utils.FrameCodec.readFrame;
import static org.example.transfer.utils.TransferUtils.*;

// Represents connected devices
class ConnectionService {
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread listenerThread;
    private ExecutorService executor;

    void start(int port) throws IOException {

        serverSocket = new ServerSocket(port);

        executor = Executors.newFixedThreadPool(4);
        listenerThread = new Thread(this::acceptLoop, "listener");
        listenerThread.start();
        running = true;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                executor.submit(() -> {
                    try {
                        handleIncoming(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleIncoming(Socket s) throws IOException {
        while (running) {
            try (InputStream in = new BufferedInputStream(s.getInputStream())) {
                Frame f = readFrame(in);
                switch (f.getType()) {
                    case MESSAGE:
                        saveText(f.getPayload());
                        break;
                    default:
                        throw new IOException("Unsopported format of transfer was recieved");
                }
            }
        }
    }

    void stop() throws IOException, InterruptedException {
        running = false;
        serverSocket.close();
        listenerThread.join();
        executor.shutdown();
    }
}