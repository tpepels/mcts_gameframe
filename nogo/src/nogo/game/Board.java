package nogo.game;

import ai.StatCounter;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Board implements IBoard {
    public static int SIZE = 9, EMPTY = 0, BLACK = P1, WHITE = P2;
    private static MoveList moveList = new MoveList(500);
    private static List<IMove> po_Moves = new ArrayList<>(500);
    //
    private int[][] board;
    private int nMoves = 0, currentPlayer = BLACK;
    //
    private Stack<IMove> movesMade = new Stack<>();

    public Board() {
        this.board = new int[SIZE][SIZE];
    }

    @Override
    public void initialize() {
        nMoves = 0;
        currentPlayer = BLACK;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        board[move.getMove()[1]][move.getMove()[0]] = currentPlayer;
        currentPlayer = getOpponent(currentPlayer);
        movesMade.push(move);
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        boolean free;
        moveList.clear();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (board[y][x] == EMPTY) {
                    board[y][x] = currentPlayer;
                    seenI++;
                    // Check the freedom of the position
                    free = checkFree(x, y, currentPlayer);
                    // Check the freedom of the surrounding positions
                    if (x + 1 < SIZE && board[y][x + 1] != EMPTY) {
                        seenI++;
                        free = checkFree(x + 1, y, board[y][x + 1]);
                    }
                    if (free && x - 1 >= 0 && board[y][x - 1] != EMPTY) {
                        seenI++;
                        free = checkFree(x - 1, y, board[y][x - 1]);
                    }
                    if (free && y + 1 < SIZE && board[y + 1][x] != EMPTY) {
                        seenI++;
                        free = checkFree(x, y + 1, board[y + 1][x]);
                    }
                    if (free && y - 1 >= 0 && board[y - 1][x] != EMPTY) {
                        seenI++;
                        free = checkFree(x, y - 1, board[y - 1][x]);
                    }
                    // This move will not reduce any
                    if (free) {
                        moveList.add(new Move(x, y));
                    }
                    board[y][x] = EMPTY;
                }
            }
        }
        return moveList.copy();
    }

    private long seen[][] = new long[SIZE][SIZE];
    private long seenI = Long.MIN_VALUE;

    private boolean checkFree(int x, int y, int color) {
        seen[y][x] = seenI;
        if (x + 1 < SIZE) {
            if (seen[y][x + 1] != seenI && board[y][x + 1] == color)
                return checkFree(x + 1, y, color);
            else if (board[y][x + 1] == EMPTY)
                return true;
        }
        if (x - 1 >= 0) {
            if (seen[y][x - 1] != seenI && board[y][x - 1] == color)
                return checkFree(x - 1, y, color);
            else if (board[y][x - 1] == EMPTY)
                return true;
        }
        if (y + 1 < SIZE) {
            if (seen[y + 1][x] != seenI && board[y + 1][x] == color)
                return checkFree(x, y + 1, color);
            else if (board[y + 1][x] == EMPTY)
                return true;
        }
        if (y - 1 >= 0) {
            if (seen[y - 1][x] != seenI && board[y - 1][x] == color)
                return checkFree(x, y - 1, color);
            else if (board[y - 1][x] == EMPTY)
                return true;
        }
        return false;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        boolean free;
        po_Moves.clear();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (board[y][x] == EMPTY) {
                    board[y][x] = currentPlayer;
                    seenI++;
                    // Check the freedom of the position
                    free = checkFree(x, y, currentPlayer);
                    // Check the freedom of the surrounding positions
                    if (x + 1 < SIZE && board[y][x + 1] != EMPTY) {
                        seenI++;
                        free = checkFree(x + 1, y, board[y][x + 1]);
                    }
                    if (free && x - 1 >= 0 && board[y][x - 1] != EMPTY) {
                        seenI++;
                        free = checkFree(x - 1, y, board[y][x - 1]);
                    }
                    if (free && y + 1 < SIZE && board[y + 1][x] != EMPTY) {
                        seenI++;
                        free = checkFree(x, y + 1, board[y + 1][x]);
                    }
                    if (free && y - 1 >= 0 && board[y - 1][x] != EMPTY) {
                        seenI++;
                        free = checkFree(x, y - 1, board[y - 1][x]);
                    }
                    // This move will not reduce any
                    if (free) {
                        po_Moves.add(new Move(x, y));
                    }
                    board[y][x] = EMPTY;
                }
            }
        }
        return po_Moves;
    }

    @Override
    public IBoard copy() {
        Board newBoard = new Board();
        newBoard.nMoves = nMoves;
        newBoard.currentPlayer = currentPlayer;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                newBoard.board[i][j] = board[i][j];
            }
        }
        return newBoard;
    }

    @Override
    public void undoMove() {
        IMove move = movesMade.pop();
        if (move == null)
            throw new RuntimeException("Movesmade stack is empty.");

        board[move.getMove()[1]][move.getMove()[0]] = EMPTY;
        currentPlayer = getOpponent(currentPlayer);
        nMoves--;
    }

    @Override
    public int checkWin() {
        boolean free;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (board[y][x] == EMPTY) {
                    board[y][x] = currentPlayer;
                    seenI++;
                    // Check the freedom of the position
                    free = checkFree(x, y, currentPlayer);
                    // Check the freedom of the surrounding positions
                    if (x + 1 < SIZE && board[y][x + 1] != EMPTY) {
                        seenI++;
                        free = checkFree(x + 1, y, board[y][x + 1]);
                    }
                    if (free && x - 1 >= 0 && board[y][x - 1] != EMPTY) {
                        seenI++;
                        free = checkFree(x - 1, y, board[y][x - 1]);
                    }
                    if (free && y + 1 < SIZE && board[y + 1][x] != EMPTY) {
                        seenI++;
                        free = checkFree(x, y + 1, board[y + 1][x]);
                    }
                    if (free && y - 1 >= 0 && board[y - 1][x] != EMPTY) {
                        seenI++;
                        free = checkFree(x, y - 1, board[y - 1][x]);
                    }
                    board[y][x] = EMPTY;
                    // This move will not reduce any
                    if (free) {
                        return NONE_WIN;
                    }

                }
            }
        }
        return getOpponent(currentPlayer);
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
        return SIZE * SIZE;
    }

    @Override
    public double evaluate(int player, int version) {
        return 0;
    }

    @Override
    public void initNodePriors(int parentPlayer, StatCounter stats, IMove move, int npvisits) {

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
    public int getOpponent(int player) {
        return 3 - player;
    }

    @Override
    public void newDeterminization(int myPlayer) {
        // Fully observable game
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
    public String toString() {
        String str = "";
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == EMPTY)
                    str += ".";
                if (board[r][c] == BLACK)
                    str += "b";
                if (board[r][c] == WHITE)
                    str += "w";
            }
            str += "\n";
        }
        return str;
    }
}
