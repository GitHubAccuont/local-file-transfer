package org.example.transfer.utils;

import org.example.transfer.dto.Connection;
import org.example.transfer.dto.entity.frame.Frame;
import org.example.transfer.dto.entity.frame.FrameType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.example.transfer.utils.FrameCodec.convertTextToFrame;

public class SendUtils {

    // Max size set to 256 Kb
    private static final int BUFFER_SIZE = 256 * 1024;

    public static void sendFileAsFrames(Connection conn, File file) throws IOException {

        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("File presented doesn't exist");
        }
        byte[] name = file.getName().getBytes(StandardCharsets.UTF_8);
        ByteBuffer headerBuffer = ByteBuffer.allocate(8 + 4 + name.length);
        headerBuffer.putLong(file.length());
        headerBuffer.putInt(name.length);
        headerBuffer.put(name);
        Frame fileHeader = new Frame(FrameType.FILE_HEADER, headerBuffer.array());
        conn.send(fileHeader);
        try (FileInputStream fileStream = new FileInputStream(file)) {
            sendFileChunk(conn::send, fileStream);
        }
        Frame fileEnd = new Frame(FrameType.FILE_END, new byte[0]);
        conn.send(fileEnd);
    }

    public static void sendMessageAsFrames(Connection conn, String text) throws IOException {
        conn.send(convertTextToFrame(text));
    }

    private static void sendFileChunk(FrameSender sender, FileInputStream fileInputStream) throws IOException {

        byte[] buffer = new byte[BUFFER_SIZE];
        int n;
        while ((n = fileInputStream.read(buffer)) != -1) {

            byte[] chunk = new byte[n];
            System.arraycopy(buffer, 0, chunk, 0, n);


            Frame chunkFrame = new Frame(FrameType.FILE_CHUNK, chunk);
            sender.send(chunkFrame);
        }
    }

    @FunctionalInterface
    interface FrameSender {
        void send(Frame f) throws IOException;
    }
}
