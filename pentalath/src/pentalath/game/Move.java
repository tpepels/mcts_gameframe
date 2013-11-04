package pentalath.game;

import ai.framework.IMove;

public class Move implements IMove {

    private int position;

    public Move(int position) {
        this.position = position;
    }

    @Override
    public int[] getMove() {
        return new int[]{position};
    }

    @Override
    public int getType() {
        return 0;
    }

    public boolean equals(IMove move) {
        return move.getMove()[0] == position;
    }

    @Override
    public int getUniqueId() {
        return position;
    }

    @Override
    public String toString() {
        return "Position: " + Integer.toString(position);
    }
}
