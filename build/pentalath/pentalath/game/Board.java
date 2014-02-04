package pentalath.game;

import ai.FastTanh;
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
    private final int P_INF = 2000000;
    public Field[] board;
    // private final long[][] zobristPositions;
    // public final long zobristHash, whiteHash, blackHash;
    public int freeSquares, winner, nPieces1, nPieces2;
    public boolean firstMoveBeforePass = false;
    public boolean firstMove = true;
    public int currentPlayer = P1;
    public int dontAdd = -1, dontAddPlayer = -1;
    public long totalTime = 900000;
    //
    public ArrayList<Integer> moveList = new ArrayList<Integer>(SIZE);
    public ArrayList<int[]> captureList = new ArrayList<int[]>(SIZE);
    public double maxHistVal;
    public int startindex = 0, numCapture = 0;
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
    private int captureI = 0, firstCaptureI = 0, nMoves = 0;
    // For the win check, this array holds the positions that have been investigated
    private boolean[] seen;
    private int opp, longestRow;
    private boolean[] closedrow = new boolean[3], extrafreedom = new boolean[3];
    private int[] rowLength = new int[3], freedom = new int[6], totFreedom = new int[6];
    // Weights for the features
    // [0] Captures  (not used yet, TODO)
    // [1] my longest row,
    // [2] min. freedom of my pieces,
    // [3] min. freedom of opponent's pieces,
    // [4] longest opponent's row,
    // [5] pieces capped by opponent.
    // [6] my largest group
    // [7] opponent's largest group
    private int[] weights = {800, 50, 5, -5, -50, -800, 10, -10, 5, -5};
    private boolean[] seenFree, visited;
    private ArrayList<Field> checkedFree = new ArrayList<Field>(Board.SIZE);
    private int groupSize = 0;
    // This value is set if there exists a row of length 4 and freedom 2
    private int winByForce1 = 0, winByForce2 = 0, totalfreedom = 0;

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
        newBoard.nPieces1 = nPieces1;
        newBoard.nPieces2 = nPieces2;
        newBoard.freeSquares = freeSquares;
        newBoard.firstMove = firstMove;
        newBoard.isEnd = isEnd;
        newBoard.currentPlayer = currentPlayer;
        newBoard.nMoves = nMoves;
        newBoard.winner = winner;
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
            // First move switch
            if (board[pos].occupant == P1) {
                nPieces1--;
            }
            board[pos].occupant = player;
            // If black made a move, we can no longer switch.
            if (player == P2) {
                nPieces2++;
                firstMove = false;
            } else {
                nPieces1++;
            }
            freeSquares--;
            // zobristHash ^= zobristPositions[pos][player - 1];
            moveList.add(pos);
            currentPlayer = getOpponent(currentPlayer);
            // hashCurrentPlayer();
            nMoves++;
            return true;
        } else {
            System.err.println("Something is wrong in doMove.");
            return false;
        }
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        doMove(move.getMove()[0], player);
        if(capturePieces(move.getMove()[0]))
            return true;
        else {
            nMoves--;
            return false;
        }
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
        return moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        int count = (!firstMove) ? freeSquares : REAL_SIZE;
        poMoves.clear();
        int c = 0;
        IMove move;
        // Add the moves from the spiral ordering
        for (int i = 0; i < SIZE; i++) {
            if (board[i] == null)
                continue;
            // Check if position is free and add it to the free moves
            if (firstMove || board[i].occupant == 0) {
                move = new Move(i);
                poMoves.add(move);
                if (heuristics) {
                    // Prefer the highly connected positions
                    if (board[i].numNeighbours > 4)
                        poMoves.add(move);
                }
                c++;
                // No need to look further
                if (c == count) {
                    return poMoves;
                }
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
        isEnd = false;
        int move = moveList.get(moveIndex);
        // return the captured stones
        int[] captures = captureList.get(moveIndex);
        winner = NONE_WIN;
        int color, position;
        for (int capture : captures) {
            color = isBlack(capture) ? P2 : P1;
            position = getPosition(capture);
            playerCaps[color - 1]--;
            board[position].occupant = color;
            //
            if(color == P1)
                nPieces1++;
            else
                nPieces2++;
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
            nPieces1++;
            nPieces2--;
            firstMove = true;
        } else {
            // Remove the stone from the hash
            // zobristHash ^= zobristPositions[move][currentPlayer - 1];
            // Remove the stone from the position
            board[move].occupant = FREE;

            if(currentPlayer == P1)
                nPieces1--;
            else
                nPieces2--;

            freeSquares++;
        }
        firstMove = freeSquares == 61 || (currentPlayer == Board.P2 && freeSquares == 60);
        //
        captureList.remove(moveIndex);
        moveList.remove(moveIndex);
        nMoves--;
    }

    private void capturePositions() {
        // We can start capturing at the lowest index.
        for (int i = firstCaptureI; i < SIZE; i++) {
            if (seen[i]) {
                capturePositions[captureI] = setColor(board[i].position, board[i].occupant == Board.P2);
                captureI++;
                // zobristHash ^= zobristPositions[i][board[i].occupant - 1];
                playerCaps[board[i].occupant - 1]++;
                if(board[i].occupant == P1) {
                    nPieces1--;
                } else {
                    nPieces2--;
                }
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
     * Checks if one of the players has won the game, or the game is a draw.
     *
     * @return NONE = 0, WHITE_WIN = Board.WHITE, BLACK_WIN = Board.BLACK, DRAW = 3;
     */
    @Override
    public int checkWin() {
        winner = NONE_WIN;
        if (moveList.size() == 0)
            return NONE_WIN;
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
                    winner = player;
                    return player;
                }
                currentField = currentField.neighbours[j];
            }
        }
        // There are fewer free squares on the board than needed to build a row
        if (freeSquares == 0) {
            isEnd = true;
            winner = DRAW;
            return DRAW;
        }
        // None of the players win, continue the game
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
        nMoves = 0;
    }

    @Override
    public void newDeterminization(int myPlayer) {
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
        return true;
    }

    @Override
    public double evaluate(int player) {
        // early termination (mcts_pd0 and mcts_pd3) is losing by a lot against vanilla mcts
        // I suspect something is still wrong with this
        //
        seenFree = new boolean[Board.SIZE];
        int minFreeOpp = P_INF, minFreeMe = P_INF, currentFree, count = 0, score = 0;
        int maxRowOpp = 0, maxRowMe = 0, currentMax, maxGroupMe = 0, maxGroupOpp = 0, maxTotalFreeMe = 0, maxTotalFreeOpp = 0;
        boolean isOpp, myTurn;
        score += weights[0] * playerCaps[getOpponent(player) - 1];
        // The number of my pieces captured by the opponent
        score += weights[5] * playerCaps[player - 1];
        // Check minimal freedom, longest rows etc.
        for (int i = 0; i < board.length; i++) {
            // Check if position is part of the board
            if (board[i] == null || board[i].occupant == Board.FREE)
                continue;
            isOpp = board[i].occupant != player;
            myTurn = currentPlayer == board[i].occupant;
            // Check if longest row.
            currentMax = checkRowLength(board[i], currentPlayer == board[i].occupant);
            // Check if row of 4 with 2 freedom
            if (winByForce1 > 0) {
                score = (isOpp) ? -5000 : 5000;
                return score;
            } else if (winByForce2 > 0) {
                // System.out.println("Force move win for: " + board.board[i].occupant);
                // good or bad :)
                score = (isOpp) ? -6000 : 6000;
                return score;
            }
            // Check if row length is higher than current highest
            if (isOpp && currentMax > maxRowOpp) {
                maxRowOpp = currentMax;
            } else if (!isOpp && currentMax > maxRowMe) {
                maxRowMe = currentMax;
            }
            // Check the maximum total freedom in every direction
            if (isOpp && totalfreedom > maxTotalFreeOpp) {
                maxTotalFreeOpp = totalfreedom;
            } else if (!isOpp && totalfreedom > maxTotalFreeMe) {
                maxTotalFreeMe = totalfreedom;
            }
            // Check for minimal freedom.
            checkedFree.clear();
            visited = new boolean[Board.SIZE];
            if (myTurn) // Be pessimistic about group-size if not my turn
                groupSize = 1;
            else
                groupSize = 0;
            //
            assert (board[i] != null);
            currentFree = checkFreedom(board[i], 0);
            for (Field f : checkedFree) {
                f.freedom = currentFree;
            }
            // Check the largest group
            if (isOpp && groupSize > maxGroupOpp) {
                maxGroupOpp = groupSize;
            } else if (!isOpp && groupSize > maxGroupMe) {
                maxGroupMe = groupSize;
            }
            // There should be at least two pieces on the board or no use to compare freedom
            if (Board.REAL_SIZE - freeSquares > 2) {
                // Check if freedom is lower than current lowest.
                if (isOpp && currentFree < minFreeOpp) {
                    minFreeOpp = currentFree;
                } else if (!isOpp && currentFree < minFreeMe) {
                    minFreeMe = currentFree;
                }
            }
            count++;
            if (count == Board.REAL_SIZE - freeSquares)
                break;
        }
        // Final scoring
        score += weights[1] * maxRowMe;
        score += weights[2] * minFreeMe;
        score += weights[3] * minFreeOpp;
        score += weights[4] * maxRowOpp;
        score += weights[8] * maxTotalFreeMe;
        score += weights[9] * maxTotalFreeOpp;

        double score_nt = FastTanh.tanh(score / 1000.0);
        return score_nt;
    }

    @Override
    public double getQuality() {
        if (winner == P1_WIN)
            return ((ROW_SIZE - getRowScore(P2)) / (double) ROW_SIZE);
        else if (winner == P2_WIN)
            return ((ROW_SIZE - getRowScore(P1)) / (double) ROW_SIZE);
        return 1;
    }

    public double getRowScore(int player) {
        double currentMax, maxRow = -10;
        // Check minimal freedom, longest rows etc.
        for (int i = 0; i < board.length; i++) {
            // Check if position is part of the board
            if (board[i] == null || board[i].occupant != player)
                continue;
            // Check if longest row.
            currentMax = checkRowLength(board[i], false);
            // Check if row length is higher than current highest
            if (currentMax > maxRow) {
                maxRow = currentMax;
            }
        }
        return maxRow;
    }

    /**
     * Set/get the freedom of a field
     *
     * @param f The current field
     * @return The freedom of the field
     */
    private int checkFreedom(Field f, int current) {
        // This field was checked before, return its freedom.
        if (seenFree[f.position])
            return f.freedom;

        visited[f.position] = true;
        //
        Field[] nb = f.neighbours;
        for (Field n : nb) {
            if (n == null)
                continue;
            // For each free neighbor increase the current freedom.
            if (n.occupant == Board.FREE && !visited[n.position]) {
                current++;
                // Count each free position only once!
                visited[n.position] = true;
            } else if (n.occupant == f.occupant && !visited[n.position]) {
                // Check similarly occupied neighbors
                groupSize++;
                current = checkFreedom(n, current);
                checkedFree.add(n);
            }
        }
        seenFree[f.position] = true;
        return current;
    }

    /**
     * Check the longest row that this field is part of
     *
     * @param f The current field
     * @return The length of the longest row
     */
    private int checkRowLength(Field f, boolean myTurn) {
        winByForce1 = 0;
        winByForce2 = 0;
        totalfreedom = 0;
        //
        opp = (f.occupant == Board.P2) ? Board.P1 : Board.P2;
        // Each row is of at least length 1
        for (int i = 0; i < rowLength.length; i++) {
            // The current stone
            rowLength[i] = 1;
            closedrow[i] = true;
            extrafreedom[i] = false;
            //
            freedom[i] = 0;
            freedom[i + 3] = 0;
            totFreedom[i] = 0;
            totFreedom[i + 3] = 0;
        }
        longestRow = 0;
        boolean prevFree, rowfinished;
        Field currentField;
        // Check once in each direction.
        for (int j = 0; j < Board.NUM_NEIGHBOURS; j++) {
            prevFree = false;
            rowfinished = false;
            currentField = f.neighbours[j];
            // If we've already seen this position, the current row will not be longer than when
            // we saw it before.
            if (currentField == null)
                continue;

            // Check for a row of 5 in each direction.
            while (currentField != null && currentField.occupant != opp) {
                if (!rowfinished) {
                    // Is part of the row, increase
                    if (currentField.occupant == f.occupant) {
                        if (prevFree) {
                            closedrow[j % 3] = false; // a gap
                        } else {
                            prevFree = false;
                            rowLength[j % 3]++;
                        }
                    } else if (!prevFree) {
                        totalfreedom++;
                        // The row has some freedom in this direction
                        prevFree = true;
                        freedom[j]++;
                        totFreedom[j]++;
                    } else if (prevFree) {
                        extrafreedom[j % 3] = true;
                        // Two free squares == no longer part of a row
                        rowfinished = true;
                        totalfreedom++;
                        totFreedom[j]++;
                        // Total freedom is not considered later in the game
                        if (totFreedom[j % 3] + rowLength[j % 3] >= Board.ROW_SIZE)
                            break;
                    }
                } else {
                    // Keep counting the free squares in this direction
                    totalfreedom++;
                    totFreedom[j]++;
                }
                //
                currentField = currentField.neighbours[j];
            }
        }
        //
        int longestrowi = -1;
        for (int i = 0; i < rowLength.length; i++) {
            // Check for the longest row, only if it can be extended to a row of 5
            if (rowLength[i] > longestRow
                    && rowLength[i] + totFreedom[i] + totFreedom[i + 3] >= Board.ROW_SIZE) {
                longestRow = rowLength[i];
                longestrowi = i;
            }
        }
        // If not player's turn, be pessimistic about freedom
        if (!myTurn) {
            if (longestrowi >= 0) {
                // Assume the opponent will block the longest row
                freedom[longestrowi]--;
                // Assume the opponent will cut of row with most freedom
                if (totFreedom[longestrowi] > totFreedom[longestrowi + 3]) {
                    totalfreedom -= totFreedom[longestrowi];
                    totFreedom[longestrowi] = 0;
                } else {
                    totalfreedom -= totFreedom[longestrowi + 3];
                    totFreedom[longestrowi + 3] = 0;
                }
                // Re-check if still the longest row.
                for (int i = 0; i < rowLength.length; i++) {
                    // Check for the longest row
                    if (rowLength[i] > longestRow
                            && rowLength[i] + totFreedom[i] + totFreedom[i + 3] >= Board.ROW_SIZE) {
                        longestRow = rowLength[i];
                    }
                }
            }
        }
        for (int i = 0; i < rowLength.length; i++) {
            // This condition always leads to a win, closed row of 4, freedom on both sides
            // Or, if myTurn, closed row of three with freedom on both sides, and one extra freedom.
            if (rowLength[i] == 4 && freedom[i] == 2 && closedrow[i]) {
                winByForce1 = f.occupant;
                return longestRow;
            } else if (myTurn && rowLength[i] == 3 && freedom[i] == 2 && closedrow[i]
                    && extrafreedom[i]) {
                winByForce2 = f.occupant;
            } else if (myTurn && rowLength[i] == 4 && freedom[i] >= 1 && closedrow[i]) {
                winByForce2 = f.occupant;
            }
        }
        return longestRow;
    }
}
