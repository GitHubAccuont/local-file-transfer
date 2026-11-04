package transfer.utils;

import org.example.transfer.dto.frame.Frame;
import org.example.transfer.dto.frame.FrameType;
import org.example.transfer.utils.FrameCodec;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class FrameCodecTests {

    @Test
    public void testMessageRoundTrip() throws IOException {
        byte[] payload = "Hello".getBytes(StandardCharsets.UTF_8);
        Frame frame = new Frame(FrameType.MESSAGE, payload);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FrameCodec.writeFrame(frame, out);

        Frame result = FrameCodec.readFrame(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(FrameType.MESSAGE, result.getType());
        assertArrayEquals(payload, result.getPayload());
    }

    @Test
    public void testNegativeLengthThrows() {
        byte[] badWire = new byte[]{FrameType.MESSAGE.getCode(), (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        assertThrows(IOException.class, () -> FrameCodec.readFrame(new ByteArrayInputStream(badWire)));
    }

    @Test
    public void testTooLargeLengthThrows() {
        byte[] badWire = ByteBuffer.allocate(5)
                .put(FrameType.MESSAGE.getCode())
                .putInt(1024 * 1024 * 20)
                .array();
        assertThrows(IOException.class, () -> FrameCodec.readFrame(new ByteArrayInputStream(badWire)));
    }

    @Test
    public void testTruncatedHeaderThrows() {
        byte[] badWire = new byte[]{FrameType.MESSAGE.getCode(), 0, 0};
        assertThrows(IOException.class, () -> FrameCodec.readFrame(new ByteArrayInputStream(badWire)));
    }

    @Test
    public void testTruncatedPayloadThrows() {
        byte[] badWire = ByteBuffer.allocate(5 + 3)
                .put(FrameType.MESSAGE.getCode())
                .putInt(10)
                .put(new byte[]{1, 2, 3})
                .array();
        assertThrows(IOException.class, () -> FrameCodec.readFrame(new ByteArrayInputStream(badWire)));
    }
}