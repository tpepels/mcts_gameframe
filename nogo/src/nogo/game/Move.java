package nogo.game;

import ai.framework.IMove;

public class Move extends IMove {

    private int[] move;

    public Move(int x, int y) {
        this.move = new int[]{x, y};
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
        return this.move[0] == move.getMove()[0] && this.move[1] == move.getMove()[1];
    }

    @Override
    public int getUniqueId() {
        return move[0] + (move[1] * Board.SIZE);
    }

    @Override
    public boolean isChance() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public String toString() {
        return "(" + move[1] + ", " + move[0] + ")";
    }
}
