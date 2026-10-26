package org.example.transfer.utils;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class TransferUtils {

    public static Consumer<String> messageSink = null;

    public static void saveFile(byte[] in){ /*TODO*/    }

    public static void showMessage(byte[] in) {
        String text = new String(in, StandardCharsets.UTF_8);
        System.out.println("[DEBUG] Message was received: " + text);
        if (messageSink != null) messageSink.accept(text);
    }
}
