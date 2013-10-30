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
    public static final int WHITE = P1, BLACK = P2, WIDTH = 10, HEIGHT = 13, MAX_REV = 2;
    public static final int[] occupancy = new int[]
            {
                    0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
                    0, 0, 0, 0, 1, 1, 0, 0, 0, 0,
                    0, 0, 0, 0, 1, 1, 1, 0, 0, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 0, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    0, 0, 0, 0, 1, 1, 1, 0, 0, 0,
                    0, 0, 0, 0, 1, 1, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            };
    private static final int SIZE = WIDTH * HEIGHT, B_HOME_MIN = 103, W_HOME_MAX = 27, N_PIECES = 6;
    private static final int[][] B_TARGET = {{5, 0}, {4, 1}, {5, 1}, {4, 2}, {5, 2}, {6, 2}},
            W_TARGET = {{5, 12}, {4, 11}, {5, 11}, {4, 10}, {5, 10}, {6, 10}};
    // Neighbour vectors, differs for odd and even fields
    private static final int[] N_VECTOR_ODD = {-10, -9, +1, +11, +10, -1}, N_VECTOR_EVEN = {-11, -10, +1, +10, +9, -1};
    // Move lists
    private static final MoveList moves = new MoveList(1000);
    private static final ArrayList<IMove> playoutMoves = new ArrayList<IMove>(100);
    // List to keep track of positions seen for jumping
    private static final long[] seen = new long[SIZE];
    private static long seenIndex = 1;
    // The board and pieces
    public final Field[] board;
    public final Piece[] pieces = new Piece[N_PIECES + N_PIECES];
    private final Stack<IMove> pastMoves = new Stack<IMove>();        // Stack for undo-move
    //
    private final FastRandom random = new FastRandom();
    private Move[] lastMove = new Move[2];
    private int[] reverseMoves = {0, 0};
    private int[] homePieces = new int[2], target;
    private int[][] targets; // Holds the targets for the current player to move
    private int winner = NONE_WIN, currentPlayer = P1;

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
    @Override
    public void initialize() {
        int c = 0;
        Piece p;
        // Initialize the empty fields
        for (int i = 0; i < SIZE; i++) {
            // Only use fields that are part of the board
            if (occupancy[i] == 0)
                continue;
            //
            board[i].occupant = null;
        }
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
        homePieces[0] = N_PIECES;
        homePieces[1] = N_PIECES;
        pastMoves.clear();
    }

    public void doMove(IMove move) {
        Piece piece = board[move.getMove()[0]].occupant;
        board[move.getMove()[0]].occupant = null;
        board[move.getMove()[1]].occupant = piece;
        // Keep track of the stone's location
        piece.location = move.getMove()[1];
        // This is a bit of a trick, mail me for questions about this :)
        if (insideHome(getOpponent(currentPlayer), move.getMove()[0]) && outSideHome(getOpponent(currentPlayer), move.getMove()[1])) {
            homePieces[getOpponent(currentPlayer) - 1]--;
        }
        if (insideHome(getOpponent(currentPlayer), move.getMove()[1]) && outSideHome(getOpponent(currentPlayer), move.getMove()[0])) {
            homePieces[getOpponent(currentPlayer) - 1]++;
        }
        // Check if the piece was moved inside the home from outside the home
        if (insideHome(currentPlayer, piece.location) && outSideHome(currentPlayer, move.getMove()[0])) {
            homePieces[currentPlayer - 1]++;
            if (homePieces[currentPlayer - 1] == N_PIECES) {
                winner = currentPlayer;
            }
        }
        lastMove[currentPlayer - 1] = (Move) move;
        if (lastMove[currentPlayer - 1].isReverse(move))
            reverseMoves[currentPlayer - 1]++;
        else
            reverseMoves[currentPlayer - 1] = 0;
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
        targets = (colour == BLACK) ? B_TARGET : W_TARGET;
        target = targets[0];
        if (homePieces[colour - 1] != N_PIECES) {
            int c = 0;
            while (c < targets.length && board[target[0] + (target[1] * WIDTH)].occupant != null) {
                target = targets[++c];
            }
        }
        generateMovesForPiece(colour, position, position, 0, insideHome(colour, position),
                closerOnly, getDistanceToHome(position, target));
    }

    private void generateMovesForPiece(int colour, int initialPosition, int position, int hops, boolean inHome, boolean closerOnly, double initDistance) {
        Field n;
        Move m;
        for (int i = 0; i < board[position].neighbours.length; i++) {
            n = board[position].neighbours[i];
            // Checked this position before || Piece is in home-base, and is not allowed to leave
            if (n == null || seen[n.position] == seenIndex || (inHome && outSideHome(colour, n.position)))
                continue;
            // Mark as seen so we don't check it again
            seen[n.position] = seenIndex;
            // Pieces can move to empty squares, or hop over other pieces
            if (hops == 0 && n.occupant == null) {
                if (!closerOnly || initDistance > getDistanceToHome(n.position, target)) {
                    m = new Move(initialPosition, n.position, hops);
                    if (!tooManyReversals(m))
                        moves.add(m);
                }
            } else if (n.occupant != null && n.neighbours[i] != null && n.neighbours[i].occupant == null) {
                // Mark as seen so we don't check it again
                seen[n.neighbours[i].position] = seenIndex;
                if (inHome && outSideHome(colour, n.neighbours[i].position))
                    continue;
                // Check if the move is closer to target, and the neighbour is not outside the home, if the piece is inside
                if (!closerOnly || initDistance > getDistanceToHome(n.position, target)) {
                    m = new Move(initialPosition, n.neighbours[i].position, hops + 1);
                    if (!tooManyReversals(m))
                        moves.add(m);
                }
                // Search for a hop-over
                generateMovesForPiece(colour, initialPosition, n.neighbours[i].position, hops + 1, inHome, closerOnly, initDistance);
            }
        }
    }

    public boolean tooManyReversals(Move move) {
        return reverseMoves[currentPlayer - 1] > MAX_REV && move.isReverse(lastMove[currentPlayer - 1]);
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
        newBoard.lastMove[0] = lastMove[0];
        newBoard.lastMove[1] = lastMove[1];
        newBoard.reverseMoves[0] = reverseMoves[0];
        newBoard.reverseMoves[1] = reverseMoves[1];
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
        return moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        moves.clear();
        int startI = (currentPlayer == P1) ? 0 : N_PIECES;
        int piece = random.nextInt(N_PIECES);
        int c = 0;
        while (moves.size() == 0 && c < N_PIECES) {
            generateMovesForPiece(pieces[piece + startI].location, true);
            piece++;
            c++;
            if (piece >= N_PIECES)
                piece = 0;
        }
        // No moves were generated because no moves is strictly closer to goal
        if (moves.size() == 0) {
            generateMovesForPlayer(currentPlayer, false);
            return Arrays.asList(moves.getArrayCopy());
        } else if (heuristics) { // Don't do the hop-thing with reverse moves in the list
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

            if (insideHome(getOpponent(currentPlayer), move.getMove()[0]) && outSideHome(getOpponent(currentPlayer), move.getMove()[1])) {
                homePieces[getOpponent(currentPlayer) - 1]++;
            }

            if (insideHome(getOpponent(currentPlayer), move.getMove()[1]) && outSideHome(getOpponent(currentPlayer), move.getMove()[0])) {
                homePieces[getOpponent(currentPlayer) - 1]--;
            }

            if (insideHome(currentPlayer, move.getMove()[1]) && outSideHome(currentPlayer, move.getMove()[0])) {
                homePieces[currentPlayer - 1]--;
                if (homePieces[currentPlayer - 1] < 0) {
                    System.err.println("home pieces smaller than 0!");
                }
            }
            winner = NONE_WIN;
            lastMove[currentPlayer - 1] = null;
        }
    }

    private int getDistanceToHome(int from, int[] target) {
        return Math.abs((from % WIDTH) - target[0]) + Math.abs((from / WIDTH) - target[1]);
    }

    private boolean insideHome(int colour, int position) {
        return (colour == BLACK && position < W_HOME_MAX) || (colour == WHITE && position > B_HOME_MIN);
    }

    private boolean outSideHome(int colour, int position) {
        return (colour == BLACK && position > W_HOME_MAX) || (colour == WHITE && position < B_HOME_MIN);
    }

    @Override
    public boolean drawPossible() {
        return false;
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
        return 125126;
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
