package chinesecheckers.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import chinesecheckers.gui.CCGui;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Board implements IBoard {

    public static final int EMPTY = 0, WHITE = P1, BLACK = P2;
    public static final int N_PIECES = 10, WIDTH = 13, HEIGHT = 17, SIZE = 221;
    public static final int[] occupancy = new int[]
            {
                    0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                    0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
            };
    private static final int B_HOME_MIN = 169, W_HOME_MAX = 52;
    private static final int[] N_VECTOR_ODD = {-13, -12, +1, +14, +13, -1}, N_VECTOR_EVEN = {-14, -13, +1, +13, +12, -1};
    //
    public final Field[] board;
    public MoveList moves = new MoveList(1000);                // Moves per player
    private int winner = NONE_WIN, currentPlayer = P1;
    //
    private Stack<IMove> pastMoves = new Stack<IMove>();        // Stack for undo-move
    private boolean[] seen;                                     // List to keep track of positions seen for jumping

    public Board() {
        board = new Field[SIZE];
        // Initialize the empty fields
        for (int i = 0; i < SIZE; i++) {
            // Only use fields that are part of the board
            if (occupancy[i] == 0)
                continue;
            //
            board[i] = new Field(i);
        }
        int[] nVector;
        // Set the neighbours of each field.
        for (int i = 0; i < SIZE; i++) {
            // First, check if the field is part of the board
            if (occupancy[i] == 0)
                continue;
            // Even or odd row
            if ((i / WIDTH) % 2 == 0)
                nVector = N_VECTOR_EVEN;
            else
                nVector = N_VECTOR_ODD;

            // Get the neighbours for the cell
            for (int j = 0; j < nVector.length; j++) {
                int nField = nVector[j] + i;
                if (Math.abs(nVector[j]) == 1) {
                    // Make sure the west-east fields don't go to a different row.
                    if (nField / WIDTH != i / WIDTH)
                        continue;
                } else {
                    // Make sure the other fields always go to a different row.
                    if (nField / WIDTH == i / WIDTH)
                        continue;
                }
                // Check if in field
                if (nField >= 0 && nField < SIZE) {
                    if (occupancy[nField] == 1) {
                        board[i].neighbours[j] = board[nField];
                    }
                }
            }
        }
    }

    /**
     * Initialize the board to the default configuration
     */
    public void initialize() {
        // Initialize home-positions
        for (int i = 0; i < W_HOME_MAX; i++) {
            if (occupancy[i] == 1) {
                board[i].occupant = WHITE;
            }
        }
        for (int i = B_HOME_MIN; i < board.length; i++) {
            if (occupancy[i] == 1) {
                board[i].occupant = BLACK;
            }
        }
        // Reset current player and game winner
        winner = NONE_WIN;
        currentPlayer = P1;
        pastMoves.clear();
    }

    public void doMove(IMove move) {
        if (winner != NONE_WIN) {
            System.out.println("Game already won.");
            return;
        }
        int stone = board[move.getMove()[0]].occupant;
        if (stone != currentPlayer) {
            System.err.println("Wrong stone in doMove");
            return;
        }
        //
        board[move.getMove()[0]].occupant = EMPTY;
        board[move.getMove()[1]].occupant = stone;
        pastMoves.push(move);
        // Check if a player has won
        int homeStones = 0;
        if (currentPlayer == P1) {
            for (int i = B_HOME_MIN; i < board.length; i++) {
                if (occupancy[i] == 0)
                    continue;
                if (board[i].occupant == WHITE) {
                    homeStones++;
                    if (homeStones == N_PIECES) {
                        winner = P1;
                        return;
                    }
                }
            }
        } else {
            for (int i = 0; i < W_HOME_MAX; i++) {
                if (occupancy[i] == 0)
                    continue;
                if (board[i].occupant == BLACK) {
                    homeStones++;
                    if (homeStones == N_PIECES) {
                        winner = P2;
                        return;
                    }
                }
            }
        }
        currentPlayer = getOpponent(currentPlayer);
    }

    private void generateMovesForPlayer(int player) {
        moves.clear();
        int c = 0;
        // Copy the piece positions
        for (int i = 0; i < SIZE; i++) {
            if (occupancy[i] == 0)
                continue;
            //
            if (board[i].occupant == player) {
                generateMovesForPiece(board[i].position);
                c++;
                // Stop when we've seen all pieces
                if (c == N_PIECES)
                    break;
            }
        }
    }

    /**
     * Generate all moves (including hops) for a given piece
     *
     * @param position Initial position
     */
    public void generateMovesForPiece(int position) {
        seen = new boolean[SIZE];
        int color = board[position].occupant;
        boolean inHome = false;
        if (color == BLACK && position < W_HOME_MAX) { // The piece is in the home-base, not allowed to leave it
            inHome = true;
        } else if (color == WHITE && position > B_HOME_MIN) {
            inHome = true;
        }
        generateMovesForPiece(color, position, position, 0, inHome);
    }

    private void generateMovesForPiece(int color, int initialPosition, int position, int hops, boolean inHome) {
        Field n;
        for (int i = 0; i < board[position].neighbours.length; i++) {
            n = board[position].neighbours[i];
            // Checked this position before || Piece is in home-base, and is not allowed to leave
            if (n == null || seen[n.position] || (inHome && outSideHome(color, n.position)))
                continue;
            // Pieces can move to empty squares, or hop over other pieces
            if (hops == 0 && n.occupant == EMPTY) {
                // Mark as seen so we don't check it again
                seen[n.position] = true;
                moves.add(new Move(initialPosition, n.position, hops));
            } else if (n.occupant != EMPTY && n.neighbours[i] != null && n.neighbours[i].occupant == EMPTY) {
                // Mark as seen so we don't check it again
                seen[n.position] = true;
                seen[n.neighbours[i].position] = true;
                int direction = getHopMultiplier(color, position, n.position);
                // Make sure hops doesn't become 0 again
                int nextHop = (hops + direction == 0) ? -1 : hops + direction;
                moves.add(new Move(initialPosition, n.neighbours[i].position, nextHop));
                // Search for a hop-over
                generateMovesForPiece(color, initialPosition, n.neighbours[i].position, nextHop, inHome);
            }
        }
    }

    /**
     * Returns +1 if forward, or -1 if reverse (relative to the home)
     */
    private int getHopMultiplier(int color, int from, int to) {
        // Black should try to move up, white down
        if (color == BLACK && from >= to) {  // Equality can not be punished
            return 1;
        } else if (color == WHITE && from <= to) {
            return 1;
        }
        return -1;
    }

    /**
     * Is a given position outside the home for a player
     */
    private boolean outSideHome(int color, int position) {
        if (color == BLACK && position > W_HOME_MAX) { // The piece is in the home-base, not allowed to leave it
            return true;
        } else if (color == WHITE && position < B_HOME_MIN) {
            return true;
        }
        return false;
    }

    @Override
    public boolean drawPossible() {
        return false;
    }

    @Override
    public IBoard copy() {
        Board newBoard = new Board();
        // Copy the piece positions
        for (int i = 0; i < SIZE; i++) {
            if (occupancy[i] == 0)
                continue;
            //
            if (board[i].occupant != EMPTY) {
                newBoard.board[i].occupant = board[i].occupant;
            }
        }
        newBoard.winner = winner;
        newBoard.currentPlayer = currentPlayer;
        return newBoard;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        doMove(move);
        return true;
    }

    @Override
    public IMove[] getExpandMoves() {
        generateMovesForPlayer(currentPlayer);
        return moves.getArrayCopy();
    }

    @Override
    public List<IMove> getPlayoutMoves() {
        generateMovesForPlayer(currentPlayer);
        return Arrays.asList(moves.getArrayCopy());
    }

    @Override
    public void undoMove() {
        IMove move = pastMoves.pop();
        if (move != null) {
            currentPlayer = getOpponent(currentPlayer);
            board[move.getMove()[1]].occupant = EMPTY;
            board[move.getMove()[0]].occupant = currentPlayer;
            //
            winner = NONE_WIN;
        }
    }

    @Override
    public int getOpponent(int player) {
        return currentPlayer == P1 ? P2 : P1;
    }

    @Override
    public int checkWin() {
        return winner;
    }

    @Override
    public int checkPlayoutWin() {
        return winner;
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public int getMaxUniqueMoveId() {
        return 100000000;
    }
}
