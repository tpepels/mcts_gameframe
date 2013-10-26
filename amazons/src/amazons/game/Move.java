package amazons.game;

import ai.framework.IMove;

public class Move implements IMove {
    public final int[] move;
    public int from, to, arrow;

    public Move(int from, int to, int arrow) {
        move = new int[]{from, to};
        this.arrow = arrow;
    }

    @Override
    public int[] getMove() {
        return move;
    }

    @Override
    public int getType() {
        return arrow;
    }

    @Override
    public boolean equals(IMove move) {
        return this.move[0] == move.getMove()[0] && this.move[1] == getMove()[1] && move.getType() == arrow;
    }

    @Override
    public int getUniqueId() {
        return 0;
    }
}
