package alphabeta;

import framework.IMove;

public class Transposition {
    public static final int REAL = 0, L_BOUND = 1, U_BOUND = 2;

    public long hash;
    public double value;
    public int depth, flag;
    public IMove bestMove;
    public int bestMoveIndex;

    Transposition() {
        reset();
    }

    public void reset() {
        hash = -1;
        bestMoveIndex = -1;
    }

    public boolean empty() {
        return hash < 0;
    }
}
