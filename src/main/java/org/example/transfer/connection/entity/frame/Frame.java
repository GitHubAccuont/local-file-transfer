package org.example.transfer.connection.entity.frame;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class Frame {

    /**
     * First byte is the type of the Frame (values for it can be seen in the FrameType.java), the following
     * 4 bytes of any exchanged frame holds its size and determine how much bytes will be attempted to read
     * All frames have 5 different variants:
     * MESSAGE((byte) 0x01),
     * [UTF-8 byte encoded messages]
     * <p>
     * FILE_HEADER((byte) 0x02),
     * [8 bytes: file size][4 bytes: name length][N bytes: name]
     * <p>
     * FILE_CHUNK((byte) 0x03),
     * [chunk of file turned into bytes] Can ONLY appear after another FILE_CHUNK | FILE_HEADER
     * <p>
     * FILE_END((byte) 0x04),
     * [0 bytes, no payload, signals end of file] Only accepted if after FILE_CHUNK
     * <p>
     * UNKNOWN((byte) 0x05);
     * [however many bytes were specified in payload size] Will be ignored/logged as unsupported payload
     */
    private final FrameType type;
    private final byte[] payload;

    public Frame(FrameType type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    public FrameType getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }
}
