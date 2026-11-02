package org.example.transfer.utils;

import org.example.transfer.dto.frame.Frame;
import org.example.transfer.dto.frame.FrameType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


/**
 * Utility class to write/read {@link Frame} instances in/out the data streams we get from connections.
 * Read has few checks to prevent some random bytes recieved being taken as the valid data.
 *
 */
public class FrameCodec {

    //Max size set to 1 Mb
    private static final int MAX_FRAME_SIZE = 1024 * 1024;


    public static Frame readFrame(InputStream in) throws IOException {

        byte[] type = in.readNBytes(1);
        byte[] length = in.readNBytes(4);

        if (length.length != 4 || type.length != 1) {
            throw new IOException("Input stream ended before reaching required size");
        }

        int frameLength = ByteBuffer.wrap(length).getInt();
        FrameType frameType = FrameType.fromByte(type[0]);

        if (frameLength < 0 || frameLength > MAX_FRAME_SIZE) {
            throw new IOException("Frame is too big to be processed");
        }
        byte[] payload = in.readNBytes(frameLength);

        if (payload.length != frameLength) {
            throw new IOException("Input contains shorter payload than specified one");
        }
        return new Frame(frameType, payload);

    }

    public static void writeFrame(Frame frame, OutputStream out) throws IOException {

        out.write(frame.getType().getCode());
        out.write(ByteBuffer.allocate(4).putInt(frame.getPayload().length).array());
        out.write(frame.getPayload());
        out.flush();

    }

    public static Frame convertTextToFrame(String text) {
        if (text == null || text.isEmpty()) throw new IllegalArgumentException("Incorrect value for message was given");
        return new Frame(FrameType.MESSAGE, text.getBytes(StandardCharsets.UTF_8));
    }
}
