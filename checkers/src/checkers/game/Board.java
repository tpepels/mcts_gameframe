package checkers.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.*;

public class Board implements IBoard {
    public final static int EMPTY = 0, W_PIECE = P1, B_PIECE = P2, W_KING = W_PIECE * 10, B_KING = B_PIECE * 10, MAX_MOVES = 250;
    public final static MoveList slideMoves = new MoveList(1000), jumpMoves = new MoveList(1000);
    private final static long[] seen = new long[64];
    private static long seenI = Long.MIN_VALUE;
    private final Stack<Move> pastMoves = new Stack<Move>();
    private final static List<Integer> captures = new ArrayList<Integer>();
    private final static int[] n = {-1, 1}, p1n = {-1}, p2n = {1};
    //
    public int[][] board = new int[8][8];
    private int nMoves = 0, currentPlayer = P1, nPieces1 = 12, nPieces2 = 12, winner = NONE_WIN;

    @Override
    public boolean doAIMove(IMove move, int player) {
        int fromX = move.getMove()[0], fromY = move.getMove()[1];
        int toX = move.getMove()[2], toY = move.getMove()[3];
        // Move the piece
        int piece = board[fromY][fromX];
        board[fromY][fromX] = Board.EMPTY;
        board[toY][toX] = piece;
        // Check for captures
        Move mv = (Move) move;
        if (mv.getCaptures() != null) {
            for (int i : mv.getCaptures()) {
                if (i > 100)
                    i /= 100;
                board[i / 8][i % 8] = Board.EMPTY;
                //
                if (player == P1)
                    nPieces2--;
                else
                    nPieces1--;
                // System.out.println("1 " + nPieces1 + " 2 " + nPieces2);
            }
        }
        // Check for promotion
        if (player == P1 && toY == 0 && board[toY][toX] < 10) {
            board[toY][toX] = W_KING;
            mv.promotion = true;
        }
        if (player == P2 && toY == 7 && board[toY][toX] < 10) {
            board[toY][toX] = B_KING;
            mv.promotion = true;
        }

        pastMoves.push(mv);
        currentPlayer = getOpponent(currentPlayer);
        nMoves++;
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        jumpMoves.clear();
        slideMoves.clear();
        int piece;
        boolean captureFound = false;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                piece = board[i][j];
                if (piece == currentPlayer || piece == (10 * currentPlayer)) {
                    seenI++;
                    captures.clear();
                    if (generateMovesForPiece(j, i, j, i, currentPlayer, 0, piece == (10 * currentPlayer), captureFound)) {
                        captureFound = true;
                    }
                }
            }
        }
        if (captureFound)
            return jumpMoves;
        else
            return slideMoves;
    }

    public static int[] convertIntegers(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }

    private boolean inBounds(int y, int x) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        return Arrays.asList(getExpandMoves().getArrayCopy());
    }

    private boolean generateMovesForPiece(int initX, int initY, int x, int y, int colour, int hops, boolean king, boolean captureOnly) {
        int[] dir;
        if (!king)
            dir = (colour == W_PIECE) ? p1n : p2n;
        else
            dir = n;
        boolean hopMove = false;
        int location;
        int opp = getOpponent(colour);
        for (int j : dir) {
            for (int k : n) {
                location = (8 * (y + j)) + (x + k);
                if (inBounds(y + j, x + k)) {
                    if (!captureOnly && hops == 0 && board[y + j][x + k] == Board.EMPTY) {
                        //
                        slideMoves.add(new Move(new int[]{x, y, x + k, y + j}, null));
                    } else if (seen[location] != seenI &&
                            (board[y + j][x + k] == opp || board[y + j][x + k] == (opp * 10)) &&
                            inBounds(y + (j * 2), x + (k * 2)) &&
                            board[y + (j * 2)][x + (k * 2)] == EMPTY) {

                        seen[location] = seenI;
                        hopMove = true;

                        // Encode the king
                        if (board[y + j][x + k] == (opp * 10))
                            location *= 100;

                        captures.add(new Integer(location));
                        //
                        if (!generateMovesForPiece(initX, initY, x + (k * 2), y + (j * 2), colour, hops + 1, king, captureOnly)) {
                            // Add accordingly to the number of captures
                            jumpMoves.add(new Move(new int[]{initX, initY, x + (k * 2), y + (j * 2)}, convertIntegers(captures)));
                        }
                        captures.remove(captures.size() - 1);
                    }
                }
            }
        }
        return hopMove;
    }

    @Override
    public void undoMove() {
        Move move = pastMoves.pop();
        currentPlayer = getOpponent(currentPlayer);
        //
        int fromX = move.getMove()[0], fromY = move.getMove()[1];
        int toX = move.getMove()[2], toY = move.getMove()[3];
        // Move the piece
        int piece = board[toY][toX];
        board[fromY][fromX] = piece;
        board[toY][toX] = Board.EMPTY;
        // Check for captures
        if (move.getCaptures() != null) {
            for (int i : move.getCaptures()) {
                // Replace the correct piece
                if (currentPlayer == P1) {
                    piece = B_PIECE;
                    nPieces2++;
                } else {
                    piece = W_PIECE;
                    nPieces1++;
                }
                // The captured piece was a king
                if (i > 100) {
                    piece *= 10;
                    i /= 100;
                }
                // Replace the piece on the board
                board[i / 8][i % 8] = piece;
            }
        }
        // Check for promotion (demotion)
        if (move.promotion && currentPlayer == P1)
            board[fromY][fromX] = W_PIECE;
        if (move.promotion && currentPlayer == P2)
            board[fromY][fromX] = B_PIECE;

        nMoves--;
        winner = NONE_WIN;
    }

    @Override
    public int getOpponent(int player) {
        return 3 - player;
    }

    @Override
    public int checkWin() {
        if (nMoves == MAX_MOVES)
            return DRAW;
        if (nPieces1 == 0)
            winner = P2_WIN;
        else if (nPieces2 == 0)
            winner = P1_WIN;
        else
            winner = NONE_WIN;
        return winner;
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
        return 64 + (100 * 64);
    }

    @Override
    public void initialize() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = EMPTY;
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 1; j <= 8; j += 2) {
                if (i % 2 == 0) {
                    board[i][j] = B_PIECE;
                    board[7 - i][j - 1] = W_PIECE;
                } else {
                    board[i][j - 1] = B_PIECE;
                    board[7 - i][j] = W_PIECE;
                }
            }
        }

        nPieces1 = 12;
        nPieces2 = 12;
        currentPlayer = P1;
        winner = NONE_WIN;
    }

    public double evaluate(int player) {
        return 0;
    }

    @Override
    public double getQuality() {
        if (winner == P1)
            return nPieces1 / 12.;
        else
            return nPieces2 / 12.;
    }

    @Override
    public void newDeterminization(int myPlayer) {
        // Fully observable
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

    @Override
    public IBoard copy() {
        Board newBoard = new Board();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                newBoard.board[i][j] = board[i][j];
            }
        }

        newBoard.currentPlayer = currentPlayer;
        newBoard.nPieces1 = nPieces1;
        newBoard.nPieces2 = nPieces2;
        newBoard.winner = winner;
        newBoard.nMoves = nMoves;

        return newBoard;
    }
}
