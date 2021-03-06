package framework.util;

import ai.MCTSOptions;
import framework.*;

public class RandomPlayer implements AIPlayer {

    private int player;
    private String game;
    private IMove theMove;

    @Override
    public void newGame(int myPlayer, String game) {
        this.player = myPlayer;
        this.game = game;
    }

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove) {
        // FIXME: does not account for invalid moves!
        MoveList list = board.getExpandMoves();
        int i = (int) (MCTSOptions.r.nextDouble() * list.size());
        theMove = list.get(i);
    }

    @Override
    public void stop() {
    }

    @Override
    public void setOptions(MCTSOptions options) {
    }

    @Override
    public IMove getBestMove() {
        return theMove;
    }
}

