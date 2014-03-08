package domineering.game;

import ai.framework.IMove;

public class Move extends IMove {
    private int[] move = new int[4];

    public Move(int x1, int y1, int x2, int y2) {
        this.move[0] = x1;
        this.move[1] = y1;
        this.move[2] = x2;
        this.move[3] = y2;
    }


    public int[] getMove() {
        return move;
    }


    public int getType() {
        return 0;
    }


    public boolean equals(IMove move) {
        return this.move[0] == move.getMove()[0] && this.move[1] == move.getMove()[1]
                && this.move[2] == move.getMove()[2] && this.move[3] == move.getMove()[3];
    }


    public int getUniqueId() {
        return move[0] + (move[1] * 100) + (move[2] * 10000) + (move[3] * 1000000);
    }


    public boolean isChance() {
        return false;
    }

    @Override
    public String toString() {
        return "(" + move[0] + "," + move[1] + ") -> (" + move[2] + "," + move[3] + ")";
    }
}
