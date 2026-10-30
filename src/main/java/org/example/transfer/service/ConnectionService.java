package org.example.transfer.service;

import org.example.transfer.dto.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

// Represents connected devices
public class ConnectionService {
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread listenerThread;

    public void start(int port, Function<Socket, Connection> factory) throws IOException {

        running = true;
        serverSocket = new ServerSocket(port);

        listenerThread = new Thread(() -> acceptLoop(factory), "listener");
        listenerThread.start();
    }

    private void acceptLoop(Function<Socket, Connection> factory) {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                Connection c = factory.apply(s);
                c.start();
            } catch (IOException e) {
                if (!running)
                    return;
            }
        }
    }

    public void stop() throws IOException, InterruptedException {
        running = false;
        serverSocket.close();
        listenerThread.join();
    }

    public int boundPort() {
        return serverSocket.getLocalPort();
    }
}