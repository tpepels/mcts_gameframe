package checkers.game;

import ai.framework.IMove;

public class Move implements IMove {
    public static int P_SLIDE = 1, K_SLIDE = 2, P_CAP = 3, K_CAP = 4;
    //
    public final int[] move, captures;
    public final int type;
    public boolean promotion = false;
    public int kMovesBefore;

    public Move(int[] move, int[] captures, boolean king, boolean capture) {
        this.move = move;
        this.captures = captures;
        int tp = 0;
        if (king)
            tp += 2;
        else
            tp += 1;
        if (capture)
            tp += 2;
        this.type = tp;
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
        return type;
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
    public boolean isChance() {
        return false;
    }

    @Override
    public String toString() {
        return "(" + (move[1] * 8 + move[0]) + ") -> (" + (move[3] * 8 + move[2]) + ")";
    }
}
