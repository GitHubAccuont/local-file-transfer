package org.example.transfer.utils;

import org.example.transfer.connection.entity.frame.Frame;
import org.example.transfer.connection.entity.frame.FrameType;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FrameCodec {

    //Max size set to 1 Mb
    private static final int MAX_FRAME_SIZE = 1 * 1024 * 1024;
    // Max size set to 256 Kb
    private static final int BUFFER_SIZE = 256 * 1024;


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

    private static void writeFrame(Frame frame, OutputStream stream) throws IOException {

        stream.write(frame.getType().getCode());
        stream.write(ByteBuffer.allocate(4).putInt(frame.getPayload().length).array());
        stream.write(frame.getPayload());
        stream.flush();

    }

    public static void writeMessageAsFrame(String text, OutputStream out) throws IOException {

        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Incorrect value for message was given");
        }
        byte[] byteText = text.getBytes(StandardCharsets.UTF_8);
        Frame textFrame = new Frame(FrameType.MESSAGE, byteText);
        writeFrame(textFrame, out);
    }

    public static void writeFileAsFrame(OutputStream out, File file) throws IOException {

        if (file == null || !Files.exists(file.toPath())) {
            throw new IllegalArgumentException("File presented doesn't exist");
        }
        byte[] name = file.getName().getBytes(StandardCharsets.UTF_8);
        ByteBuffer headerBuffer = ByteBuffer.allocate(8 + 4 + name.length);
        headerBuffer.putLong(file.length());
        headerBuffer.putInt(name.length);
        headerBuffer.put(name);
        Frame fileHeader = new Frame(FrameType.FILE_HEADER, headerBuffer.array());
        writeFrame(fileHeader, out);
        try (FileInputStream fileStream = new FileInputStream(file)){
            writeFileChunkFrames(out, fileStream);
        }
        Frame fileEnd = new Frame(FrameType.FILE_END, new byte[0]);
        writeFrame(fileEnd, out);
    }

    private static void writeFileChunkFrames(OutputStream out, FileInputStream fileInputStream) throws IOException {


        byte[] buffer = new byte[BUFFER_SIZE];
        int n;
        while ((n = fileInputStream.read(buffer)) != -1) {

            byte[] chunk = new byte[n];
            System.arraycopy(buffer, 0 , chunk, 0, n);


            Frame chunkFrame = new Frame(FrameType.FILE_CHUNK, chunk);
            writeFrame(chunkFrame, out);
        }
    }
}
