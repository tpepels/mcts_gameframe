package chinesecheckers.game;

import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastTanh;
import framework.util.StatCounter;

import java.util.*;

public class Board implements IBoard {
    public static final int EMPTY = 0, WHITE = P1, BLACK = P2, WIDTH = 10, HEIGHT = 13, MAX_MOVES = 1000;
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
    private static final ArrayList<IMove> homeComingMoves = new ArrayList<IMove>(20);
    private static final Random random = new Random();
    // List to keep track of positions seen for jumping
    private static final long[] seen = new long[SIZE];
    private static long seenIndex = 1;
    // Zobrist stuff
    static long[][] zbnums = null;
    static long blackHash, whiteHash;
    private long zbHash = 0;
    // The board and pieces
    public final Field[] board;
    public final Piece[] pieces = new Piece[N_PIECES + N_PIECES];
    private final Stack<IMove> pastMoves = new Stack<IMove>();        // Stack for undo-move
    //
    private int[] homePieces = new int[2], target;
    private int[][] targets; // Holds the targets for the current player to move
    private int winner = NONE_WIN, currentPlayer = P1, nMoves = 0;

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
        nMoves = 0;
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

        // initialize the zobrist numbers

        if (zbnums == null) {
            // init the zobrist numbers
            Random rng = new Random();
            //
            zbnums = new long[occupancy.length][3];
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
        for (int r = 0; r < occupancy.length; r++) {
            if (occupancy[r] == 0)
                continue;
            zbHash ^= zbnums[r][EMPTY];
        }
        zbHash ^= whiteHash;
    }

    public void doMove(IMove move) {

        zbHash ^= zbnums[move.getMove()[0]][currentPlayer];
        zbHash ^= zbnums[move.getMove()[1]][EMPTY];

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
        pastMoves.push(move);
        zbHash ^= zbnums[move.getMove()[0]][EMPTY];
        zbHash ^= zbnums[move.getMove()[1]][currentPlayer];
        currentPlayer = getOpponent(currentPlayer);
        hashCurrentPlayer();
        nMoves++;
    }

    private void hashCurrentPlayer() {
        if (currentPlayer == Board.P1) {
            zbHash ^= blackHash;
            zbHash ^= whiteHash;
        } else {
            zbHash ^= whiteHash;
            zbHash ^= blackHash;
        }
    }

    private void generateMovesForPlayer(int player, boolean closer) {
        moves.clear();
        homeComingMoves.clear();
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
        for (int i = 0; i < board[position].neighbours.length; i++) {
            n = board[position].neighbours[i];
            // Checked this position before || Piece is in home-base, and is not allowed to leave
            if (n == null || seen[n.position] == seenIndex || (inHome && outSideHome(colour, n.position)))
                continue;
            // Mark as seen so we don't check it again
            seen[n.position] = seenIndex;
            // Pieces can move to empty squares, or hop over other pieces
            if (hops == 0 && n.occupant == null) {
                if (!closerOnly || initDistance >= getDistanceToHome(n.position, target)) {
                    moves.add(new Move(initialPosition, n.position, hops));
                    // Add the moves to the list of home coming moves
                    if (!inHome && insideHome(currentPlayer, n.position)) {
                        homeComingMoves.add(new Move(initialPosition, n.position, hops));
                    }
                }
            } else if (n.occupant != null && n.neighbours[i] != null && n.neighbours[i].occupant == null) {
                // Mark as seen so we don't check it again
                seen[n.neighbours[i].position] = seenIndex;
                if (inHome && outSideHome(colour, n.neighbours[i].position))
                    continue;
                // Check if the move is closer to target, and the neighbour is not outside the home, if the piece is inside
                if (!closerOnly || initDistance >= getDistanceToHome(n.position, target)) {
                    moves.add(new Move(initialPosition, n.neighbours[i].position, hops + 1));
                    // Add the moves to the list of home coming moves
                    if (!inHome && insideHome(currentPlayer, n.neighbours[i].position)) {
                        for (int k = 0; k < hops + 1; k++)
                            homeComingMoves.add(new Move(initialPosition, n.neighbours[i].position, hops + 1));
                    }
                }
                // Search for a hop-over
                generateMovesForPiece(colour, initialPosition, n.neighbours[i].position, hops + 1, inHome, closerOnly, initDistance);
            }
        }
    }

