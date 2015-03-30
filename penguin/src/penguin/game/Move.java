package penguin.game;

import framework.IMove;

public class Move extends IMove {

    public static final int PLACE = 1, MOVE = 2, PASS = 3;
    private int type;
    private int[] movearr;
    private int scoreInc;

    public Move(int r, int c, int rp, int cp, int type, int scoreInc) {
        movearr = new int[4];
        movearr[0] = r;
        movearr[1] = c;
        movearr[2] = rp;
        movearr[3] = cp;

        this.type = type;
        this.scoreInc = scoreInc;
    }

    public int[] getMove() {
        return movearr;
    }


    public int getType() {
        return type;
    }

    public int getScoreInc() {
        return scoreInc;
    }

    public boolean equals(IMove move) {
        Move m = (Move) move;
        return (m.type == type
                && m.movearr[0] == movearr[0] && m.movearr[1] == movearr[1]
                && m.movearr[2] == movearr[2] && m.movearr[3] == movearr[3]);
    }


    public int getUniqueId() {
        int digit1 = movearr[0] * 8 + movearr[1];
        int digit2 = movearr[2] * 8 + movearr[3];
        return (digit1 * 64 + digit2);
    }


    public boolean isChance() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return type == MOVE;
    }

    @Override
    public String toString() {
        if (type == PASS)
            return "Pass";
        else if (type == PLACE)
            return "Place " + movearr[0] + "," + movearr[1];

        String str = "(" + movearr[0] + "," + movearr[1] + ") -> ("
                + movearr[2] + "," + movearr[3] + ")";
        return str;
    }
}

