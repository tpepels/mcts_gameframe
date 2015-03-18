package nogo.game;

import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.StatCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Board implements IBoard {
    public static int SIZE = 9, EMPTY = 0, BLACK = P1, WHITE = P2;
    private static MoveList moveList = new MoveList(500);
    private static List<IMove> po_Moves = new ArrayList<>(500);
    //
    private int[][] board;
    private int movesForPlayer = 0;
    private int nMoves = 0, currentPlayer = BLACK;
    //
    private Stack<IMove> movesMade = new Stack<>();

    static long[][] zbnums = null;
    static long blackHash, whiteHash;
    private long zbHash = 0;

    public Board() {
        this.board = new int[SIZE][SIZE];
    }

    @Override
    public void initialize() {
        nMoves = 0;
        currentPlayer = BLACK;
        movesForPlayer = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
        // initialize the zobrist numbers
        if (zbnums == null) {
            // init the zobrist numbers
            Random rng = new Random();

            // SIZE locations, 3 states for each location
            zbnums = new long[SIZE * SIZE][3];

            for (int i = 0; i < zbnums.length; i++) {
                zbnums[i][0] = rng.nextLong();
                zbnums[i][1] = rng.nextLong();
                zbnums[i][2] = rng.nextLong();
            }

            whiteHash = rng.nextLong();
            blackHash = rng.nextLong();
        }
        // now build the initial hash
        zbHash = 0;
        for (int r = 0; r < SIZE * SIZE; r++) {
            zbHash ^= zbnums[r][EMPTY];
        }
        currentPlayer = P1;
        zbHash ^= blackHash;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        int pos = (move.getMove()[1] * SIZE) + move.getMove()[0];
        zbHash ^= zbnums[pos][EMPTY];
        board[move.getMove()[1]][move.getMove()[0]] = currentPlayer;
        zbHash ^= zbnums[pos][currentPlayer];
        currentPlayer = getOpponent(currentPlayer);
        hashCurrentPlayer();
        movesMade.push(move);
        movesForPlayer = 0;
        nMoves++;
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        boolean free;
        int opp = getOpponent(currentPlayer);
        moveList.clear();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (board[y][x] == EMPTY) {
                    board[y][x] = currentPlayer;
                    seenI++;
                    // Check the freedom of the position
                    free = checkFree(x, y, currentPlayer);
                    // Check the freedom of the surrounding positions
                    if (free && x + 1 < SIZE && seen[y][x + 1] != seenI && board[y][x + 1] == opp) {
                        free = checkFree(x + 1, y, board[y][x + 1]);
                    }
                    if (free && x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == opp) {
                        free = checkFree(x - 1, y, board[y][x - 1]);
                    }
                    if (free && y + 1 < SIZE && board[y + 1][x] == opp) {
                        free = checkFree(x, y + 1, board[y + 1][x]);
                    }
                    if (free && y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == opp) {
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
        if (x + 1 < SIZE && board[y][x + 1] == EMPTY)
            return true;
        if (x - 1 >= 0 && board[y][x - 1] == EMPTY)
            return true;
        if (y + 1 < SIZE && board[y + 1][x] == EMPTY)
            return true;
        if (y - 1 >= 0 && board[y - 1][x] == EMPTY)
            return true;

        if (x + 1 < SIZE && seen[y][x + 1] != seenI && board[y][x + 1] == color) {
            if (checkFree(x + 1, y, color))
                return true;
        }
        if (x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == color) {
            if (checkFree(x - 1, y, color))
                return true;
        }
        if (y + 1 < SIZE && seen[y + 1][x] != seenI && board[y + 1][x] == color) {
            if (checkFree(x, y + 1, color))
                return true;
        }
        if (y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == color) {
            if (checkFree(x, y - 1, color))
                return true;
        }
        return false;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        // The moves were already generated for a win-check
        if (movesForPlayer == currentPlayer)
            return po_Moves;

        boolean free;
        po_Moves.clear();
        int opp = getOpponent(currentPlayer);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (board[y][x] == EMPTY) {
                    board[y][x] = currentPlayer;
                    seenI++;
                    // Check the freedom of the position
                    free = checkFree(x, y, currentPlayer);
                    // Check the freedom of the surrounding positions
                    if (free && x + 1 < SIZE && seen[y][x + 1] != seenI && board[y][x + 1] == opp) {
                        free = checkFree(x + 1, y, board[y][x + 1]);
                    }
                    if (free && x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == opp) {
                        free = checkFree(x - 1, y, board[y][x - 1]);
                    }
                    if (free && y + 1 < SIZE && board[y + 1][x] == opp) {
                        free = checkFree(x, y + 1, board[y + 1][x]);
                    }
                    if (free && y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == opp) {
                        free = checkFree(x, y - 1, board[y - 1][x]);
                    }
                    //
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
        newBoard.zbHash = zbHash;
        return newBoard;
    }

    @Override
    public void undoMove() {
        IMove move = movesMade.pop();
        if (move == null)
            throw new RuntimeException("Movesmade stack is empty.");

        currentPlayer = getOpponent(currentPlayer);
        int pos = (move.getMove()[1] * SIZE) + move.getMove()[0];
        zbHash ^= zbnums[pos][currentPlayer];
        board[move.getMove()[1]][move.getMove()[0]] = EMPTY;
        zbHash ^= zbnums[pos][EMPTY];

        hashCurrentPlayer();
        nMoves--;
        movesForPlayer = 0;
    }

    @Override
    public int checkWin() {
        movesForPlayer = 0;
        getPlayoutMoves(false);
        movesForPlayer = currentPlayer;
        if (po_Moves.isEmpty())
            return getOpponent(currentPlayer);
        else
            return NONE_WIN;
    }

    @Override
    public int checkPlayoutWin() {
        return NONE_WIN; // Not needed, if no more moves can be made, opponent of currentPlayer wins
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

    private void hashCurrentPlayer() {
        if (currentPlayer == Board.P2) {
            zbHash ^= blackHash;
            zbHash ^= whiteHash;
        } else {
            zbHash ^= whiteHash;
            zbHash ^= blackHash;
        }
    }

    private int getZbId(int r, int c) {
        int id = (r * SIZE + c) * 3;
        if (board[r][c] == P1)
            id += 1;
        else if (board[r][c] == P2)
            id += 2;
        return id;
    }

    @Override
    public long hash() {
        return zbHash;
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
