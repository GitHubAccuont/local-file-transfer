package transfer.utils.dto;

import org.junit.jupiter.api.Test;
import org.example.transfer.dto.Connection;
import org.example.transfer.dto.frame.Frame;
import org.example.transfer.dto.frame.FrameType;
import org.example.transfer.dto.Connection.ConnectionListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionTests {

    @Test
    public void testSendAndReceiveBetweenTwoConnections() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        BlockingQueue<Frame> receivedFrames = new LinkedBlockingQueue<>();

        ConnectionListener serverListener = new ConnectionListener() {
            @Override public void onFrame(Frame f) { receivedFrames.offer(f); }
            @Override public void onClosed() {}
        };

        Thread acceptThread = new Thread(() -> {
            try (Socket s = serverSocket.accept()) {
                Connection serverConn = new Connection(s, serverListener);
                serverConn.start();
                Thread.sleep(1000);
            } catch (Exception ignored) {}
        });
        acceptThread.start();

        Socket clientSocket = new Socket("localhost", port);
        Connection clientConn = new Connection(clientSocket, new ConnectionListener() {
            @Override public void onFrame(Frame f) {}
            @Override public void onClosed() {}
        });
        clientConn.start();

        byte[] payload = "Ping".getBytes(StandardCharsets.UTF_8);
        clientConn.send(new Frame(FrameType.MESSAGE, payload));

        Frame received = receivedFrames.poll(2, TimeUnit.SECONDS);
        assertNotNull(received, "Server did not receive the frame in time");
        assertEquals(FrameType.MESSAGE, received.getType());
        assertArrayEquals(payload, received.getPayload());

        clientConn.close();
        serverSocket.close();
    }

    @Test
    public void testOnClosedFiresWhenPeerDisconnects() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        CountDownLatch closedLatch = new CountDownLatch(1);

        Thread acceptThread = new Thread(() -> {
            try (Socket s = serverSocket.accept()) {
                Connection serverConn = new Connection(s, new ConnectionListener() {
                    @Override public void onFrame(Frame f) {}
                    @Override public void onClosed() { closedLatch.countDown(); }
                });
                serverConn.start();
            } catch (Exception ignored) {}
        });
        acceptThread.start();

        Socket clientSocket = new Socket("localhost", port);
        Connection clientConn = new Connection(clientSocket, new ConnectionListener() {
            @Override public void onFrame(Frame f) {}
            @Override public void onClosed() {}
        });
        clientConn.start();

        clientConn.close();

        boolean closed = closedLatch.await(2, TimeUnit.SECONDS);
        assertTrue(closed, "onClosed callback did not fire after peer disconnected");

        serverSocket.close();
    }
}