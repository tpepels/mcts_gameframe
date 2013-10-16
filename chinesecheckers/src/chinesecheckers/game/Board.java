package chinesecheckers.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import chinesecheckers.gui.CCGui;

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
    private static final int B_HOME_MIN = 172, W_HOME_MAX = 47;
    private static final int[][] B_TARGET = {{6, 0}, {5, 1}, {6, 1}, {5, 2}, {6, 2}, {7, 2}, {4, 3}, {5, 3}, {6, 3}, {7, 3}},
            W_TARGET = {{6, 16}, {5, 15}, {6, 15}, {5, 14}, {6, 14}, {7, 14}, {4, 13}, {5, 13}, {6, 13}, {7, 13}};
    private static final int[] N_VECTOR_ODD = {-13, -12, +1, +14, +13, -1}, N_VECTOR_EVEN = {-14, -13, +1, +13, +12, -1};
    private static final long[] seen = new long[SIZE];          // List to keep track of positions seen for jumping
    private static long seenIndex = 1;
    //
    public final Field[] board;
    public final Piece[] pieces = new Piece[20];
    private final Stack<IMove> pastMoves = new Stack<IMove>();        // Stack for undo-move
    private int[] homePieces = new int[2], target;
    private int[][] targets;
    private int winner = NONE_WIN, currentPlayer = P1;
    private int numMoves = 0;

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
        if (insideHome(currentPlayer, move.getMove()[0]) && outSideHome(currentPlayer, move.getMove()[1]))
            System.err.println("Invalid move!");
        Piece stone = board[move.getMove()[0]].occupant;
        board[move.getMove()[0]].occupant = null;
        board[move.getMove()[1]].occupant = stone;
        // Keep track of the stone's location
        stone.location = move.getMove()[1];
        // This is a bit of a trick, mail me for questions about this :)
        if (insideHome(getOpponent(currentPlayer), move.getMove()[0]) && outSideHome(getOpponent(currentPlayer), move.getMove()[1])) {
            homePieces[getOpponent(currentPlayer) - 1]--;
            ((Move) move).leftHome = true;
        } else if (insideHome(getOpponent(currentPlayer), move.getMove()[1]) && outSideHome(getOpponent(currentPlayer), move.getMove()[0])) {
            homePieces[getOpponent(currentPlayer) - 1]++;
            ((Move) move).backHome = true;
        }
        // Check if the piece was moved inside the home from outside the home
        if (insideHome(currentPlayer, move.getMove()[1]) && outSideHome(currentPlayer, move.getMove()[0])) {
            homePieces[currentPlayer - 1]++;
            // Remember that this was a homecoming move, for undo move
            ((Move) move).homeMove = true;
            if (homePieces[currentPlayer - 1] == N_PIECES) {
                winner = currentPlayer;
                //System.out.println(numMoves);
            }
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
        targets = (colour == BLACK) ? B_TARGET : W_TARGET;
        target = targets[0];
        if (homePieces[colour - 1] != N_PIECES) {
            int c = 0;
            while (board[target[0] + (target[1] * WIDTH)].occupant != null) {
                target = targets[++c];
            }
        }
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
                //
                if (inHome && outSideHome(colour, n.neighbours[i].position))
                    continue;
                // Check if the move is closer to target, and the neighbour is not outside the home, if the piece is inside
                if (!closerOnly || initDistance > getDistanceToHome(n.position, target))
                    moves.add(new Move(initialPosition, n.neighbours[i].position, hops + 1));
                // Search for a hop-over
                generateMovesForPiece(colour, initialPosition, n.neighbours[i].position, hops + 1, inHome, closerOnly, initDistance);
            }
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
        numMoves++;
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        generateMovesForPlayer(currentPlayer, false);
        return moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        generateMovesForPlayer(currentPlayer, true);
        // No moves were generated because no moves is strictly closer to goal
        if (moves.size() == 0)
            generateMovesForPlayer(currentPlayer, false);
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
            } else if (((Move) move).leftHome) {
                homePieces[getOpponent(currentPlayer) - 1]++;
            } else if (((Move) move).backHome) {
                homePieces[getOpponent(currentPlayer) - 1]--;
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
