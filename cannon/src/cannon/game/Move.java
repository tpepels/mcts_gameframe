package cannon.game;

import ai.framework.IMove;

public class Move implements IMove {

    public static final int MOVE = 1, CAPTURE = 2, FIRE = 3, RETREAT = 4, CASTLE = 5, C_MOVE = 6;

    private int[] move = new int[2];
    private int type;

    public Move(int type, int[] move) {
        this.move = move;
        this.type = type;
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
    public boolean equals(IMove move) {
        return move.getMove()[0] == this.move[0] && move.getMove()[1] == this.move[1]
                && type == move.getType();
    }

    @Override
    public int getUniqueId() {
        return move[0] + 100 * move[1] + 1000 * type;
    }

    public String toString() {
        String typeString = "";
        switch (type) {
            case MOVE:
                typeString = "move";
                break;
            case CAPTURE:
                typeString = "capture";
                break;
            case FIRE:
                typeString = "fire";
                break;
            case RETREAT:
                typeString = "retreat";
                break;
            case CASTLE:
                typeString = "castle";
                break;
            case C_MOVE:
                typeString = "cannon move";
                break;
        }
        return typeString + " from: " + move[0] + " to " + move[1];
    }
}
