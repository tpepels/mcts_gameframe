package checkers.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.List;

public class Board implements IBoard {
    public final static int W_PIECE = 1, B_PIECE = 2, W_QUEEN = W_PIECE * 10, B_QUEEN = B_PIECE * 10;
    public final static MoveList moves = new MoveList(1000);
    //
    public int[][] board = new int[8][8];
    private int nMoves = 0, currentPlayer = P1,  nPieces1 = 12, nPieces2 = 12;

    @Override
    public boolean doAIMove(IMove move, int player) {
        return false;
    }

    @Override
    public MoveList getExpandMoves() {
        moves.clear();
        int piece;
        for(int i = 0; i < board.length; i++) {
            for(int j = 0; j < board.length; j++) {
                piece = board[i][j];
                if(piece == currentPlayer || piece / 10 == currentPlayer) {

                }
            }
        }
        return moves;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        return null;
    }

    @Override
    public void undoMove() {

    }

    @Override
    public int getOpponent(int player) {
        return 0;
    }

    @Override
    public int checkWin() {
        return 0;
    }

    @Override
    public int checkPlayoutWin() {
        return 0;
    }

    @Override
    public int getPlayerToMove() {
        return 0;
    }

    @Override
    public int getMaxUniqueMoveId() {
        return 0;
    }

    @Override
    public void initialize() {

    }

    @Override
    public double evaluate(int player) {
        return 0;
    }

    @Override
    public double getQuality() {
        return 0;
    }

    @Override
    public void newDeterminization(int myPlayer) {
        // Fully observable
    }

    @Override
    public boolean isPartialObservable() {
        return false;
    }

    @Override
    public int getNMovesMade() {
        return nMoves;
    }

    @Override
    public boolean isLegal(IMove move) {
        return true;
    }

    @Override
    public boolean drawPossible() {
        return false;
    }

    @Override
    public IBoard copy() {
        return null;
    }
}
