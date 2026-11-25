package com.game.client;

import javafx.application.Platform;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 网络连接。读取在后台线程进行，收到的每条消息都通过 Platform.runLater
 * 交给 JavaFX 线程处理（JavaFX 只允许在 FX 线程里修改界面）。
 */
public class Connection {
    private final Socket socket = new Socket();
    private PrintWriter out;
    private volatile boolean closing = false;

    public void connect(String host, int port, Consumer<String> onMessage, Runnable onDisconnect) throws IOException {
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setTcpNoDelay(true);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> onMessage.accept(msg));
                }
            } catch (IOException ignored) {
                // 连接断开
            } finally {
                if (!closing) Platform.runLater(onDisconnect);
            }
        }, "net-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public void send(String msg) {
        if (out != null) out.println(msg);
    }

    public void close() {
        closing = true;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
