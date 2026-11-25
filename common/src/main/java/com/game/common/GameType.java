package com.game.common;

/** 支持的游戏类型。code 用于网络协议。 */
public enum GameType {
    TTT("井字棋", 3, 3, 3),
    C4("四子棋", 6, 7, 4);

    public final String displayName;
    public final int rows;
    public final int cols;
    /** 连成多少个算赢 */
    public final int winLength;

    GameType(String displayName, int rows, int cols, int winLength) {
        this.displayName = displayName;
        this.rows = rows;
        this.cols = cols;
        this.winLength = winLength;
    }

    public Game newGame() {
        return this == TTT ? new TicTacToe() : new ConnectFour();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
