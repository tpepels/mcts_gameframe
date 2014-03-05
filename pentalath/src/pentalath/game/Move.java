package pentalath.game;

import ai.framework.IMove;

public class Move extends IMove {

    private int position;

    public Move(int position) {
        this.position = position;
    }

    public int[] getMove() {
        return new int[]{position};
    }

    public int getType() {
        return 0;
    }

    public boolean equals(IMove move) {
        return move.getMove()[0] == position;
    }

    public int getUniqueId() {
        return position;
    }

    public boolean isChance() {
        return false;
    }

    public String toString() {
        return "Position: " + Integer.toString(position);
    }
}
