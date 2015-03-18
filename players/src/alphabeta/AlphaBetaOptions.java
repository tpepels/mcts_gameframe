package alphabeta;

import ai.mcts.MCTSOptions;

public class AlphaBetaOptions extends MCTSOptions {

    public int timeLimit = 1000;  // in millisections
    public boolean debugInfoAB = false;
    public boolean debugInfoMove = true;
    public boolean transpositions = false;
    public int evVer = 0;
}
