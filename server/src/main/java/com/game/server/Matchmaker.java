package com.game.server;

import com.game.common.GameType;

import java.util.EnumMap;
import java.util.Map;

/** 匹配：每种游戏维护一个等待中的玩家，来了第二个就开局。 */
public class Matchmaker {
    private final Map<GameType, ClientHandler> waiting = new EnumMap<>(GameType.class);

    public synchronized void join(ClientHandler player, GameType type) {
        ClientHandler other = waiting.get(type);
        if (other != null && other != player && other.isConnected()) {
            waiting.remove(type);
            GameRoom room = new GameRoom(type, other, player);
            GameServer.log("开局 [" + type.name() + "] " + other.getName() + " vs " + player.getName());
            room.start();
        } else {
            waiting.put(type, player);
            player.sendWaiting();
        }
    }

    public synchronized void cancel(ClientHandler player) {
        waiting.values().removeIf(p -> p == player);
    }
}
