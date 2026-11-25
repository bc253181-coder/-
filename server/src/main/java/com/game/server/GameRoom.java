package com.game.server;

import com.game.common.Game;
import com.game.common.GameType;
import com.game.common.Protocol;

import java.util.stream.Collectors;

/** 一局对战。服务器是权威方：落子合法性和胜负都在这里判断。 */
public class GameRoom {
    private final GameType type;
    private ClientHandler playerX;
    private ClientHandler playerO;
    private Game game;
    private boolean rematchX, rematchO;
    private boolean closed = false;

    public GameRoom(GameType type, ClientHandler a, ClientHandler b) {
        this.type = type;
        this.playerX = a;
        this.playerO = b;
    }

    public synchronized void start() {
        game = type.newGame();
        rematchX = rematchO = false;
        playerX.setRoom(this);
        playerO.setRoom(this);
        playerX.send(Protocol.START + " " + type.name() + " X " + playerO.getName());
        playerO.send(Protocol.START + " " + type.name() + " O " + playerX.getName());
        broadcastState();
    }

    public synchronized void move(ClientHandler who, int move) {
        if (closed) return;
        char mark = markOf(who);
        if (game.isOver()) {
            who.send(Protocol.ERROR + " 对局已结束");
            return;
        }
        if (game.getTurn() != mark) {
            who.send(Protocol.ERROR + " 还没轮到你");
            return;
        }
        if (!game.play(mark, move)) {
            who.send(Protocol.ERROR + " 这里不能落子");
            return;
        }
        broadcastState();
        if (game.isOver()) {
            String result = game.getWinner() == 0 ? "DRAW" : String.valueOf(game.getWinner());
            String cells = game.getWinningCells().isEmpty() ? "-"
                    : game.getWinningCells().stream().map(String::valueOf).collect(Collectors.joining(","));
            broadcast(Protocol.OVER + " " + result + " " + cells);
        }
    }

    public synchronized void chat(ClientHandler who, String text) {
        if (closed) return;
        broadcast(Protocol.CHAT + " " + who.getName() + " " + text);
    }

    /** 双方都同意后开新局，并交换先后手。 */
    public synchronized void rematch(ClientHandler who) {
        if (closed || !game.isOver()) return;
        if (who == playerX) rematchX = true; else rematchO = true;
        if (rematchX && rematchO) {
            ClientHandler tmp = playerX;
            playerX = playerO;
            playerO = tmp;
            start();
        } else {
            opponentOf(who).send(Protocol.REMATCH_OFFER);
        }
    }

    /** 玩家离开或掉线。 */
    public synchronized void leave(ClientHandler who) {
        if (closed) return;
        closed = true;
        ClientHandler other = opponentOf(who);
        who.setRoom(null);
        other.setRoom(null);
        other.send(Protocol.OPPONENT_LEFT);
        GameServer.log("房间关闭: " + who.getName() + " 离开");
    }

    private void broadcastState() {
        broadcast(Protocol.STATE + " " + game.boardString() + " " + game.getTurn() + " " + game.getLastIndex());
    }

    private void broadcast(String msg) {
        playerX.send(msg);
        playerO.send(msg);
    }

    private char markOf(ClientHandler p) { return p == playerX ? Game.X : Game.O; }
    private ClientHandler opponentOf(ClientHandler p) { return p == playerX ? playerO : playerX; }
}
