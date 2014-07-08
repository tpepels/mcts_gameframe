package cannon.game;

import framework.IMove;

public class Move extends IMove {

    public static final int MOVE = 1, CAPTURE = 2, FIRE = 3, RETREAT = 4, CASTLE = 5, C_MOVE = 6;

    private int[] move = new int[2];
    private int type;

    public Move(int type, int[] move) {
        this.move = move;
        this.type = type;
    }


    public int[] getMove() {
        return move;
    }


    public int getType() {
        return type;
    }


    public boolean equals(IMove move) {
        return move.getMove()[0] == this.move[0] && move.getMove()[1] == this.move[1]
                && type == move.getType();
    }


    public int getUniqueId() {
        return move[0] + 100 * move[1] + 1000 * type;
    }


    public boolean isChance() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return type == CAPTURE || type == FIRE;
    }

    @Override
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
