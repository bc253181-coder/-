package com.game.common;

public class TicTacToe extends Game {
    public TicTacToe() { super(GameType.TTT); }

    @Override
    protected int resolveMove(int move) {
        return (move >= 0 && move < cells.length) ? move : -1;
    }
}
