package org.example.transfer.dto.frame;

public class Frame {

    /**
     * Representation of frames being sent to/from app. The data sent using {@link org.example.transfer.utils.FrameCodec}
     * and the actual class doesnt hold any methods representing its transfer. All the parsing/reading made with codec so this class just
     * holds some data about sent Frame instance.
     * First byte is the type of the Frame, the following
     * next 4 bytes of any exchanged frame holds its size and determine how much bytes will be attempted to read.
     * The rest is the payload which is actual data attempted to be sent.
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
