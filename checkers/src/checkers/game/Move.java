package checkers.game;

import ai.framework.IMove;

/**
 * Created by tom on 08/12/13.
 */
public class Move implements IMove {
    public int[] move;
    public int hops = 0;

    public Move(int[] move) {
        this.move = move;
        this.hops = move.length / 2;
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

        if(mv.getMove().length != move.length)
            return false;

        for(int i = 0; i < move.length; i++) {
            if(mv.getMove()[i]!=move[i])
                return false;
        }

        return true;
    }

    @Override
    public int getUniqueId() {
        // TODO
        return 0;
    }
}
