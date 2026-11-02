package org.example.transfer.dto.frame;

import java.util.Arrays;

/**
 * The enum storing possible values for frame types (in byte form).
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
public enum FrameType {
    MESSAGE((byte) 0x01),
    FILE_HEADER((byte) 0x02),
    FILE_CHUNK((byte) 0x03),
    FILE_END((byte) 0x04),
    UNKNOWN((byte) 0x05);


    private final byte code;

    FrameType(byte code) {
        this.code = code;
    }

    public static FrameType fromByte(byte code) {

        return Arrays.stream(FrameType.values()).filter(e -> e.code == code).findFirst().orElse(UNKNOWN);
    }

    public byte getCode() {
        return code;
    }
}
