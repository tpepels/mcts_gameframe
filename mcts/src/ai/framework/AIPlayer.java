package ai.framework;

import ai.mcts.MCTSOptions;

public interface AIPlayer {
    public void newGame(int myPlayer);

    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove);

    public void stop();

    public void setOptions(MCTSOptions options);

    public IMove getBestMove();

}
