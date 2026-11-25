package com.game.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 棋盘游戏的通用逻辑：轮流落子、判断胜负/平局。
 * 棋盘用一维数组存储，下标 = row * cols + col。
 * 格子内容：'.' 空，'X' 或 'O' 为棋子。
 */
public abstract class Game {
    public static final char EMPTY = '.';
    public static final char X = 'X';
    public static final char O = 'O';

    protected final GameType type;
    protected final char[] cells;
    private char turn = X;
    private boolean over = false;
    private char winner = 0;           // 'X' / 'O'；平局或未结束为 0
    private List<Integer> winningCells = List.of();
    private int lastIndex = -1;

    protected Game(GameType type) {
        this.type = type;
        this.cells = new char[type.rows * type.cols];
        Arrays.fill(cells, EMPTY);
    }

    /**
     * 把客户端发来的 move 转换成实际落子的格子下标。
     * 井字棋：move 就是格子下标；四子棋：move 是列号，棋子落到该列最低空位。
     * 非法时返回 -1。
     */
    protected abstract int resolveMove(int move);

    /** 尝试落子，成功返回 true。 */
    public synchronized boolean play(char player, int move) {
        if (over || player != turn) return false;
        int idx = resolveMove(move);
        if (idx < 0 || idx >= cells.length || cells[idx] != EMPTY) return false;

        cells[idx] = player;
        lastIndex = idx;

        List<Integer> line = findWinningLine(idx);
        if (line != null) {
            over = true;
            winner = player;
            winningCells = line;
        } else if (isFull()) {
            over = true;
        } else {
            turn = (turn == X) ? O : X;
        }
        return true;
    }

    /** 以最后一步为中心，检查横、竖、两条斜线。 */
    private List<Integer> findWinningLine(int idx) {
        int r = idx / type.cols, c = idx % type.cols;
        char p = cells[idx];
        int[][] dirs = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            List<Integer> line = new ArrayList<>();
            line.add(idx);
            collect(r, c, d[0], d[1], p, line);
            collect(r, c, -d[0], -d[1], p, line);
            if (line.size() >= type.winLength) return line;
        }
        return null;
    }

    private void collect(int r, int c, int dr, int dc, char p, List<Integer> out) {
        r += dr;
        c += dc;
        while (r >= 0 && r < type.rows && c >= 0 && c < type.cols && cells[r * type.cols + c] == p) {
            out.add(r * type.cols + c);
            r += dr;
            c += dc;
        }
    }

    private boolean isFull() {
        for (char ch : cells) if (ch == EMPTY) return false;
        return true;
    }

    public synchronized String boardString() { return new String(cells); }
    public synchronized char getTurn() { return turn; }
    public synchronized boolean isOver() { return over; }
    public synchronized char getWinner() { return winner; }
    public synchronized List<Integer> getWinningCells() { return winningCells; }
    public synchronized int getLastIndex() { return lastIndex; }
    public GameType getType() { return type; }
}
