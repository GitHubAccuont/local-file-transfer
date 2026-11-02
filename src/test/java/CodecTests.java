import org.example.transfer.dto.frame.Frame;
import org.example.transfer.dto.frame.FrameType;
import org.example.transfer.utils.FrameCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CodecTests {


    public static void main(String[] args) {
        try {

            testMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static void testMessage() throws Exception {

        String testText = "Test for message codec work";
        byte[] payload = testText.getBytes(StandardCharsets.UTF_8);
        int length = payload.length;

        Frame testFrame = new Frame(FrameType.MESSAGE, payload);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FrameCodec.writeFrame(testFrame, out);
        out.flush();
        byte[] wire = out.toByteArray();
        InputStream in = new ByteArrayInputStream(wire);
        Frame res = FrameCodec.readFrame(in);

        if (res.getType() != FrameType.MESSAGE)
            throw new AssertionError("expected MESSAGE but got " + res.getType());

        if (!Arrays.equals(res.getPayload(), payload))
            throw new AssertionError("payload mismatch: expected " + Arrays.toString(payload)
                    + " but got " + Arrays.toString(res.getPayload()));
    }

}
