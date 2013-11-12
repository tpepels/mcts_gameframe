package breakthrough.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.List;

public class Board implements IBoard {
    @Override
    public void newDeterminization(int myPlayer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isPartialObservable() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getNMovesMade() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isLegal(IMove move) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean drawPossible() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IBoard copy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MoveList getExpandMoves() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void undoMove() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getOpponent(int player) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int checkWin() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int checkPlayoutWin() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getPlayerToMove() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getMaxUniqueMoveId() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initialize() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
