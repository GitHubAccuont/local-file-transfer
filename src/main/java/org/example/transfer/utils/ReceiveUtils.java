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
    private final Path receiveDir;
    private final ReceiveListener listener;

    private OutputStream currentOut;
    private long expectedBytes;
    private long receivedBytes;
    private String currentFileName;
    private Path currentFilePath;
    private boolean receiving = false;
    private Path tempFilePath = null;

    public ReceiveUtils(Path sendPath, ReceiveListener listener) {
        this.receiveDir = sendPath;
        this.listener = listener;
    }

    public static String frameToMessage(Frame f) {
        return new String(f.getPayload(), StandardCharsets.UTF_8);
    }

    public void processFrame(Frame frame) throws IOException {
        try {
            switch (frame.getType()) {
                case FILE_HEADER:
                    startProcessing(frame);
                    break;
                case FILE_CHUNK:
                    processNextFrame(frame);
                    break;
                case FILE_END:
                    endProcessing(frame);
                    break;
                default:
                    throw new IOException("ReceiveUtils received an unsupported frame type: " + frame.getType());
            }
        } catch (IOException e) {
            reset();
            listener.onTransferError(e.getMessage());
            throw e;
        }
    }

    private void startProcessing(Frame frame) throws IOException {
        if (frame == null || frame.getType() != FrameType.FILE_HEADER || frame.getPayload() == null) {
            throw new IOException("Incorrect input on file transfer start");
        }
        if (receiving) {
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

        listener.onFileStarted(currentFileName, expectedBytes);
    }

    private void processNextFrame(Frame frame) throws IOException {
        if (!receiving) {
            throw new IOException("Received file chunks before any headers");
        }
        if (frame == null || frame.getType() != FrameType.FILE_CHUNK || frame.getPayload() == null) {
            throw new IOException("Received empty Frame");
        }
        currentOut.write(frame.getPayload());
        receivedBytes += frame.getPayload().length;
    }

    private void endProcessing(Frame frame) throws IOException {
        if (frame == null || frame.getType() != FrameType.FILE_END || !receiving) {
            throw new IOException("Incorrect input on file transfer");
        }
        if (receivedBytes != expectedBytes) {
            throw new IOException("Mismatch in expected vs received bytes");
        }
        currentOut.close();
        Files.move(tempFilePath, currentFilePath, StandardCopyOption.REPLACE_EXISTING);
        tempFilePath = null;
        receiving = false;
        reset();

        listener.onFileFinished(currentFileName);
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

    public boolean isReceiving() {
        return receiving;
    }

    public interface ReceiveListener {
        void onFileStarted(String fileName, long expectedSize);

        void onFileFinished(String fileName);

        void onTransferError(String reason);
    }
}