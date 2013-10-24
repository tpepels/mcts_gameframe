package pentalath.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;

public class Board implements IBoard {
    // @formatter:off
    // This is only to check whether a cell is part of the board (1).
    public static final short[] occupancy =
            {0, 0, 1, 1, 1, 1, 1, 0, 0,
                    0, 1, 1, 1, 1, 1, 1, 0, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 1,
                    1, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 1, 1, 1, 1, 1, 1, 0, 0,
                    0, 0, 1, 1, 1, 1, 1, 0, 0,};
    // @formatter:on
    public static final int FREE = 0;
    public static final int SIZE = 81, REAL_SIZE = 61, WIDTH = 9;
    public static final int NUM_NEIGHBOURS = 6, ROW_SIZE = 5;
    public static final MoveList moves = new MoveList(REAL_SIZE);
    public static final ArrayList<IMove> poMoves = new ArrayList<IMove>(REAL_SIZE);
    //
    private static final int[] N_VECTOR_ODD = {-9, -8, +1, +10, +9, -1}, N_VECTOR_EVEN = {-10, -9, +1, +9, +8, -1};
    // Move ordering that starts at the centre and spirals outwards
    public final int[] spiralOrder = {40, 30, 31, 41, 49, 48, 39, 29, 21, 22, 23, 32, 42, 50, 59,
            58, 57, 47, 38, 28, 20, 11, 12, 13, 14, 24, 33, 43, 51, 60, 68, 67, 66, 65, 56, 46, 37,
            27, 19, 10, 2, 3, 4, 5, 6, 15, 25, 34, 44, 52, 61, 69, 78, 77, 76, 75, 74, 64, 55, 45,
            36};
    // set the bit in this position to 1 if a stone is black.
    private final int BLACK_BIT = 128;
    public Field[] board;
    // private final long[][] zobristPositions;
    // public final long zobristHash, whiteHash, blackHash;
    public int freeSquares;
    public boolean firstMoveBeforePass = false;
    public boolean firstMove = true;
    public int currentPlayer = P1;
    public long totalTime = 900000;
    //
    public ArrayList<Integer> moveList = new ArrayList<Integer>(SIZE);
    public ArrayList<int[]> captureList = new ArrayList<int[]>(SIZE);
    public double maxHistVal;
    public int startindex = 0; // Skip the first when getting a move to make
    public int numCapture = 0;
    // private void hashCurrentPlayer() {
    // if (currentPlayer == Board.P1) {
    // zobristHash ^= blackHash;
    // zobristHash ^= whiteHash;
    // } else {
    // zobristHash ^= whiteHash;
    // zobristHash ^= blackHash;
    // }
    // }
    public int[] playerCaps = new int[2];
    //
    private boolean isEnd = false;
    private int[] freeMoves;
    private int[] capturePositions;
    private int captureI = 0, firstCaptureI = 0;
    // For the win check, this array holds the positions that have been investigated
    private boolean[] seen;

