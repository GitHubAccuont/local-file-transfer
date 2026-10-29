package org.example.transfer.utils;

import org.example.transfer.connection.entity.frame.Frame;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MessageUtils {

    public static Consumer<String> messageSink = null;

    public static void showMessage(Frame frame) {

        byte[] in = frame.getPayload();
        String text = new String(in, StandardCharsets.UTF_8);
        System.out.println("[DEBUG] Message was received: " + text);
        if (messageSink != null) messageSink.accept(text);
    }
}
