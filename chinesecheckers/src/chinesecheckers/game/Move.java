package chinesecheckers.game;

import ai.framework.IMove;

public class Move extends IMove {
    private final int[] move;
    private final int hops;

    public Move(int from, int to, int hops) {
        move = new int[]{from, to};
        this.hops = hops;
    }


    public int[] getMove() {
        return move;
    }


    public int getType() {
        return hops;
    }


    public boolean equals(IMove move) {
        return move.getMove()[0] == this.move[0] && move.getMove()[1] == this.move[1] && hops == move.getType();
    }


    public int getUniqueId() {
        return move[0] + (move[1] * 1000);
    }


    public boolean isChance() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return hops > 1;
    }

    public String toString() {
        return "From: " + move[0] + " to: " + move[1] + " hops: " + hops;
    }
}
