package org.example.transfer.service;

import org.example.transfer.dto.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

/**
 * The connection service implementation. It's responsible for running the server socket.
 * Any live connection made whether its inbound (to our server) or outbound(made by us) is stored within and represented by lifecycle of
 * {@link Connection} class. Input for {@code start()} function takes the port along the factory function
 * which is not really needed for it to work, but is one of better ways to give us callbacks to reflect new connections in UI.
 * At the time limit of connection is 1 whether its inbound or outbound but that can be changed with some UI changes and adding connection storage.
 *
 */
public class ConnectionServer {
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