    //
    public Board() {
        // First, generate a random hashing key for all positions
        // zobristPositions = new long[SIZE][];
        // // Use the same seed everytime
        // Random r = new Random();
        // for (int i = 0; i < SIZE; i++) {
        // if (occupancy[i] == 0)
        // continue;
        // // Generate a random number for each possible occupation
        // zobristPositions[i] = new long[2];
        // zobristPositions[i][P1 - 1] = r.nextLong();
        // zobristPositions[i][P2 - 1] = r.nextLong();
        // }
        // whiteHash = r.nextLong();
        // blackHash = r.nextLong();
        // zobristHash ^= whiteHash;
        //
        board = new Field[SIZE];
        // Initialize the empty fields
        for (int i = 0; i < SIZE; i++) {
            // Only use fields that are part of the board
            if (occupancy[i] == 0)
                continue;
            //
            freeSquares++;
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
                        board[i].numNeighbours++;
                        board[i].neighbours[j] = board[nField];
                    }
                }
            }
        }
    }

    @Override
    public Board copy() {
        Board newBoard = new Board();
        for (int i = 0; i < SIZE; i++) {
            if (occupancy[i] == 0)
                continue;
            //
            if (board[i].occupant != FREE) {
                // No need to take undomove into account here,
                // since we will not go back further than the current gamestate!
                newBoard.board[i].occupant = board[i].occupant;
            }
            // Generate a random number for each possible occupation
            // newBoard.zobristPositions[i] = new long[2];
            // newBoard.zobristPositions[i][P1 - 1] = zobristPositions[i][P1 - 1];
            // newBoard.zobristPositions[i][P2 - 1] = zobristPositions[i][P2 - 1];
        }
        //
        // newBoard.zobristHash = zobristHash;
        newBoard.freeSquares = freeSquares;
        newBoard.firstMove = firstMove;
        newBoard.isEnd = isEnd;
        newBoard.currentPlayer = currentPlayer;
        if (moveList.size() > 0)
            newBoard.moveList.add(moveList.get(moveList.size() - 1));
        if (captureList.size() > 0)
            newBoard.captureList.add(captureList.get(captureList.size() - 1));
        return newBoard;
    }

    public long timeLeft() {
        return totalTime;
    }

    public void pass() {
        firstMoveBeforePass = firstMove;
        currentPlayer = getOpponent(currentPlayer);
        // hashCurrentPlayer();
    }

    public void undoPass() {
        currentPlayer = getOpponent(currentPlayer);
        // hashCurrentPlayer();
        firstMove = firstMoveBeforePass;
    }

    /**
     * Put a stone on the board
     *
     * @param pos    The position on the board
     * @param player The current player
     */
    public boolean doMove(int pos, int player) {
        if ((board[pos].occupant == FREE || firstMove) && !isEnd) {
            // First move exception rule for the zobrist hash
            if (firstMove && board[pos].occupant != FREE) {
                // zobristHash ^= zobristPositions[pos][Board.P1 - 1];
                freeSquares++;
            }
            //
            board[pos].occupant = player;
            // If black made a move, we can no longer switch.
            if (player == P2)
                firstMove = false;
            //
            freeSquares--;
            //
            // zobristHash ^= zobristPositions[pos][player - 1];
            moveList.add(pos);
            currentPlayer = getOpponent(currentPlayer);
            // hashCurrentPlayer();
            return true;
        } else {
            System.err.println("Something is wrong in doMove.");
            return false;
        }
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        doMove(move.getMove()[0], player);
        return capturePieces(move.getMove()[0]);
    }

    public int getNextMove(int[] history, int[] bfboard, int[] availmoves, int index) {
        int maxIndex = index;
        double value = 0., maxValue = 0.;
        // Loop through the currently available moves
        for (int i = index; i < availmoves.length; i++) {
            if (bfboard[availmoves[i]] == 0)
                continue;
            // The relative history heuristic
            value = (double) history[availmoves[i]] / (double) bfboard[availmoves[i]];
            // value = history[availmoves[i]];
            if (value > maxValue) { // > ensures that the availmoves order is abided
                maxValue = value;
                maxHistVal = value;
                maxIndex = i;
            }
        }
        // Insert the maxvalue in the current index
        int temp = availmoves[index];
        availmoves[index] = availmoves[maxIndex];
        availmoves[maxIndex] = temp;
        //
        return availmoves[index];
    }

    public int[] getAvailableMoves(int... initorder) {
        if (!firstMove)
            freeMoves = new int[freeSquares];
        else
            freeMoves = new int[spiralOrder.length];
        seen = new boolean[SIZE];

        int c = 0;
        // First add the moves from the initial order list
        for (int i = 0; i < initorder.length; i++) {
            if (initorder[i] > 0 && !seen[initorder[i]]
                    && (board[initorder[i]].occupant == FREE || firstMove)) {
                freeMoves[c] = initorder[i];
                seen[initorder[i]] = true; // Make sure we don't add this move again
                c++;
            }
        }
        startindex = c;
        // Add the moves from the spiral ordering
        for (int i = 0; i < spiralOrder.length; i++) {
            // Check if position is free, and if it is not in the previous ordering
            if ((board[spiralOrder[i]].occupant == 0 || firstMove) && !seen[spiralOrder[i]]) {
                freeMoves[c] = spiralOrder[i];
                c++;
                // No need to look further
                if (c == freeMoves.length)
                    return freeMoves;
            }
        }
        System.err.println("Error in getavailablemoves(int...)");
        return freeMoves;
    }

    @Override
    public MoveList getExpandMoves() {
        int count = (!firstMove) ? freeSquares : REAL_SIZE;
        int c = 0;
        moves.clear();
        //
        for (int i = 0; i < SIZE; i++) {
            if (board[i] == null)
                continue;
            // Check if position is free and add it to the free moves
            if (firstMove || board[i].occupant == 0) {
                moves.add(new Move(i));
                c++;
                if (c == count)
                    break;
            }
        }
        return moves;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        int count = 0;
        count = (!firstMove) ? freeSquares : REAL_SIZE;
        poMoves.clear();
        int c = 0;
        // Add the moves from the spiral ordering
        for (int i = 0; i < SIZE; i++) {
            if (board[i] == null)
                continue;
            // Check if position is free and add it to the free moves
            if (firstMove || board[i].occupant == 0) {
                poMoves.add(new Move(i));
                if (heuristics) {
                    // Prefer the highly connected positions
                    if (board[i].numNeighbours > 4)
                        poMoves.add(new Move(i));
                }
                c++;
                // No need to look further
                if (c == count)
                    return poMoves;
            }
        }
        System.err.println("Error in getPlayoutMoves()");
        return poMoves;
    }

    public boolean capturePieces(int pos) {
        Field[] nb = board[pos].neighbours;
        int player = board[pos].occupant;
        int opponent = getOpponent(player);
        // To mark positions we have already seen.
        seen = new boolean[SIZE];
        boolean suicide = false, capture = false;
        Field capturePos = null;
        // Check if this move is a suicide move.
        if (!checkFree(board[pos])) {
            suicide = true;
        }
        // Check for capture condition (around the placed stone only!)
        for (Field f : nb) {
            if (f == null)
                continue;
            // This position was already checked or is free
            if (f.occupant == FREE)
                continue;
            seen = new boolean[SIZE];
            // Check the position's freedom.
            if (!checkFree(f)) {
                if (f.occupant == opponent) {
                    capture = true;
                    capturePos = f;
                    firstCaptureI = 0;
                    break;
                } else {
                    suicide = true;
                }
            }
        }
        numCapture = 0;
        captureI = 0;
        playerCaps = new int[2]; // Pieces captured per player COLOR-1.
        if (suicide && capture) { // Freedom suicide
            // Will now store all positions to be eliminated.
            seen = new boolean[SIZE];
            numCapture++;
            checkCapturePositions(capturePos);
            capturePositions = new int[numCapture];
            capturePositions();
            freeSquares += numCapture;
        } else if (capture) { // Capture!
            numCapture++;
            // Will now store all positions to be eliminated.
            seen = new boolean[SIZE];
            checkCapturePositions(capturePos);
            capturePositions = new int[numCapture];
            capturePositions();
            freeSquares += numCapture;
        } else if (suicide) {
            // zobristHash ^= zobristPositions[pos][board[pos].occupant - 1];
            // Not allowed!
            board[pos].occupant = FREE;
            freeSquares++;
            // reset the current player remove the move
            moveList.remove(moveList.size() - 1);
            currentPlayer = player;
            // hashCurrentPlayer();
            return false;
        }
        // System.out.println("Captured " + playerCaps[0] + " white pieces");
        // System.out.println("Captured " + playerCaps[1] + " black pieces");
        //
        if (numCapture == 0)
            capturePositions = new int[0];
        //
        captureList.add(capturePositions);
        return true;
    }

    /**
     * Removes the stone last placed, resets the captured stones
     */
    @Override
    public void undoMove() {
        int moveIndex = moveList.size() - 1;
        // No moves left!
        if (moveIndex < 0) {
            System.err.println("No moves to undo!");
            return;
        }
        isEnd = false;
        //
        int move = moveList.get(moveIndex);
        // return the captured stones
        int[] captures = captureList.get(moveIndex);
        int color, position;
        for (int i = 0; i < captures.length; i++) {
            color = isBlack(captures[i]) ? P2 : P1;
            position = getPosition(captures[i]);
            board[position].occupant = color;
            // return the stone to the hash
            // zobristHash ^= zobristPositions[position][color - 1];
            freeSquares--;
        }
        currentPlayer = board[move].occupant;
        if (currentPlayer == 0) {
            System.err.println("error in undo move");
            return;
        }
        // hashCurrentPlayer();
        // // This means it was the first move
        if (currentPlayer == Board.P2 && freeSquares == 60) {
            // zobristHash ^= zobristPositions[move][Board.BLACK - 1];
            // zobristHash ^= zobristPositions[move][Board.WHITE - 1];
            board[move].occupant = Board.P1;
            firstMove = true;
        } else {
            // Remove the stone from the hash
            // zobristHash ^= zobristPositions[move][currentPlayer - 1];
            // Remove the stone from the position
            board[move].occupant = FREE;
            freeSquares++;
        }
        firstMove = freeSquares == 61 || (currentPlayer == Board.P2 && freeSquares == 60);
        //
        captureList.remove(moveIndex);
        moveList.remove(moveIndex);
    }

    private void capturePositions() {
        // We can start capturing at the lowest index.
        for (int i = firstCaptureI; i < SIZE; i++) {
            if (seen[i]) {
                capturePositions[captureI] = setColor(board[i].position,
                        board[i].occupant == Board.P2);
                captureI++;
                // zobristHash ^= zobristPositions[i][board[i].occupant - 1];
                playerCaps[board[i].occupant - 1]++;
                board[i].occupant = FREE;
                // stop after all positions are captured
                if (captureI == numCapture)
                    return;
            }
        }
        System.out.println("HOHOHO!");
    }

    private void checkCapturePositions(Field field) {
        Field[] nb = field.neighbours;
        seen[field.position] = true;
        int player = field.occupant;
        for (Field f : nb) {
            if (f == null)
                continue;
            // Position was previously investigated
            if (seen[f.position])
                continue;
            // check if this position should be cleared
            if (f.occupant == player) {
                seen[f.position] = true;
                // Remember where to start the loop for capturing
                if (f.position < firstCaptureI)
                    firstCaptureI = f.position;
                //
                numCapture++;
                checkCapturePositions(f);
            }
        }
    }

    private boolean checkFree(Field field) {
        Field[] nb = field.neighbours;
        boolean free = false;
        for (Field f : nb) {
            if (f == null)
                continue;
            // A neighbour is free --> freedom!
            if (f.occupant == FREE) {
                return true;
            }
            // This position was already checked
            if (seen[f.position] || f.occupant != field.occupant)
                continue;
            // Don't check this position again!
            seen[f.position] = true;
            //
            free = checkFree(f);
            if (free)
                return true;
        }
        // None of the neighbours is free
        return free;
    }

    @Override
    public int getOpponent(int player) {
        if (player == Board.P1)
            return Board.P2;
        else if (player == Board.P2)
            return Board.P1;
        else
            throw new RuntimeException("Illegal player in getOpponent.");
    }

    /**
     * Checks if one of the players has won the pentalath.game, or the pentalath.game is a draw.
     *
     * @return NONE = 0, WHITE_WIN = Board.WHITE, BLACK_WIN = Board.BLACK, DRAW = 3;
     */
    @Override
    public int checkWin() {
        Field lastPosition = board[moveList.get(moveList.size() - 1)];
        // No need to check if there are less than 8 pieces on the board
        if ((Board.REAL_SIZE - freeSquares) < (ROW_SIZE * 2) - 2)
            return NONE_WIN;
        int player = lastPosition.occupant;
        // Each row is of at least length 1 :)
        Field currentField;
        int[] rowLength = new int[3];
        for (int i = 0; i < rowLength.length; i++) {
            // The current stone
            rowLength[i]++;
        }
        // Check once in each direction.
        for (int j = 0; j < NUM_NEIGHBOURS; j++) {
            currentField = lastPosition.neighbours[j];
            //
            if (currentField == null)
                continue;

            // Check for a row of 5 in each direction.
            while (currentField != null && currentField.occupant == player) {
                rowLength[j % 3]++;
                // One of the players has won!
                if (rowLength[j % 3] == ROW_SIZE) {
                    isEnd = true;
                    return player;
                }
                currentField = currentField.neighbours[j];
            }
        }
        // There are fewer free squares on the board than needed to build a row
        if (freeSquares == 0) {
            isEnd = true;
            return DRAW;
        }
        // None of the players win, continue the pentalath.game
        return NONE_WIN;
    }

    @Override
    public int checkPlayoutWin() {
        return checkWin();
    }

    /**
     * Sets the BLACK_BIT bit to true if black
     *
     * @return the new number
     */
    private int setColor(int number, boolean black) {
        if (black)
            number += BLACK_BIT;
        return number;
    }

    private boolean isBlack(int number) {
        return (number >= BLACK_BIT);
    }

    private int getPosition(int number) {
        if (number >= 128)
            return number - BLACK_BIT;
        else
            return number;
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public int getMaxUniqueMoveId() {
        return SIZE;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void newDeterminization(int myPlayer) {
    }

    @Override
    public boolean isPartialObservable() {
        return false;
    }

    @Override
    public boolean isLegal(IMove move) {
        return true;
    }

    @Override
    public boolean drawPossible() {
        return true;
    }
}
