package alphabeta;

import ai.mcts.MCTSOptions;  // need this for now to implement AIPlayer

public class AlphaBetaOptions extends MCTSOptions {

    public int timeLimit = 1000;  // in millisections
    public boolean debugInfoAB = false;
    public boolean debugInfoMove = true;
    public boolean transpositions = false;
}
