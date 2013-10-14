package chinesecheckers.game;

import ai.FastRandom;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
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
    private static final MoveList moves = new MoveList(1000);
    private static final ArrayList<IMove> playoutMoves = new ArrayList<IMove>(100);
    private static final int B_HOME_MIN = 169, W_HOME_MAX = 52, B_TARGET = 0, W_TARGET = 16;
    private static final int[] N_VECTOR_ODD = {-13, -12, +1, +14, +13, -1}, N_VECTOR_EVEN = {-14, -13, +1, +13, +12, -1};
    private final static long[] seen = new long[SIZE];          // List to keep track of positions seen for jumping
    private static FastRandom random = new FastRandom();
    private static long seenIndex = 1;
    //
    public final Field[] board;
    public final Piece[] pieces = new Piece[20];
    private int winner = NONE_WIN, currentPlayer = P1;
    private int[] homePieces = new int[]{0, 0};
    //
    private Stack<IMove> pastMoves = new Stack<IMove>();        // Stack for undo-move
    private int target;

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
                // Check if neighbour in field
                if (nField >= 0 && nField < SIZE) {
                    if (occupancy[nField] == 1) {
                        board[i].neighbours[j] = board[nField];
                    }
                }
            }
        }
    }

    public static MoveList getMoves() {
        return moves;
    }

    /**
     * Initialize the board to the default configuration
     */
    public void initialize() {
        int c = 0;
        Piece p;
        // Initialize home-positions
        for (int i = 0; i < W_HOME_MAX; i++) {
            if (occupancy[i] == 1) {
                p = new Piece(c, i, WHITE);
                pieces[c] = p;
                board[i].occupant = p;
                c++;
            }
        }
        // Black pieces have id >= 10
        for (int i = B_HOME_MIN; i < board.length; i++) {
            if (occupancy[i] == 1) {
                p = new Piece(c, i, BLACK);
                pieces[c] = p;
                board[i].occupant = p;
                c++;
            }
        }
        // Reset current player and game winner
        winner = NONE_WIN;
        currentPlayer = P1;
        homePieces = new int[]{0, 0};
        pastMoves.clear();
    }

    public void doMove(IMove move) {
        Piece stone = board[move.getMove()[0]].occupant;
        //
        board[move.getMove()[0]].occupant = null;
        board[move.getMove()[1]].occupant = stone;
        // Keep track of the stone's location
        stone.location = move.getMove()[1];
        // Check if the piece was moved inside the home from outside the home
        if (insideHome(currentPlayer, move.getMove()[1]) && outSideHome(currentPlayer, move.getMove()[0])) {
            homePieces[currentPlayer - 1]++;
            // Remember that this was a home-coming move, for undomove
            ((Move) move).homeMove = true;
            if (homePieces[currentPlayer - 1] == N_PIECES)
                winner = currentPlayer;
        }
        pastMoves.push(move);
        currentPlayer = getOpponent(currentPlayer);
    }

    private void generateMovesForPlayer(int player, boolean closer) {
        moves.clear();
        int startI = (player == P1) ? 0 : N_PIECES;
        int endI = (player == P1) ? N_PIECES : pieces.length;
        for (int i = startI; i < endI; i++) {
            generateMovesForPiece(pieces[i].location, closer);
        }
    }

    /**
     * Generate all moves (including hops) for a given piece
     *
     * @param position Initial position
     */
    public void generateMovesForPiece(int position, boolean closerOnly) {
        seenIndex++;
        int colour = board[position].occupant.colour;
        if (colour == EMPTY)
            System.err.println("Wrong colour in generateMovesForPiece");
        target = (colour == BLACK) ? B_TARGET : W_TARGET;
        generateMovesForPiece(colour, position, position, 0, insideHome(colour, position),
                closerOnly, getDistanceToHome(position, target));
    }

    private void generateMovesForPiece(int colour, int initialPosition, int position, int hops, boolean inHome, boolean closerOnly, double initDistance) {
        Field n;
        for (int i = 0; i < board[position].neighbours.length; i++) {
            n = board[position].neighbours[i];
            // Checked this position before || Piece is in home-base, and is not allowed to leave
            if (n == null || seen[n.position] == seenIndex || (inHome && outSideHome(colour, n.position)))
                continue;
            // Pieces can move to empty squares, or hop over other pieces
            if (hops == 0 && n.occupant == null) {
                // Mark as seen so we don't check it again
                seen[n.position] = seenIndex;
                if (!closerOnly || initDistance > getDistanceToHome(n.position, target))
                    moves.add(new Move(initialPosition, n.position, hops));
            } else if (n.occupant != null && n.neighbours[i] != null && n.neighbours[i].occupant == null) {
                // Mark as seen so we don't check it again
                seen[n.position] = seenIndex;
                seen[n.neighbours[i].position] = seenIndex;
                // Check if the move is closer to target
                if (!closerOnly || initDistance > getDistanceToHome(n.position, target))
                    moves.add(new Move(initialPosition, n.neighbours[i].position, hops + 1));
                // Search for a hop-over
                generateMovesForPiece(colour, initialPosition, n.neighbours[i].position, hops + 1, inHome, closerOnly, initDistance);
            }
        }
    }

    private int getDistanceToHome(int from, int target) {
        return Math.abs((from / WIDTH) - target);
    }

    private boolean insideHome(int colour, int position) {
        if (colour == BLACK && position < W_HOME_MAX) { // The piece is in the home-base, not allowed to leave it
            return true;
        } else if (colour == WHITE && position > B_HOME_MIN) {
            return true;
        }
        return false;
    }

    /**
     * Is a given position outside the home for a player
     */
    private boolean outSideHome(int colour, int position) {
        if (colour == BLACK && position > W_HOME_MAX) { // The piece is in the home-base, not allowed to leave it
            return true;
        } else if (colour == WHITE && position < B_HOME_MIN) {
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
        Piece p;
        // Copy the piece positions
        for (int i = 0; i < pieces.length; i++) {
            p = pieces[i].copy();
            newBoard.board[p.location].occupant = p;
            newBoard.pieces[i] = p;
        }
        newBoard.winner = winner;
        newBoard.currentPlayer = currentPlayer;
        newBoard.homePieces[0] = homePieces[0];
        newBoard.homePieces[1] = homePieces[1];
        return newBoard;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        doMove(move);
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        generateMovesForPlayer(currentPlayer, false);
        return moves;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        if (random.nextDouble() < 0.95) {
            generateMovesForPlayer(currentPlayer, true);
            // No moves were generated because no moves is strictly closer to goal
            if (moves.size() == 0)
                generateMovesForPlayer(currentPlayer, false);
        } else {
            generateMovesForPlayer(currentPlayer, false);
        }
        //
        if (heuristics) {
            playoutMoves.clear();
            int hops;
            for (int i = 0; i < moves.size(); i++) {
                playoutMoves.add(moves.get(i));
                hops = moves.get(i).getType();
                // Add moves proportional to the number of hops
                for (int j = 0; j < hops; j++) {
                    playoutMoves.add(moves.get(i));
                }
            }
            return playoutMoves;
        } else {
            return Arrays.asList(moves.getArrayCopy());
        }
    }

    @Override
    public void undoMove() {
        IMove move = pastMoves.pop();
        if (move != null) {
            currentPlayer = getOpponent(currentPlayer);
            // Reset the location of the piece
            Piece p = board[move.getMove()[1]].occupant;
            p.location = move.getMove()[0];
            board[move.getMove()[1]].occupant = null;
            board[move.getMove()[0]].occupant = p;
            // This move places the piece outside the home
            if (((Move) move).homeMove) {
                homePieces[currentPlayer - 1]--;
            }
            //
            winner = NONE_WIN;
        }
    }

    @Override
    public int getOpponent(int player) {
        return (currentPlayer == P1) ? P2 : P1;
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

    @Override
    public void newDeterminization(int myPlayer) {
        // Only required for partial observable games
    }

    @Override
    public boolean isPartialObservable() {
        return false;
    }

    @Override
    public boolean isLegal(IMove move) {
        return true;
    }
}
