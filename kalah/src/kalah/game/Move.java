package kalah.game;

import ai.framework.IMove;

public class Move implements IMove {

    public static final int MOVE = 1, CAPTURE = 2, STORE = 3;
    
    private int type;

    // values[0] is the house
    // values[1] is the num of pieces captured by the opponent
    private int[] values; 

    public Move(int type, int house, int numCaptured) {
        this.type = type; 

        values = new int[2]; 
        values[0] = house; 
        values[1] = numCaptured; 
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
        return (m.type == type && m.values[0] == m.values[0] && m.values[1] == m.values[1]); 
    }

    @Override
    public int getUniqueId() {
        // FIXME!!!!!
        int maxCap = 2*Board.N_HOUSES*Board.N_SPIECES;        
        if (true) throw new RuntimeException("Unimplemented!");
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
