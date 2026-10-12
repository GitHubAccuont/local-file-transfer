package org.example.transfer.connection.entity.frame;

import java.util.Arrays;

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

    public byte getCode(){
        return code;
    }
}
