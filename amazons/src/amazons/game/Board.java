package amazons.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Board implements IBoard {
    // The board size
    public static final int SIZE = 8, B_SIZE = SIZE * SIZE, N_QUEENS = 4;
    // Board occupants
    public static final int EMPTY = 0, WHITE_Q = P1, BLACK_Q = P2, ARROW = 3;
    private static final Random r = new Random();
    private static final MoveList moves = new MoveList(5000);
    private static final ArrayList<IMove> playoutMoves = new ArrayList<IMove>();
    // Initial queen positions
    private static final int[][] initPositions = {{58, 61, 40, 47}, {2, 5, 16, 23}};
    public final int[][] queens = new int[2][4];
    // Board is public for fast access
    public final int[] board;
    private final int[] ALL_MOVE_INT = {9, -9, 7, -7, 8, -8, -1, 1};
    private final int[] possibleMoves = new int[40], possibleShots = new int[40];
    private final Stack<IMove> pastMoves = new Stack<IMove>();
    private int nMoves = 0;
    private int lastFrom, lastTo, currentPlayer;
    private int winner = NONE_WIN;

    /**
     * Initialise the board using the default size
     */
    public Board() {
        board = new int[SIZE * SIZE];
        currentPlayer = P1;
    }

    @Override
    public void newDeterminization(int myPlayer) {
        // Game is fully observable
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
    public boolean drawPossible() {
        return false;
    }

    /**
     * Copy the current board to a new board class.
     *
     * @return A new copied board.
     */
    public Board copy() {
        Board newBoard = new Board();
        // Copy the board data
        System.arraycopy(board, 0, newBoard.board, 0, board.length);
        System.arraycopy(queens[0], 0, newBoard.queens[0], 0,
                queens[0].length);
        System.arraycopy(queens[1], 0, newBoard.queens[1], 0,
                queens[1].length);
        newBoard.currentPlayer = currentPlayer;
        newBoard.nMoves = nMoves;
        newBoard.winner = winner;
        return newBoard;
    }

    @Override
    public MoveList getExpandMoves() {
        moves.clear();
        int from, moveCount, shotCount;
        for (int i = 0; i < queens[currentPlayer - 1].length; i++) {
            // Select the location to move from, ie the queen to move
            from = queens[currentPlayer - 1][i];
            moveCount = getPossibleMovesFrom(from, possibleMoves);
            // Move count holds the possible number of moves possible from this position
            for (int j = 0; j < moveCount; j++) {
                moveQueen(from, possibleMoves[j], currentPlayer);
                // Iterate through the possible shots
                shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                for (int k = 0; k < shotCount; k++) {
                    moves.add(new Move(from, possibleMoves[j], possibleShots[k]));
                }
                undoQueenMove(currentPlayer);
            }
        }
        return moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        playoutMoves.clear();
        int from, moveCount, shotCount;
        if (heuristics) {
            int start = r.nextInt(N_QUEENS);
            int c = 0;
            while (playoutMoves.isEmpty() && c < N_QUEENS) {
                // Select the location to move from, ie the queen to move
                from = queens[currentPlayer - 1][start];
                moveCount = getPossibleMovesFrom(from, possibleMoves);
                // Move count holds the possible number of moves possible from this position
                for (int j = 0; j < moveCount; j++) {
                    moveQueen(from, possibleMoves[j], currentPlayer);
                    // Iterate through the possible shots
                    shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                    for (int k = 0; k < shotCount; k++) {
                        playoutMoves.add(new Move(from, possibleMoves[j], possibleShots[k]));
                    }
                    undoQueenMove(currentPlayer);
                }
                // Next queen, in case of no moves
                start = (start == N_QUEENS - 1) ? 0 : start + 1;
                c++;
            }
        }
        //
        if (playoutMoves.isEmpty()) {
            for (int i = 0; i < queens[currentPlayer - 1].length; i++) {
                // Select the location to move from, ie the queen to move
                from = queens[currentPlayer - 1][i];
                moveCount = getPossibleMovesFrom(from, possibleMoves);
                // Move count holds the possible number of moves possible from this position
                for (int j = 0; j < moveCount; j++) {
                    moveQueen(from, possibleMoves[j], currentPlayer);
                    // Iterate through the possible shots
                    shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                    for (int k = 0; k < shotCount; k++) {
                        playoutMoves.add(new Move(from, possibleMoves[j], possibleShots[k]));
                    }
                    undoQueenMove(currentPlayer);
                }
            }
        }
        return playoutMoves;
    }

    @Override
    public void undoMove() {
        IMove move = pastMoves.pop();
        if (move != null) {
            currentPlayer = getOpponent(currentPlayer);
            // clear the arrow (this has to be done before replacing the queen!)
            board[move.getType()] = EMPTY;
            // Replace the queen
            board[move.getMove()[0]] = board[move.getMove()[1]];
            board[move.getMove()[1]] = EMPTY;
            queens[currentPlayer - 1][board[move.getMove()[0]] % 10] = move.getMove()[0];
            nMoves--;
            winner = NONE_WIN;
        }
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        board[move.getMove()[1]] = board[move.getMove()[0]];
        board[move.getMove()[0]] = EMPTY;
        queens[currentPlayer - 1][board[move.getMove()[1]] % 10] = move.getMove()[1];
        // Shoot the arrow (after moving the queen!)
        board[move.getType()] = ARROW;
        //
        pastMoves.push(move);
        currentPlayer = getOpponent(currentPlayer);
        nMoves++;
        return true;
    }

    private void undoQueenMove(int player) {
        //
        board[lastFrom] = board[lastTo];
        board[lastTo] = EMPTY;
        queens[player - 1][board[lastFrom] % 10] = lastFrom;
    }

    private void moveQueen(int from, int to, int player) {
        // Store the move, so it can be undone later
        lastFrom = from;
        lastTo = to;
        //
        board[to] = board[from];
        board[from] = EMPTY;
        queens[player - 1][board[to] % 10] = to;
    }

    @Override
    public int getOpponent(int player) {
        return (player == P1) ? P2 : P1;
    }

    @Override
    public int checkWin() {
        boolean[] can = {false, false};
        for (int i = P1; i <= P2; i++) {
            for (int j = 0; j < queens[i - 1].length; j++) {
                // Check if player can make a move from position.
                if (canMakeMoveFrom(queens[i - 1][j])) {
                    can[i - 1] = true;
                    break;
                }
            }
        }
        if (!can[0]) {
            winner = P2_WIN;
            return P2_WIN;
        }
        if (!can[1]) {
            winner = P1_WIN;
            return P1_WIN;
        }
        winner = NONE_WIN;
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
        return 646465;
    }

    @Override
    public void initialize() {
        nMoves = 0;
        for (int i = 0; i < board.length; i++) {
            board[i] = EMPTY;
        }
        // Setup initial positions
        for (int i = 0; i < initPositions[0].length; i++) {
            board[initPositions[0][i]] = WHITE_Q * 10 + i;
            queens[0][i] = initPositions[0][i];
            board[initPositions[1][i]] = BLACK_Q * 10 + i;
            queens[1][i] = initPositions[1][i];
        }
    }

    @Override
    public double evaluate(int player) {
        return 0.0;
    }

    @Override
    public double getQuality() {
        double count = getFreedom(winner);
        // The more available moves the winning player has, the better
        return count / (N_QUEENS * 16.);
    }

    private int getFreedom(int player) {
        int from, moveCount, shotCount, total = 0;
        for (int i = 0; i < queens[player - 1].length; i++) {
            // Select the location to move from, ie the queen to move
            from = queens[player - 1][i];
            moveCount = getPossibleMovesFrom(from, possibleMoves);
            // Move count holds the possible number of moves possible from this position
            for (int j = 0; j < moveCount; j++) {
                moveQueen(from, possibleMoves[j], player);
                // Iterate through the possible shots
                shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                for (int k = 0; k < shotCount; k++) {
                    total++;
                }
                undoQueenMove(currentPlayer);
            }
        }
        return total;
    }

    public int getPossibleMovesFrom(int from, int[] moves) {
        int count = 0, position;
        int col, row, direction, min, max;
        for (int i = 0; i < ALL_MOVE_INT.length; i++) {
            // Select a random direction.
            direction = ALL_MOVE_INT[i];
            col = from % SIZE;
            row = from / SIZE;
            //
            if (direction == -(SIZE + 1)) {
                min = from + (Math.min(col, row) * direction);
                max = (B_SIZE - 1);
            } else if (direction == (SIZE + 1)) {
                col = (SIZE - 1) - col;
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == (SIZE - 1)) {
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == -(SIZE - 1)) {
                col = (SIZE - 1) - col;
                min = from + (Math.min(col, row) * direction);
                max = B_SIZE;
            } else if (direction == SIZE || direction == -SIZE) {
                max = (B_SIZE - 1);
                min = 0;
            } else {
                max = row * SIZE + (SIZE - 1);
                min = row * SIZE;
            }

            position = from + direction;
            // Select a position along the chosen direction
            while (position <= max && position >= min
                    && board[position] == Board.EMPTY) {
                //
                moves[count] = position;
                count++;
                position += direction;
            }
        }
        // Returns the number of moves found
        return count;
    }

    private boolean canMakeMoveFrom(int from) {
        int col, row, direction, min, max, position;
        for (int i = 0; i < ALL_MOVE_INT.length; i++) {
            // Select a random direction.
            direction = ALL_MOVE_INT[i];
            col = from % SIZE;
            row = from / SIZE;
            //
            if (direction == -(SIZE + 1)) {
                min = from + (Math.min(col, row) * direction);
                max = (B_SIZE - 1);
            } else if (direction == (SIZE + 1)) {
                col = (SIZE - 1) - col;
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == (SIZE - 1)) {
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == -(SIZE - 1)) {
                col = (SIZE - 1) - col;
                min = from + (Math.min(col, row) * direction);
                max = B_SIZE;
            } else if (direction == SIZE || direction == -SIZE) {
                max = (B_SIZE - 1);
                min = 0;
            } else {
                max = row * SIZE + (SIZE - 1);
                min = row * SIZE;
            }

            position = from + direction;
            // Select a position along the chosen direction
            if (position <= max && position >= min
                    && board[position] == Board.EMPTY) {
                //
                return true;
            }
        }
        return false;
    }

    public boolean humanMove(int moveFrom, int moveTo, int shootTo) {
        int side = board[moveFrom] / SIZE; // This is the number of the player or  arrow.
        int queenId = board[moveFrom] % 10; // This is the id of the queen
        //
        // Simple checks for bounds and emptyness of target squares
        if (moveFrom >= 0 && moveFrom < SIZE * SIZE && moveTo >= 0
                && moveTo < SIZE * SIZE && shootTo >= 0
                && shootTo < SIZE * SIZE && shootTo != moveTo
                && side == currentPlayer && board[moveTo] == EMPTY
                && (board[shootTo] == EMPTY || shootTo == moveFrom)) {
            if (checkMove(moveFrom, moveTo, -1)
                    && checkMove(moveTo, shootTo, moveFrom)) {
                System.out.println("Move from: " + moveFrom);
                System.out.println("Move to: " + moveTo);
                System.out.println("Shoot to: " + shootTo);
                // Move the queen
                board[moveTo] = board[moveFrom];
                board[moveFrom] = EMPTY;
                // Place the arrow
                board[shootTo] = ARROW;
                // Remember the position of the queen.
                queens[side - 1][queenId] = moveTo;
                currentPlayer = getOpponent(currentPlayer);
                nMoves++;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Checks if move from to to is possible, only used by humanMove method
     *
     * @param from    Move from
     * @param to      Move to
     * @param skipPos Treat this position as empty
     * @return true if the move is possible
     */
    private boolean checkMove(int from, int to, int skipPos) {
        int col, row, direction, min, max, position;
        for (int i = 0; i < ALL_MOVE_INT.length; i++) {
            // Select a random direction.
            direction = ALL_MOVE_INT[i];
            col = from % SIZE;
            row = from / SIZE;

            if (direction == -(SIZE + 1)) {
                min = from + (Math.min(col, row) * direction);
                max = B_SIZE - 1;
            } else if (direction == (SIZE + 1)) {
                col = (SIZE - 1) - col;
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == (SIZE - 1)) {
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == -(SIZE - 1)) {
                col = (SIZE - 1) - col;
                min = from + (Math.min(col, row) * direction);
                max = B_SIZE;
            } else if (direction == SIZE || direction == -SIZE) {
                max = B_SIZE - 1;
                min = 0;
            } else {
                max = row * SIZE + (SIZE - 1);
                min = row * SIZE;
            }

            position = from + direction;
            // Select a position along the chosen direction
            while (position <= max && position >= min
                    && (board[position] == Board.EMPTY || skipPos == position)) {
                //
                if (position == to) {
                    return true;
                }
                position += direction;
            }
        }
        return false;
    }
}
