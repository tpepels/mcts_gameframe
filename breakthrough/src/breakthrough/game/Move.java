package breakthrough.game;

import framework.IMove;

public class Move extends IMove {

    public static final int MOVE = 1, CAPTURE = 2;
    private int type;
    private int[] movearr;
    private int oldProgress1, oldProgress2;
    private int oldCapBonus1, oldCapBonus2;

    public Move(int r, int c, int rp, int cp, int type, int oldProgress1, int oldProgress2,
                int oldCapBonus1, int oldCapBonus2) {
        movearr = new int[4];
        movearr[0] = r;
        movearr[1] = c;
        movearr[2] = rp;
        movearr[3] = cp;

        this.type = type;
        this.oldProgress1 = oldProgress1;
        this.oldProgress2 = oldProgress2;
        this.oldCapBonus1 = oldCapBonus1;
        this.oldCapBonus2 = oldCapBonus2;
    }

    public int getOldProgress1() {
        return oldProgress1;
    }

    public int getOldProgress2() {
        return oldProgress2;
    }

    public int getOldCapBonus1() {
        return oldCapBonus1;
    }

    public int getOldCapBonus2() {
        return oldCapBonus2;
    }


    public int[] getMove() {
        return movearr;
    }


    public int getType() {
        return type;
    }


    public boolean equals(IMove move) {
        Move m = (Move) move;
        return (m.movearr[0] == movearr[0] && m.movearr[1] == movearr[1]
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
        return type == CAPTURE;
    }

    @Override
    public String toString() {
        String str = "";
        //str = "(" + movearr[0] + "," + movearr[1] + ") -> ("
        //        + movearr[2] + "," + movearr[3] + ")";

        str += ("" + ((char) (97 + movearr[1])));
        str += ("" + (8 - movearr[0]));
        str += ("" + ((char) (97 + movearr[3])));
        str += ("" + (8 - movearr[2]));

        if (type == CAPTURE) return "Cap " + str;
        return "Mov " + str;
    }
}
