package checkers.game;

import ai.framework.IMove;

public class Move implements IMove {
    public int[] move, captures;
    public int hops = 0;
    public boolean promotion = false;

    public Move(int[] move, int[] captures) {
        this.move = move;
        this.hops = move.length / 2;
        this.captures = captures;
    }

    public int[] getCaptures() {
        return captures;
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
    public boolean equals(IMove mv) {

        if (mv.getMove().length != move.length)
            return false;

        for (int i = 0; i < move.length; i++) {
            if (mv.getMove()[i] != move[i])
                return false;
        }

        return true;
    }

    @Override
    public int getUniqueId() {
        return ((move[1] * 8) + move[0]) + 100 * ((move[3] * 8) + move[2]);
    }

    @Override
    public String toString() {
        return "(" + (move[1] * 8 + move[0]) + ") -> (" + (move[3] * 8 + move[2]) + ")";
    }
}
