package transfer.utils.service;

import org.junit.jupiter.api.Test;
import org.example.transfer.dto.Connection;
import org.example.transfer.dto.frame.Frame;
import org.example.transfer.service.ConnectionServer;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConnectionServerTests {

    @Test
    public void testServerAcceptsConnection() throws Exception {
        ConnectionServer server = new ConnectionServer();
        CountDownLatch acceptedLatch = new CountDownLatch(1);

        server.start(0, socket -> {
            acceptedLatch.countDown();
            return new Connection(socket, new Connection.ConnectionListener() {
                @Override public void onFrame(Frame f) {}
                @Override public void onClosed() {}
            });
        });

        try (Socket client = new Socket("localhost", server.boundPort())) {
            boolean accepted = acceptedLatch.await(2, TimeUnit.SECONDS);
            assertTrue(accepted, "Server failed to accept connection via factory");
        } finally {
            server.stop();
        }
    }
}
