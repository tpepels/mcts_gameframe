package domineering.game;

import framework.FiniteBoard;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.StatCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Board implements FiniteBoard {
    private static final int EMPTY = 0;
    public static boolean CRAM = false;
    private static final ArrayList<IMove> poMoves = new ArrayList<IMove>(1000);
    private static final MoveList static_moves = new MoveList(1000);
    //
    private final int[][] board;
    private final int size;
    private int nMoves, currentPlayer, freeSquares;
    private Stack<IMove> pastMoves = new Stack<IMove>();
    //
    private final long[][] zobristPositions;
    private long zobristHash, whiteHash, blackHash;

    public Board(int size) {
        // First, generate a random hashing key for all positions
        zobristPositions = new long[size * size][];
        Random r = new Random();
        for (int i = 0; i < size * size; i++) {
            // Generate a random number for each possible occupation
            zobristPositions[i] = new long[2];
            zobristPositions[i][P1 - 1] = r.nextLong();
            zobristPositions[i][P2 - 1] = r.nextLong();
        }
        whiteHash = r.nextLong();
        blackHash = r.nextLong();
        zobristHash ^= whiteHash;
        this.size = size;
        this.board = new int[size][size];
        this.freeSquares = size * size;
        this.nMoves = 0;
    }

    @Override
    public IBoard copy() {
        Board newBoard = new Board(size);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                //
                newBoard.board[i][j] = board[i][j];

                // Generate a random number for each possible occupation
                newBoard.zobristPositions[i * size + j] = new long[2];
                newBoard.zobristPositions[i * size + j][P1 - 1] = zobristPositions[i * size + j][P1 - 1];
                newBoard.zobristPositions[i * size + j][P2 - 1] = zobristPositions[i * size + j][P2 - 1];
            }
        }
        newBoard.zobristHash = zobristHash;
        newBoard.whiteHash = whiteHash;
        newBoard.blackHash = blackHash;
        newBoard.nMoves = nMoves;
        newBoard.currentPlayer = currentPlayer;
        newBoard.freeSquares = freeSquares;
        return newBoard;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        int x1 = move.getMove()[0], x2 = move.getMove()[2];
        int y1 = move.getMove()[1], y2 = move.getMove()[3];
        board[y1][x1] = currentPlayer;
        board[y2][x2] = currentPlayer;
        zobristHash ^= zobristPositions[y1 * size + x1][currentPlayer - 1];
        zobristHash ^= zobristPositions[y2 * size + x2][currentPlayer - 1];
        freeSquares -= 2;
        nMoves++;
        currentPlayer = getOpponent(currentPlayer);
        hashCurrentPlayer();
        pastMoves.push(move);
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        static_moves.clear();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == EMPTY) {
                    // if cram, check in all directions
                    if (CRAM || currentPlayer == P1)
                        if (i + 1 < size && board[i + 1][j] == EMPTY) static_moves.add(new Move(j, i, j, i + 1));
                    if (CRAM || currentPlayer == P2)
                        if (j + 1 < size && board[i][j + 1] == EMPTY) static_moves.add(new Move(j, i, j + 1, i));
                }
            }
        }
        return static_moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        poMoves.clear();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == EMPTY) {
                    // if cram, check in all directions
                    if (CRAM || currentPlayer == P1)
                        if (i + 1 < size && board[i + 1][j] == EMPTY) poMoves.add(new Move(j, i, j, i + 1));
                    if (CRAM || currentPlayer == P2)
                        if (j + 1 < size && board[i][j + 1] == EMPTY) poMoves.add(new Move(j, i, j + 1, i));
                }
            }
        }
        return poMoves;
    }

    @Override
    public void undoMove() {
        IMove move = pastMoves.pop();
        currentPlayer = getOpponent(currentPlayer);
        nMoves--;
        freeSquares -= 2;
        hashCurrentPlayer();
        //
        int x1 = move.getMove()[0], x2 = move.getMove()[2];
        int y1 = move.getMove()[1], y2 = move.getMove()[3];
        int color = board[y1][x1];
        // return the stone to the hash
        zobristHash ^= zobristPositions[y1 * size + x1][color - 1];
        zobristHash ^= zobristPositions[y2 * size + x2][color - 1];
        //
        board[y1][x1] = EMPTY;
        board[y2][x2] = EMPTY;
    }

    @Override
    public int getOpponent(int player) {
        return 3 - player;
    }

    @Override
    public int checkWin() {
        boolean canMove = false;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == EMPTY) {
                    // if cram, check in all directions
                    if (CRAM || currentPlayer == P1)
                        if (i + 1 < size && board[i + 1][j] == EMPTY) canMove = true;
                    if (CRAM || currentPlayer == P2)
                        if (j + 1 < size && board[i][j + 1] == EMPTY) canMove = true;
                }
                if (canMove) break;
            }
            if (canMove) break;
        }
        // If the current player can move, nothing can be said about the winner
        if (!canMove)
            return getOpponent(currentPlayer);
        else
            return NONE_WIN;
    }

    @Override
    public int checkPlayoutWin() {
        return checkWin();
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public int getMaxUniqueMoveId() {
        return (size + 1) * 1000000;
    }

    @Override
    public void initialize() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = EMPTY;
            }
        }
        freeSquares = size * size;
        nMoves = 0;
        currentPlayer = P1;
    }

    @Override
    public double evaluate(int player, int version) {
        return 0;
    }

    @Override
    public void initNodePriors(int parentPlayer, StatCounter stats, IMove move, int npvisits) {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                switch (board[i][j]) {
                    case EMPTY:
                        sb.append(".");
                        break;
                    case P1:
                        sb.append("W");
                        break;
                    case P2:
                        sb.append("B");
                        break;
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public double getQuality() {
        return 0;
    }

    @Override
    public MoveList getOrderedMoves() {
        return null;
    }

    @Override
    public long hash() {
        return 0;
    }

    @Override
    public void newDeterminization(int myPlayer) {
        // Not required
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
    public boolean noMovesIsDraw() {
        return false;
    }

    @Override
    public int getHorizon() {
        // After this many turns, the board will be full
        return ((size * size) / 2) - (nMoves) + 1;
    }

    @Override
    public long getStateHash() {
        return zobristHash;
    }

    private void hashCurrentPlayer() {
        if (currentPlayer == Board.P1) {
            zobristHash ^= blackHash;
            zobristHash ^= whiteHash;
        } else {
            zobristHash ^= whiteHash;
            zobristHash ^= blackHash;
        }
    }
}
