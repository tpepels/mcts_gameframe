package amazons.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Board implements IBoard {
    // The board size
    public static final int SIZE = 10;
    // Board occupants
    public static final int EMPTY = 0, WHITE_Q = P1, BLACK_Q = P2, ARROW = 3;
    private static final MoveList moves = new MoveList(5000);
    private static final ArrayList<IMove> playoutMoves = new ArrayList<IMove>();
    // Initial queen positions
    public final int[][] POSITIONS = {{60, 69, 93, 96}, {3, 6, 30, 39}};
    // Board is public for fast access
    public final int[] board;
    private final int[] ALL_MOVE_INT = {11, -11, 9, -9, 10, -10, -1, 1};
    private final int[] possibleMoves = new int[40], possibleShots = new int[40];
    private final Stack<IMove> pastMoves = new Stack<IMove>();
    private int max;
    private int min;
    private int col;
    private int row;
    private int direction;
    private int count;
    private int position;
    private int winner = NONE_WIN;
    private int lastFrom, lastTo, currentPlayer;

    /**
     * Initialise the board using the default size (10)
     */
    public Board() {
        board = new int[SIZE * SIZE];

        currentPlayer = P1;
        winner = NONE_WIN;
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
        System.arraycopy(POSITIONS[0], 0, newBoard.POSITIONS[0], 0,
                POSITIONS[0].length);
        System.arraycopy(POSITIONS[1], 0, newBoard.POSITIONS[1], 0,
                POSITIONS[1].length);
        newBoard.winner = winner;
        newBoard.currentPlayer = currentPlayer;
        return newBoard;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        board[move.getMove()[1]] = board[move.getMove()[0]];
        board[move.getMove()[0]] = EMPTY;
        POSITIONS[player - 1][board[move.getMove()[1]] % 10] = move.getMove()[1];
        // Shoot the arrow
        board[move.getType()] = ARROW;
        //
        pastMoves.push(move);
        currentPlayer = getOpponent(currentPlayer);
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        moves.clear();
        for (int i = 0; i < POSITIONS[currentPlayer - 1].length; i++) {
            // Select the location to move from, ie the queen to move
            int from = POSITIONS[currentPlayer - 1][i];
            int moveCount = getPossibleMovesFrom(from, possibleMoves);
            // Move count holds the possible number of moves possible from this position
            for (int j = 0; j < moveCount; j++) {
                moveQueen(from, possibleMoves[j]);
                // Iterate through the possible shots
                int shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                for (int k = 0; k < shotCount; k++) {
                    moves.add(new Move(from, possibleMoves[j], possibleShots[k]));
                }
                undoQueenMove();
            }
        }
        return moves.copy();
    }

    private void undoQueenMove() {
        board[lastFrom] = board[lastTo];
        board[lastTo] = EMPTY;
        POSITIONS[currentPlayer - 1][board[lastFrom] % 10] = lastFrom;
    }

    private void moveQueen(int from, int to) {
        // Store the move, so it can be undone later
        lastFrom = from;
        lastTo = to;
        //
        board[to] = board[from];
        board[from] = EMPTY;
        POSITIONS[currentPlayer - 1][board[to] % 10] = to;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        playoutMoves.clear();
        for (int i = 0; i < POSITIONS[currentPlayer - 1].length; i++) {
            // Select the location to move from, ie the queen to move
            int from = POSITIONS[currentPlayer - 1][i];
            int moveCount = getPossibleMovesFrom(from, possibleMoves);
            // Move count holds the possible number of moves possible from this position
            for (int j = 0; j < moveCount; j++) {
                moveQueen(from, possibleMoves[j]);
                // Iterate through the possible shots
                int shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                for (int k = 0; k < shotCount; k++) {
                    playoutMoves.add(new Move(from, possibleMoves[j], possibleShots[k]));
                }
                undoQueenMove();
            }
        }
        return playoutMoves;
    }

    @Override
    public void undoMove() {
        IMove move = pastMoves.pop();
        if (move != null) {
            currentPlayer = getOpponent(currentPlayer);
            //
            board[move.getMove()[0]] = board[move.getMove()[1]];
            board[move.getMove()[1]] = EMPTY;
            POSITIONS[currentPlayer - 1][board[move.getMove()[0]] % 10] = move.getMove()[0];
            // clear the arrow
            board[move.getType()] = EMPTY;
        }
    }

    @Override
    public int getOpponent(int player) {
        return (player == P1) ? P2 : P1;
    }

    @Override
    public int checkWin() {
        boolean[] can = {false, false};
        for (int i = P1; i <= P2; i++) {
            for (int j = 0; j < POSITIONS[i - 1].length; j++) {
                // Check if player can make a move from position.
                if (canMakeMoveFrom(POSITIONS[i - 1][j])) {
                    can[i - 1] = true;
                    break;
                }
            }
        }
        if (!can[0])
            return P2_WIN;
        if (!can[1])
            return P1_WIN;
        else
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
        return 0;
    }

    @Override
    public void initialize() {
        for (int i = 0; i < board.length; i++) {
            board[i] = EMPTY;
        }
        // Setup initial positions
        for (int i = 0; i < POSITIONS[0].length; i++) {
            board[POSITIONS[0][i]] = WHITE_Q * 10 + i;
            board[POSITIONS[1][i]] = BLACK_Q * 10 + i;
        }
    }

    /**
     * Checks if move from to to is possible
     *
     * @param from    Move from
     * @param to      Move to
     * @param skippos Treat this position as empty
     * @return true if the move is possible
     */
    private boolean checkMove(int from, int to, int skippos) {
        count = 0;
        for (int i = 0; i < ALL_MOVE_INT.length; i++) {
            // Select a random direction.
            direction = ALL_MOVE_INT[i];
            col = from % 10;
            row = from / 10;

            if (direction == -11) {
                min = from + (Math.min(col, row) * direction);
                max = 99;
            } else if (direction == 11) {
                col = 9 - col;
                row = 9 - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == 9) {
                row = 9 - row;
                max = from + (Math.min(col, row) * direction);
            } else if (direction == -9) {
                col = 9 - col;
                min = from + (Math.min(col, row) * direction);
                max = 100;
            } else if (direction == 10 || direction == -10) {
                max = 99;
                min = 0;
            } else {
                max = row * 10 + 9;
                min = row * 10;
            }

            position = from + direction;
            // Select a random position along the chosen direction
            while (position <= max && position >= min
                    && (board[position] == Board.EMPTY || skippos == position)) {
                //
                if (position == to) {
                    System.out.println("Move ok!, direction: " + direction);
                    return true;
                }
                position += direction;
            }
        }
        return false;
    }

    public int getPossibleMovesFrom(int from, int[] moves) {
        count = 0;
        for (int i = 0; i < ALL_MOVE_INT.length; i++) {
            // Select a random direction.
            direction = ALL_MOVE_INT[i];
            col = from % 10;
            row = from / 10;
            //
            if (direction == -11) {
                min = from + (Math.min(col, row) * direction);
                max = 99;
            } else if (direction == 11) {
                col = 9 - col;
                row = 9 - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == 9) {
                row = 9 - row;
                max = from + (Math.min(col, row) * direction);
            } else if (direction == -9) {
                col = 9 - col;
                min = from + (Math.min(col, row) * direction);
                max = 100;
            } else if (direction == 10 || direction == -10) {
                max = 99;
                min = 0;
            } else {
                max = row * 10 + 9;
                min = row * 10;
            }

            position = from + direction;
            // Select a random position along the chosen direction
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
        for (int i = 0; i < ALL_MOVE_INT.length; i++) {
            // Select a random direction.
            direction = ALL_MOVE_INT[i];
            col = from % 10;
            row = from / 10;
            //
            if (direction == -11) {
                min = from + (Math.min(col, row) * direction);
                max = 99;
            } else if (direction == 11) {
                col = 9 - col;
                row = 9 - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == 9) {
                row = 9 - row;
                max = from + (Math.min(col, row) * direction);
            } else if (direction == -9) {
                col = 9 - col;
                min = from + (Math.min(col, row) * direction);
                max = 100;
            } else if (direction == 10 || direction == -10) {
                max = 99;
                min = 0;
            } else {
                max = row * 10 + 9;
                min = row * 10;
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
        int side = board[moveFrom] / 10; // This is the number of the player or  arrow.
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
                POSITIONS[side - 1][queenId] = moveTo;
                currentPlayer = getOpponent(currentPlayer);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public int getWinner() {
        return winner;
    }
}
