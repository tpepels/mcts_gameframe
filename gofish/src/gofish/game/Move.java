package gofish.game;

import framework.IMove;

/**
 * Created by tom on 21/03/15.
 */
public class Move extends IMove {

    public final int[] move;

    public Move(int request) {
        move = new int[]{request};
    }

    @Override
    public int[] getMove() {
        return move;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public boolean equals(IMove move) {
        return move.getMove()[0] == this.move[0];
    }

    @Override
    public int getUniqueId() {
        return move[0];
    }

    @Override
    public boolean isChance() {
        return false;
    }

    @Override
    public boolean isInteresting() {
        return false;
    }

    @Override
    public String toString() {
        switch (move[0] % 100) {
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            case 1:
                return "A";
            default:
                return Integer.toString(move[0] % 100);
        }
    }
}
