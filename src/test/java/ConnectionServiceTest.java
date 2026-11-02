import org.example.transfer.dto.frame.Frame;
import org.example.transfer.dto.frame.FrameType;
import org.example.transfer.service.ConnectionService;
import org.example.transfer.utils.MessageUtils;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.example.transfer.utils.FrameCodec.writeFrame;

public class ConnectionServiceTest {

    public static void main(String[] args) throws Exception {
        testSingleMessage();
        testTwoBackToBackMessages();
        testTruncatedFrameClosesCleanly();
        System.out.println("ALL TESTS PASSED");
    }

    // ---- helpers ----

    /**
     * Captures messages into a list + latch; returns them so tests can assert.
     */
    private static List<String> captureMessages(int expectedCount) {
        List<String> received = new ArrayList<>();
        MessageUtils.messageSink = received::add;
        return received;
    }

    // ---- tests ----

    private static void testSingleMessage() throws Exception {
        List<String> received = captureMessages(1);

        ConnectionService service = new ConnectionService();
        service.start(0);                       // 0 = OS picks a free port
        int port = service.boundPort();
        try (Socket s = new Socket("localhost", port)) {
            writeFrame(new Frame(FrameType.MESSAGE,
                    "hello".getBytes(StandardCharsets.UTF_8)), s.getOutputStream());
            waitFor(received, 1);
        } finally {
            service.stop();
            MessageUtils.messageSink = null;
        }
        assertEquals(List.of("hello"), received, "testSingleMessage");
        System.out.println("testSingleMessage PASSED");
    }

    private static void testTwoBackToBackMessages() throws Exception {
        List<String> received = captureMessages(2);

        ConnectionService service = new ConnectionService();
        service.start(0);

        int port = service.boundPort();
        try (Socket s = new Socket("localhost", port)) {
            // both frames in one write burst — this is what triggers the buffer bug
            writeFrame(new Frame(FrameType.MESSAGE,
                    "first".getBytes(StandardCharsets.UTF_8)), s.getOutputStream());
            writeFrame(new Frame(FrameType.MESSAGE,
                    "second".getBytes(StandardCharsets.UTF_8)), s.getOutputStream());
            waitFor(received, 2);
        } finally {
            service.stop();
            MessageUtils.messageSink = null;
        }
        assertEquals(List.of("first", "second"), received, "testTwoBackToBackMessages");
        System.out.println("testTwoBackToBackMessages PASSED");
    }

    private static void testTruncatedFrameClosesCleanly() throws Exception {
        List<String> received = captureMessages(1);

        ConnectionService service = new ConnectionService();
        service.start(0);
        int port = service.boundPort();
        try {
            // send half a frame, then close
            try (Socket s = new Socket("localhost", port)) {
                s.getOutputStream().write(FrameType.MESSAGE.getCode());
                s.getOutputStream().write(new byte[]{0, 0, 0, 10});
                s.getOutputStream().write(new byte[]{'a', 'b', 'c'});
                s.getOutputStream().flush();
            }
            Thread.sleep(100); // give server a moment to see EOF and return

            // service must still accept a fresh connection
            try (Socket s2 = new Socket("localhost", port)) {
                writeFrame(new Frame(FrameType.MESSAGE,
                        "after-break".getBytes(StandardCharsets.UTF_8)), s2.getOutputStream());
                waitFor(received, 1);
            }
        } finally {
            service.stop();
            MessageUtils.messageSink = null;
        }
        assertEquals(List.of("after-break"), received, "testTruncatedFrameClosesCleanly");
        System.out.println("testTruncatedFrameClosesCleanly PASSED");
    }

    // ---- assertion helpers ----

    private static void waitFor(List<String> list, int n) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (list.size() < n && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }

    private static void assertEquals(List<String> expected, List<String> actual, String test) {
        if (!expected.equals(actual)) {
            throw new AssertionError(test + " FAILED\n  expected: " + expected
                    + "\n  actual:   " + actual);
        }
    }
}