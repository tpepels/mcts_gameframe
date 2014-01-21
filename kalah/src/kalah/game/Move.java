package kalah.game;

import ai.framework.IMove;

public class Move implements IMove {

    public static final int MOVE = 1, CAPTURE = 2, STORE = 3;
    
    private int type;

    // values[0] is the house
    // values[1] is the num of pieces captured by the opponent
    private int[] values; 

    public Move(int type, int house, int numSow, int numCaptured, int prevPlayer) {
        this.type = type; 

        values = new int[3]; 
        values[0] = house; 
        values[1] = numSow;  
        values[2] = numCaptured; 
        values[3] = prevPlayer;
    }

    @Override
    public int[] getMove() {
        return values;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public boolean equals(IMove move) {
        Move m = (Move) move;
        return (   m.type == type && values[0] == m.values[0] && values[1] == m.values[1] && values[2] == m.values[2]
                && values[3] == m.values[3]); 
    }

    @Override
    public int getUniqueId() {
        return (values[0]);
    }

    @Override
    public boolean isChance() {
        return false;
    }

    public String toString() {
        String str = "House " + values[0]; 
        if (type == CAPTURE) 
          return "Cap " + str + " (" + values[1] + ")"; 
        else if (type == STORE) 
          return "Sto " + str; 
        else 
          return "Mov" + str; 
    }
}
