package org.example.transfer.connection;

import org.example.transfer.connection.entity.frame.Frame;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.example.transfer.utils.FrameCodec.readFrame;

// Represents connected devices
public class ConnectionService {
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread listenerThread;
    private ExecutorService executor;
    private Socket peerSocket;

    public void start(int port, Consumer<Frame> handler) throws IOException {

        running = true;
        serverSocket = new ServerSocket(port);

        executor = Executors.newFixedThreadPool(4);
        listenerThread = new Thread(() -> acceptLoop(handler), "listener");
        listenerThread.start();
    }

    private void acceptLoop(Consumer<Frame> handler) {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                executor.submit(() -> {
                    try {
                        try (InputStream in = new BufferedInputStream(s.getInputStream())) {
                            while (running) {
                                Frame f = readFrame(in);
                                handler.accept(f);
                            }
                        }
                    } catch (IOException e) {
                        if(!running) return;
                    }
                });
            } catch (IOException e) {
                if(!running) return;
            }
        }
    }

    public void stop() throws IOException, InterruptedException {
        running = false;
        serverSocket.close();
        listenerThread.join();
        executor.shutdown();
    }

    public int boundPort() {
        return serverSocket.getLocalPort();
    }
}