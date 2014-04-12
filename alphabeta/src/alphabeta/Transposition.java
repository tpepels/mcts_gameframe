package alphabeta;

import ai.framework.IMove;

public class Transposition {
    public static final int REAL = 0, L_BOUND = 1, U_BOUND = 2;

    public long hash;
    public double value;
    public int depth, flag; 
    public IMove bestMove;
    public int bestMoveIndex;
}
