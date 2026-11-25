package com.game.server;

import com.game.common.GameType;
import com.game.common.Protocol;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/** 处理单个客户端连接：读取命令并分发给 Matchmaker / GameRoom。 */
public class ClientHandler implements Runnable {
    private static final int MAX_CHAT = 200;
    private static final int MAX_NAME = 16;

    private final Socket socket;
    private final Matchmaker matchmaker;
    private PrintWriter out;
    private volatile GameRoom room;
    private volatile String name = "玩家";
    private volatile boolean connected = true;

    public ClientHandler(Socket socket, Matchmaker matchmaker) {
        this.socket = socket;
        this.matchmaker = matchmaker;
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            send(Protocol.WELCOME);
            String line;
            while ((line = in.readLine()) != null) {
                handle(line.trim());
            }
        } catch (SocketTimeoutException | java.net.SocketException ignored) {
            // 客户端断开
        } catch (IOException e) {
            GameServer.log("连接异常: " + e.getMessage());
        } finally {
            connected = false;
            matchmaker.cancel(this);
            GameRoom r = room;
            if (r != null) r.leave(this);
            GameServer.log("断开: " + name);
        }
    }

    private void handle(String line) {
        if (line.isEmpty()) return;
        String[] parts = line.split(" ", 2);
        String cmd = parts[0];
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case Protocol.JOIN -> {
                String[] a = arg.split(" ", 2);
                GameType type;
                try {
                    type = GameType.valueOf(a[0]);
                } catch (IllegalArgumentException e) {
                    send(Protocol.ERROR + " 未知游戏类型");
                    return;
                }
                if (a.length > 1) name = sanitizeName(a[1]);
                GameRoom r = room;
                if (r != null) r.leave(this);   // 从旧对局离开
                matchmaker.cancel(this);
                matchmaker.join(this, type);
            }
            case Protocol.MOVE -> {
                GameRoom r = room;
                if (r == null) { send(Protocol.ERROR + " 你不在对局中"); return; }
                try {
                    r.move(this, Integer.parseInt(arg));
                } catch (NumberFormatException e) {
                    send(Protocol.ERROR + " 无效的落子");
                }
            }
            case Protocol.CHAT -> {
                GameRoom r = room;
                if (r != null && !arg.isEmpty()) {
                    String text = arg.length() > MAX_CHAT ? arg.substring(0, MAX_CHAT) : arg;
                    r.chat(this, text);
                }
            }
            case Protocol.REMATCH -> {
                GameRoom r = room;
                if (r != null) r.rematch(this);
            }
            case Protocol.LEAVE -> {
                matchmaker.cancel(this);
                GameRoom r = room;
                if (r != null) r.leave(this);
            }
            default -> send(Protocol.ERROR + " 未知命令");
        }
    }

    private static String sanitizeName(String raw) {
        String n = raw.replaceAll("\\s+", "_").replaceAll("\\p{Cntrl}", "");
        if (n.isEmpty()) n = "玩家";
        return n.length() > MAX_NAME ? n.substring(0, MAX_NAME) : n;
    }

    public synchronized void send(String msg) {
        if (out != null) out.println(msg);
    }

    public void sendWaiting() { send(Protocol.WAITING); }
    public void setRoom(GameRoom room) { this.room = room; }
    public String getName() { return name; }
    public boolean isConnected() { return connected; }
}
