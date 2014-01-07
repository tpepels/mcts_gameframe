package amazons.game;

import ai.framework.IMove;

public class Move implements IMove {
    public final int[] move;
    public int arrow;

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
        return this.move[0] == move.getMove()[0] && this.move[1] == move.getMove()[1] && move.getType() == arrow;
    }

    @Override
    public int getUniqueId() {
        return move[0] + 100 * move[1] + 10000 * arrow;
    }

    @Override
    public boolean isChance() {
        return false;
    }

    @Override
    public String toString() {
        return "from: " + move[0] + " to: " + move[1] + " shot: " + arrow;
    }
}