    private int getZbId(int p) {
        int id = p * 3;
        if (board[p].occupant != null) {
            if (board[p].occupant.colour == WHITE)
                id += 1;
            else if (board[p].occupant.colour == BLACK)
                id += 2;
        }
        return id;
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
        newBoard.nMoves = nMoves;
        newBoard.zbHash = zbHash;
        return newBoard;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        doMove(move);
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        generateMovesForPlayer(currentPlayer, true);
        if (moves.size() == 0)
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
            homeComingMoves.clear();
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
            // Play moves that lead to the home first
            if (homeComingMoves.size() > 0) {
                return homeComingMoves;
            }
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
            zbHash ^= zbnums[move.getMove()[0]][EMPTY];
            zbHash ^= zbnums[move.getMove()[1]][currentPlayer];
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
            }
            zbHash ^= zbnums[move.getMove()[0]][currentPlayer];
            zbHash ^= zbnums[move.getMove()[1]][EMPTY];
            hashCurrentPlayer();
            winner = NONE_WIN;
            nMoves--;
        }
    }

    // Gets the closest distance to the home base from this position
    private int getDistClosestToHome(int from) {
        if (board[from] == null)
            return 0;

        int colour = board[from].occupant.colour;
        targets = (colour == BLACK) ? B_TARGET : W_TARGET;
        target = targets[0];

        if (homePieces[colour - 1] != N_PIECES) {
            int c = 0;
            while (c < targets.length && board[target[0] + (target[1] * WIDTH)].occupant != null) {
                target = targets[++c];
            }
        }

        return getDistanceToHome(from, target);
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
    public boolean noMovesIsDraw() {
        return false;
    }

    @Override
    public int getOpponent(int player) {
        return (currentPlayer == P1) ? P2 : P1;
    }

    @Override
    public int checkWin() {
        // Cut off games that take too long
        if (winner == NONE_WIN && nMoves > MAX_MOVES)
            return DRAW;
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
        return 125126 * 2;
    }

    @Override
    public void newDeterminization(int myPlayer, boolean postMove) {
        // Only required for partial observable games
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
    public double evaluate(int player, int version) {

        // Adds up the distance to closest home base for all pieces
        // Compares that to the opponent's 
        // As used by Nathan in his MCTS players

        int colour = (player == P1 ? WHITE : BLACK);
        if (homePieces[colour - 1] == N_PIECES)
            return 1;

        int startI = (player == P1) ? 0 : N_PIECES;
        int endI = (player == P1) ? N_PIECES : pieces.length;

        int startO = (player == P1) ? N_PIECES : 0;
        int endO = (player == P1) ? pieces.length : N_PIECES;

        int mydist = 0;
        for (int i = startI; i < endI; i++) {
            mydist += getDistClosestToHome(pieces[i].location);
        }

        int oppdist = 0;
        for (int i = startO; i < endO; i++) {
            oppdist += getDistClosestToHome(pieces[i].location);
        }

        double distDiff = (oppdist - mydist);
        //System.out.println("dist diff = " + distDiff);
        double score_th = FastTanh.tanh(distDiff / 10.0);
        return score_th;
    }

    @Override
    public void initNodePriors(int parentPlayer, StatCounter stats, IMove move, int npvisits) {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public double getQuality() {
        if (winner == P1_WIN)
            return ((double) (N_PIECES - homePieces[1]) / (double) N_PIECES);
        else if (winner == P2_WIN)
            return ((double) (N_PIECES - homePieces[0]) / (double) N_PIECES);
        return 1;
    }

    @Override
    public MoveList getOrderedMoves() {
        return null;
    }

    @Override
    public long hash() {
        return zbHash;
    }

    @Override
    public boolean poMoves() {
        return false;
    }

    @Override
    public int getNPlayers() {
        return 2;
    }

    public String toString() {
        return ("toString mostly unimplemented.. :(  nMoves = " + nMoves);
    }
}
