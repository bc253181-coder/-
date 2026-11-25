package com.game.common;

public class ConnectFour extends Game {
    public ConnectFour() { super(GameType.C4); }

    /** move 为列号，返回该列最下方空格的下标。 */
    @Override
    protected int resolveMove(int col) {
        if (col < 0 || col >= type.cols) return -1;
        for (int r = type.rows - 1; r >= 0; r--) {
            int idx = r * type.cols + col;
            if (cells[idx] == EMPTY) return idx;
        }
        return -1; // 该列已满
    }
}
