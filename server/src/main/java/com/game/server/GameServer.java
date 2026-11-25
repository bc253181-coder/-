package com.game.server;

import com.game.common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务器入口。用法：java -jar game-server.jar [端口]
 * 每个客户端连接由一个线程处理（ClientHandler）。
 */
public class GameServer {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        Matchmaker matchmaker = new Matchmaker();
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(port)) {
            log("服务器已启动，监听端口 " + port);
            while (true) {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                log("新连接: " + socket.getRemoteSocketAddress());
                pool.execute(new ClientHandler(socket, matchmaker));
            }
        }
    }

    static void log(String msg) {
        System.out.println("[" + LocalTime.now().format(TIME) + "] " + msg);
    }
}
