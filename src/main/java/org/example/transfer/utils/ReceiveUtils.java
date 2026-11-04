package org.example.transfer.utils;

import org.example.transfer.dto.frame.Frame;
import org.example.transfer.dto.frame.FrameType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ReceiveUtils {

    private static final int FILENAME_LIMIT = 1_000_000;
    // Previously was user's home directory (subdirectory recieved was created on first file transfer)
    // Path.of(System.getProperty("user.home"), "received");
    private final Path receiveDir;
    private OutputStream currentOut;
    private long expectedBytes;
    private long receivedBytes;
    private String currentFileName;
    private Path currentFilePath;
    private boolean receiving = false;
    private Path tempFilePath = null;

    public ReceiveUtils(Path sendPath){
        receiveDir = sendPath;
    }

    public static String frameToMessage(Frame f) {
        return new String(f.getPayload(), StandardCharsets.UTF_8);
    }

    public void startProcessing(Frame frame) throws IOException {
        if (frame == null || frame.getType() != FrameType.FILE_HEADER || frame.getPayload() == null) {
            reset();
            throw new IOException("Incorrect input on file transfer start");
        }
        if (receiving) {
            reset();
            throw new IOException("Received another file header during transfer");
        }
        ByteBuffer payload = ByteBuffer.wrap(frame.getPayload());
        expectedBytes = payload.getLong();
        int nameLength = payload.getInt();
        if (nameLength <= 0 || nameLength > FILENAME_LIMIT || payload.remaining() < nameLength) {
            throw new IOException("Invalid value for filename size was received");
        }
        receivedBytes = 0;
        byte[] name = new byte[nameLength];
        payload.get(name);
        currentFileName = new String(name, StandardCharsets.UTF_8);
        Path safe = Path.of(currentFileName).getFileName();
        if (safe == null || safe.toString().isEmpty()
                || safe.toString().equals(".") || safe.toString().equals("..")) {
            reset();
            throw new IOException("invalid filename");
        }
        currentFileName = safe.toString();
        if (currentFileName.isEmpty()) {
            throw new IOException("received empty file name");
        }
        currentFilePath = receiveDir.resolve(currentFileName);

        Files.createDirectories(receiveDir);
        tempFilePath = receiveDir.resolve(currentFileName + ".part");
        currentOut = Files.newOutputStream(tempFilePath);
        receiving = true;
    }

    public void processNextFrame(Frame frame) throws IOException {
        if (!receiving) {
            throw new IOException("Received file chunks before any headers");
        }
        if (frame == null || frame.getType() != FrameType.FILE_CHUNK || frame.getPayload() == null) {
            reset();
            throw new IOException("Received empty Frame");
        }
        currentOut.write(frame.getPayload());
        receivedBytes += frame.getPayload().length;
    }

    public void endProcessing(Frame frame) throws IOException {
        if (frame == null || frame.getType() != FrameType.FILE_END || !receiving) {
            reset();
            throw new IOException("Incorrect input on file transfer");
        }
        if (receivedBytes != expectedBytes) {
            reset();
            throw new IOException("Mistmatch in expected vs received bytes");
        }
        currentOut.close();
        Files.move(tempFilePath, currentFilePath, StandardCopyOption.REPLACE_EXISTING);
        tempFilePath = null;
        reset();
    }

    public void reset() {
        receiving = false;

        if (currentOut != null) {
            try {
                currentOut.close();
            } catch (IOException ignored) {
            }
            currentOut = null;
        }
        if (tempFilePath != null) {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException ignored) {
            }
            tempFilePath = null;
        }

        expectedBytes = 0;
        receivedBytes = 0;
        currentFileName = null;
        currentFilePath = null;
    }
}
