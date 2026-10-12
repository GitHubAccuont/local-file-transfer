package org.example.transfer.utils;

import org.example.transfer.connection.entity.frame.Frame;
import org.example.transfer.connection.entity.frame.FrameType;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class FrameCodec {

    private static final int MAX_FRAME_SIZE = 16 * 1024 * 1024;

    public static Frame readFrame(InputStream in) throws IOException {

        byte[] type = in.readNBytes(1);
        byte[] length = in.readNBytes(4);

        if (length.length != 4 || type.length != 1) {
            throw new IOException("Input stream ended before reaching required size");
        }

        int frameLength = ByteBuffer.wrap(length).getInt();
        FrameType frameType = FrameType.fromByte(type[0]);

        if (frameLength > MAX_FRAME_SIZE) {
            throw new IOException("Frame is too big to be processed");
        }
        byte[] payload = in.readNBytes(frameLength);

        if (payload.length != frameLength) {
            throw new IOException("Input contains shorter payload than specified one");
        }
        Frame frame = new Frame(frameType, payload);
        return frame;

    }

    public static void writeFrame(Frame frame, OutputStream stream) throws IOException {

        stream.write(frame.getType().getCode());
        stream.write(ByteBuffer.allocate(4).putInt(frame.getPayload().length).array());
        stream.write(frame.getPayload());
        stream.flush();

    }
}
