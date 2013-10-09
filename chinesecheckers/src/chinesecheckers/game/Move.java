package chinesecheckers.game;

import ai.framework.IMove;

public class Move implements IMove {
    private final int[] move;
    private final int hops;

    public Move(int from, int to, int hops) {
        move = new int[] { from, to};
        this.hops = hops;
    }

    @Override
    public int[] getMove() {
        return move;
    }

    @Override
    public int getType() {
        return hops;
    }

    @Override
    public boolean equals(IMove move) {
        return move.getMove()[0] == this.move[0] && move.getMove()[1] == this.move[1] && hops == move.getType();
    }

    @Override
    public int getUniqueId() {
        return move[0] + move[1] * 1000 + hops * 1000000;
    }

    public String toString() {
        return "From: " + move[0] + " to: " + move[1] + " hops: " + hops;
    }
}
