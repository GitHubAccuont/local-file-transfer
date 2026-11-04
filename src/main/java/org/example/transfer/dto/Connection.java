package org.example.transfer.dto;

import org.example.transfer.dto.frame.Frame;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import static org.example.transfer.utils.FrameCodec.readFrame;
import static org.example.transfer.utils.FrameCodec.writeFrame;

/**
 * Class representing any live connection going (out/in) with other same app instance.
 * There is no auto reconnect and connection is dropped in case some unexpected behavior happens during transfer.
 * {@code sendLock}
 *
 */
public class Connection {

    private final Socket socket;
    private final Object sendLock = new Object();
    private final ConnectionListener listener;

    private volatile boolean open = true;
    private volatile boolean closedNotified = false;

    private Thread readerThread;


    public Connection(Socket socket, ConnectionListener listener) {
        this.socket = socket;
        this.listener = listener;
    }

    public void start() {
        readerThread = new Thread(this::readLoop, "conn-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try (InputStream in = socket.getInputStream()) {
            while (open) {
                Frame f = readFrame(in);
                listener.onFrame(f);
            }
        } catch (IOException e) {
            close();
        } finally {
            if (!closedNotified) {
                closedNotified = true;
                listener.onClosed();
            }
        }

    }

    public void send(Frame f) throws IOException {
        if (!open) {
            throw new IOException("Connection closed");
        }
        synchronized (sendLock) {
            writeFrame(f, socket.getOutputStream());
        }
    }

    public void close() {
        open = false;
        try {
            socket.close();
            if (readerThread != null && readerThread != Thread.currentThread()) {
                readerThread.join(1000);
            }
        } catch (IOException | InterruptedException ignored) {
        }

    }

    public boolean isOpen() {
        return open;
    }

    public interface ConnectionListener {
        void onFrame(Frame f) throws IOException;

        void onClosed();
    }
}
