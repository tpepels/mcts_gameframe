package phantomdomineering.game;

import ai.MCTSOptions;
import framework.FiniteBoard;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.IntHashMap;
import framework.util.StatCounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board implements FiniteBoard {
    private static final int EMPTY = 0;
    private static final ArrayList<IMove> poMoves = new ArrayList<>(1000);
    private static final MoveList static_moves = new MoveList(1000);
    private static final IntHashMap<String> impossibles = new IntHashMap<>();
    //
    private final int[][] board;
    private final int size;
    private final List<IMove>[] blocked;
    private int nMoves, currentPlayer;
    public int lastMoveBy;

    public Board(int size) {
        this.size = size;
        this.board = new int[size][size];
        this.nMoves = 0;
        this.blocked = new ArrayList[2];
        this.lastMoveBy = 0;
    }

    @Override
    public IBoard copy() {
        Board newBoard = new Board(size);
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                newBoard.board[i][j] = board[i][j];

        newBoard.blocked[0] = new ArrayList<>(blocked[0]);
        newBoard.blocked[1] = new ArrayList<>(blocked[1]);
        newBoard.nMoves = nMoves;
        newBoard.currentPlayer = currentPlayer;
        newBoard.lastMoveBy = lastMoveBy;
        return newBoard;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        int x1 = move.getMove()[0], x2 = move.getMove()[2];
        int y1 = move.getMove()[1], y2 = move.getMove()[3];
        if (board[y1][x1] == EMPTY && board[y2][x2] == EMPTY) {
            board[y1][x1] = currentPlayer;
            board[y2][x2] = currentPlayer;
            nMoves++;
            lastMoveBy = currentPlayer;
            currentPlayer = getOpponent(currentPlayer);
        } else {
            // Remember that this move is not possible
            blocked[currentPlayer - 1].add(move);
        }
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        static_moves.clear();
        Move m;
        // Returns all moves in view of the current player.
        // This method assumes that the opponent's stones are not visible to the player
        // This method should be called every time a node is visited to determine whether new nodes
        // can be added to the tree.
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m = null;
                if (board[i][j] != currentPlayer) {
                    if (currentPlayer == P1) {
                        if (i + 1 < size && board[i + 1][j] != currentPlayer)
                            m = new Move(j, i, j, i + 1);
                    } else if (currentPlayer == P2) {
                        if (j + 1 < size && board[i][j + 1] != currentPlayer)
                            m = new Move(j, i, j + 1, i);
                    }
                    // Add only the possible moves in view of my player
                    if (m != null && !blocked[currentPlayer - 1].contains(m))
                        static_moves.add(m);
                }
            }
        }
        return static_moves.copy();
    }

    private final static List<IMove> allMoves = new ArrayList<>();
    public List<IMove> getMoves(int player) {
        allMoves.clear();
        IMove m;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m = null;
                if (board[i][j] == EMPTY) {
                    if (player == P1) {
                        if (i + 1 < size && board[i + 1][j] == EMPTY)
                            m = new Move(j, i, j, i + 1);
                    } else if (player == P2) {
                        if (j + 1 < size && board[i][j + 1] == EMPTY)
                            m = new Move(j, i, j + 1, i);
                    }
                    if (m != null)
                        allMoves.add(m);
                }
            }
        }
        return allMoves;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        poMoves.clear();
        IMove m, maxMove = null;
        int x1, y1, x2, y2, maxMoves = -size * size - 1, nMoves;
        boolean greedy = false;
        if (heuristics)
            greedy = MCTSOptions.r.nextInt() < .9;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m = null;
                if (board[i][j] == EMPTY) {
                    if (currentPlayer == P1) {
                        if (i + 1 < size && board[i + 1][j] == EMPTY)
                            m = new Move(j, i, j, i + 1);
                    } else if (currentPlayer == P2) {
                        if (j + 1 < size && board[i][j + 1] == EMPTY)
                            m = new Move(j, i, j + 1, i);
                    }
                    if (m != null)
                        poMoves.add(m);
                }
            }
        }
        if (greedy) {
            Collections.shuffle(poMoves);
            for (IMove m1 : poMoves) {
                x1 = m1.getMove()[0];
                x2 = m1.getMove()[2];
                y1 = m1.getMove()[1];
                y2 = m1.getMove()[3];
                board[y1][x1] = currentPlayer;
                board[y2][x2] = currentPlayer;
                nMoves = checkNMoves(currentPlayer);
                if (nMoves > maxMoves) {
                    maxMove = m1;
                    maxMoves = nMoves;
                }
                board[y1][x1] = EMPTY;
                board[y2][x2] = EMPTY;
            }
            poMoves.clear();
            poMoves.add(maxMove);
        }
        return poMoves;
    }

    private int checkNMoves(int player) {
        int no = 0, nm = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == EMPTY) {
                    if (i + 1 < size && board[i + 1][j] == EMPTY) {
                        if (player == P1)
                            nm++;
                        else
                            no++;
                    }
                    if (j + 1 < size && board[i][j + 1] == EMPTY) {
                        if (player == P2)
                            nm++;
                        else
                            no++;
                    }
                }
            }
        }
        return nm - no;
    }

    @Override
    public void undoMove() {
    }

    @Override
    public int getOpponent(int player) {
        return 3 - player;
    }

    @Override
    public int checkWin() {
        boolean p1Move = false, p2Move = false;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == EMPTY) {
                    if (i + 1 < size && board[i + 1][j] == EMPTY) p1Move = true;
                    if (j + 1 < size && board[i][j + 1] == EMPTY) p2Move = true;
                }
                if (p1Move && p2Move) break;
            }
            if (p1Move && p2Move) break;
        }
        // If the current player can move, nothing can be said about the winner
        if (!p1Move && p2Move)
            return P2_WIN;
        else if (p1Move && !p2Move)
            return P1_WIN;
        else if (!p1Move && !p2Move)
            return lastMoveBy;
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
        return (size + 1) * 1000000;
    }

    @Override
    public void initialize() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = EMPTY;
            }
        }
        nMoves = 0;
        currentPlayer = P1;
        blocked[0] = new ArrayList();
        blocked[1] = new ArrayList();
        impossibles.clear();
    }

    @Override
    public double evaluate(int player, int version) {
        return 0;
    }

    @Override
    public void initNodePriors(int parentPlayer, StatCounter stats, IMove move, int npvisits) {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                switch (board[i][j]) {
                    case EMPTY:
                        sb.append(".");
                        break;
                    case P1:
                        sb.append("W");
                        break;
                    case P2:
                        sb.append("B");
                        break;
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public double getQuality() {
        return 0;
    }

    @Override
    public MoveList getOrderedMoves() {
        return null;
    }

    @Override
    public long hash() {
        return 0;
    }

    @Override
    public boolean poMoves() {
        return true;
    }

    @Override
    public int getNPlayers() {
        return 2;
    }

    @Override
    public void newDeterminization(int myPlayer, boolean postMove) {
        int removed = 0;
        int opp = getOpponent(myPlayer);
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == opp) {
                    board[i][j] = EMPTY;
                    removed++;
                }
            }
        }
        // Determinize the gamestate
        if (removed > 0) {
            List<IMove> moves = getMoves(opp);
            Collections.shuffle(moves);
            List<IMove> moves1 = new ArrayList<>();
            // Shift all moves that coincide with observations to the front of the move list
            for (int i = 0; i < blocked[myPlayer - 1].size(); i++) {
                IMove move1 = blocked[myPlayer - 1].get(i);
                int x1 = move1.getMove()[0], x2 = move1.getMove()[2];
                int y1 = move1.getMove()[1], y2 = move1.getMove()[3];
                //
                for (int j = 0; j < moves.size(); j++) {
                    IMove move2 = moves.get(j);
                    int x3 = move2.getMove()[0], x4 = move2.getMove()[2];
                    int y3 = move2.getMove()[1], y4 = move2.getMove()[3];
                    if (!moves1.contains(move2)) {
                        if (((x3 == x1 && y3 == y1) || (x4 == x2 && y4 == y2)) &&
                                (board[y3][x3] == EMPTY && board[y4][x4] == EMPTY)) {
                            moves1.add(move2);
                            moves.remove(move2);
                            break;
                        }
                    }
                }
            }
            for (IMove m : moves)
                moves1.add(m);
            // Find a valid determinization
            if (!determinize(moves1, removed, myPlayer, postMove)) {
                throw new RuntimeException("Cannot find determinization!");
            }
        }
    }

    private boolean determinize(List<IMove> moves, int removed, int myPlayer, boolean postMove) {
        // Finished!
        if (removed == 0)
            return checkDeterminization(myPlayer, postMove);
        // First, play all moves that conform to MY observations are made
        for (int i = 0; i < moves.size(); i++) {
            IMove move = moves.get(i);
            int x1 = move.getMove()[0], x2 = move.getMove()[2];
            int y1 = move.getMove()[1], y2 = move.getMove()[3];
            //
            if (board[y1][x1] == EMPTY && board[y2][x2] == EMPTY) {
                board[y1][x1] = 3 - myPlayer;
                board[y2][x2] = 3 - myPlayer;
                if (!impossibles.exists(toString().hashCode(), toString())) {
                    if (determinize(moves, removed - 2, myPlayer, postMove))
                        return true;
                    else
                        impossibles.put(toString().hashCode(), toString());
                }
                board[y1][x1] = EMPTY;
                board[y2][x2] = EMPTY;
            }
        }
        return false;
    }

    private boolean checkDeterminization(int myPlayer, boolean postMove) {
        int winner = checkWin();
        if (!postMove && winner != NONE_WIN)
            return false;
        if (postMove && winner == getOpponent(myPlayer))
            return false;

        for (int i = 0; i < blocked[myPlayer - 1].size(); i++) {
            IMove move1 = blocked[myPlayer - 1].get(i);
            int x1 = move1.getMove()[0], x2 = move1.getMove()[2];
            int y1 = move1.getMove()[1], y2 = move1.getMove()[3];

            if ((board[y1][x1] == EMPTY && board[y2][x2] == EMPTY))
                return false;
        }
        // None of the blocked moves could be played!
        return true;
    }

    @Override
    public boolean isPartialObservable() {
        return true;
    }

    @Override
    public int getNMovesMade() {
        return nMoves;
    }

    @Override
    public boolean isLegal(IMove move) {
        int x1 = move.getMove()[0], x2 = move.getMove()[2];
        int y1 = move.getMove()[1], y2 = move.getMove()[3];
        if (board[y1][x1] == EMPTY && board[y2][x2] == EMPTY)
            return true;
        else
            return false;
    }

    @Override
    public boolean noMovesIsDraw() {
        return false;
    }

    @Override
    public int getHorizon() {
        // After this many turns, the board will be full
        return ((size * size) / 2) - (nMoves) + 1;
    }

    @Override
    public long getStateHash() {
        return 0;
    }
}
