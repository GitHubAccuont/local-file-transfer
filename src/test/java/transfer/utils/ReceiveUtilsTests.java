package transfer.utils;

import org.example.transfer.dto.frame.Frame;
import org.example.transfer.dto.frame.FrameType;
import org.example.transfer.utils.ReceiveUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ReceiveUtilsTests {

    ReceiveUtils testUtils;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void instantiateNewUtils() {
        this.testUtils = new ReceiveUtils(tempDir, new ReceiveUtils.ReceiveListener() {
            @Override
            public void onFileStarted(String fileName, long expectedSize) {}
            @Override
            public void onFileFinished(String fileName) {}
            @Override
            public void onTransferError(String reason) {}
        });
    }

    @Test
    public void testTextConversion() {
        String text = "Test bytes";
        Frame testFrame = new Frame(FrameType.MESSAGE, text.getBytes(StandardCharsets.UTF_8));
        assertEquals(text, ReceiveUtils.frameToMessage(testFrame));
    }

    @Test
    public void testFileReceived() throws IOException {
        byte[] fileText = "Some text inside test file".getBytes(StandardCharsets.UTF_8);
        byte[] fileName = "test.txt".getBytes(StandardCharsets.UTF_8);

        ByteBuffer headerBuffer = ByteBuffer.allocate(8 + 4 + fileName.length);
        headerBuffer.putLong(fileText.length);
        headerBuffer.putInt(fileName.length);
        headerBuffer.put(fileName);

        Frame fileHeader = new Frame(FrameType.FILE_HEADER, headerBuffer.array());
        Frame fileChunk = new Frame(FrameType.FILE_CHUNK, fileText);
        Frame fileEnd = new Frame(FrameType.FILE_END, new byte[0]);

        testUtils.processFrame(fileHeader);
        testUtils.processFrame(fileChunk);
        testUtils.processFrame(fileEnd);

        String fileNameStr = new String(fileName, StandardCharsets.UTF_8);
        Path expectedFilePath = tempDir.resolve(fileNameStr);

        assertTrue(Files.exists(expectedFilePath), "File should exist after being copied");
        assertFalse(Files.exists(tempDir.resolve(fileNameStr + ".part")), "Temp .part file should be deleted");

        byte[] actualBytes = Files.readAllBytes(expectedFilePath);
        assertArrayEquals(fileText, actualBytes, "File content should match the sent text");
    }

    @Test
    public void testIncorrectOrder() {
        Frame chunk = new Frame(FrameType.FILE_CHUNK, "random text".getBytes(StandardCharsets.UTF_8));
        Frame end = new Frame(FrameType.FILE_END, new byte[0]);

        assertThrows(IOException.class, () -> testUtils.processFrame(chunk));
        assertThrows(IOException.class, () -> testUtils.processFrame(end));
    }

    @Test
    public void testIncorrectHeader() {
        Frame badHeader = new Frame(FrameType.FILE_HEADER, new byte[]{1, 2, 3});
        assertThrows(BufferUnderflowException.class, () -> testUtils.processFrame(badHeader));
    }

    @Test
    public void testNameSizeTooBig() {
        ByteBuffer buffer = ByteBuffer.allocate(8 + 4 + 10);
        buffer.putLong(100);
        buffer.putInt(1_000_000);
        buffer.put(new byte[10]);

        Frame badHeader = new Frame(FrameType.FILE_HEADER, buffer.array());
        assertThrows(IOException.class, () -> testUtils.processFrame(badHeader));
    }

    @Test
    public void testPathInsertion() throws IOException {
        byte[] fileText = "hacked".getBytes(StandardCharsets.UTF_8);
        byte[] fileName = "../../evil.txt".getBytes(StandardCharsets.UTF_8);

        ByteBuffer headerBuffer = ByteBuffer.allocate(8 + 4 + fileName.length);
        headerBuffer.putLong(fileText.length);
        headerBuffer.putInt(fileName.length);
        headerBuffer.put(fileName);

        Frame fileHeader = new Frame(FrameType.FILE_HEADER, headerBuffer.array());
        Frame fileChunk = new Frame(FrameType.FILE_CHUNK, fileText);
        Frame fileEnd = new Frame(FrameType.FILE_END, new byte[0]);

        testUtils.processFrame(fileHeader);
        testUtils.processFrame(fileChunk);
        testUtils.processFrame(fileEnd);

        Path safePath = tempDir.resolve("evil.txt");
        assertTrue(Files.exists(safePath), "File should be sanitized and saved inside tempDir");

        Path unsafePath = tempDir.getParent().resolve("evil.txt");
        assertFalse(Files.exists(unsafePath), "File should not escape the receive directory");
    }
}