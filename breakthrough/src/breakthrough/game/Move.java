package breakthrough.game;

import ai.framework.IMove;

public class Move implements IMove {

    public static final int MOVE = 1, CAPTURE = 2;

    private int type;
    private int[] movearr; 

    public Move(int r, int c, int rp, int cp, int type) { 
        movearr = new int[4];        
        movearr[0] = r;  movearr[1] = c;
        movearr[2] = rp; movearr[3] = cp;
        this.type = type;
    }

    @Override
    public int[] getMove() {
        return movearr;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public boolean equals(IMove move) {
        Move m = (Move)move; 
        return (   m.movearr[0] == movearr[0] && m.movearr[1] == movearr[1]
                && m.movearr[2] == movearr[2] && m.movearr[3] == movearr[3]); 
    }

    @Override
    public int getUniqueId() {
      int digit1 = movearr[0]*8 + movearr[1]; 
      int digit2 = movearr[2]*8 + movearr[3]; 
      return (digit1*64 + digit2);
    }

    public String toString() { 
        String str = "(" + movearr[0] + "," + movearr[1] + ") -> (" 
                     + movearr[2] + "," + movearr[3] + ")";
        if (type == CAPTURE) return "Cap " + str; 
        return "Mov " + str; 
    }
}
