package phantomdomineering.game;

import framework.FiniteBoard;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.StatCounter;

import java.util.ArrayList;
import java.util.List;

public class Board implements FiniteBoard {
    private static final int EMPTY = 0;
    private static final ArrayList<IMove> poMoves = new ArrayList<>(1000);
    private static final MoveList static_moves = new MoveList(1000);
    //
    private final int[][] board;
    private final int size;
    private final List<IMove>[] blocked;
    private int nMoves, currentPlayer;

    public Board(int size) {
        this.size = size;
        this.board = new int[size][size];
        this.nMoves = 0;
        this.blocked = new ArrayList[2];
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
        IMove m;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != currentPlayer) {
                    if (currentPlayer == P1)
                        if (i + 1 < size && board[i + 1][j] != P1) {
                            m = new Move(j, i, j, i + 1);
                            if (!isBlocked(currentPlayer, m))
                                static_moves.add(m);
                        }
                    if (currentPlayer == P2)
                        if (j + 1 < size && board[i][j + 1] != P2) {
                            m = new Move(j, i, j + 1, i);
                            if (!isBlocked(currentPlayer, m))
                                static_moves.add(m);
                        }
                }
            }
        }
        return static_moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        poMoves.clear();
        IMove m;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != currentPlayer) {
                    // if cram, check in all directions
                    if (currentPlayer == P1)
                        if (i + 1 < size && board[i + 1][j] != P1) {
                            m = new Move(j, i, j, i + 1);
                            if (!isBlocked(currentPlayer, m))
                                poMoves.add(m);
                        }
                    if (currentPlayer == P2)
                        if (j + 1 < size && board[i][j + 1] != P2) {
                            m = new Move(j, i, j + 1, i);
                            if (!isBlocked(currentPlayer, m))
                                poMoves.add(m);
                        }
                }
            }
        }
        return poMoves;
    }

    private boolean isBlocked(int player, IMove m) {
        for (IMove bm : blocked[player - 1])
            if (bm.equals(m))
                return true;
        return false;
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
        boolean canMove = false;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == EMPTY) {
                    // if cram, check in all directions
                    if (currentPlayer == P1)
                        if (i + 1 < size && board[i + 1][j] == EMPTY) canMove = true;
                    if (currentPlayer == P2)
                        if (j + 1 < size && board[i][j + 1] == EMPTY) canMove = true;
                }
                if (canMove) break;
            }
            if (canMove) break;
        }
        // If the current player can move, nothing can be said about the winner
        if (!canMove)
            return getOpponent(currentPlayer);
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
    public void newDeterminization(int myPlayer) {
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
        if (removed > 0) {
            currentPlayer = opp;
            MoveList list = getExpandMoves();
            currentPlayer = myPlayer;
            list.shuffle();
            // Find a valid determinization
            if (!determinize1(list, removed, myPlayer, opp, 0)) {
                throw new RuntimeException("Cannot find determinization!");
            }
        }
    }

    private boolean determinize1(MoveList moves, int removed, int myPlayer, int opp, int i) {
        if (removed == 0)
            return true;
        if(blocked[myPlayer - 1].size() > 0) {
            // First, play all moves that conform to observations are made
            for (; i < blocked[myPlayer - 1].size(); i++) {
                IMove move1 = blocked[myPlayer - 1].get(i);
                int x1 = move1.getMove()[0], x2 = move1.getMove()[2];
                int y1 = move1.getMove()[1], y2 = move1.getMove()[3];
                if((board[x1][y1] != EMPTY && board[x2][y2] != EMPTY))
                    continue;
                for (int j = 0; j < moves.size(); j++) {
                    IMove move2 = moves.get(j);
                    int x3 = move2.getMove()[0], x4 = move2.getMove()[2];
                    int y3 = move2.getMove()[1], y4 = move2.getMove()[3];

                    if (((x3 == x1 && y3 == y1) || (x4 == x2 && y4 == y2)) &&
                            (board[y3][x3] == EMPTY && board[y4][x4] == EMPTY)) {
                        board[y3][x3] = opp;
                        board[y4][x4] = opp;

                        if ((i + 1) < blocked[myPlayer - 1].size()) {
                            if (determinize1(moves, removed - 2, myPlayer, opp, i + 1))
                                return true;
                        } else if (determinize2(moves, removed - 2, opp, 0))
                            return true;

                        board[y3][x3] = EMPTY;
                        board[y4][x4] = EMPTY;
                    }
                }
            }
            return false;
        } else {
            return determinize2(moves, removed, opp, 0);
        }
    }

    private boolean determinize2(MoveList moves, int removed, int opp, int i) {
        // Finished!
        if (removed == 0)
            return true;
        // First, play all moves that conform to MY observations are made
        for (; i < moves.size(); i++) {
            IMove move = moves.get(i);
            int x1 = move.getMove()[0], x2 = move.getMove()[2];
            int y1 = move.getMove()[1], y2 = move.getMove()[3];
            //
            if (board[y1][x1] == EMPTY && board[y2][x2] == EMPTY) {

                board[y1][x1] = opp;
                board[y2][x2] = opp;

                if (determinize2(moves, removed - 2, opp, i + 1))
                    return true;

                board[y1][x1] = EMPTY;
                board[y2][x2] = EMPTY;
            }
        }
        // No move was found
        return false;
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
        if (board[y1][x1] != currentPlayer && board[y2][x2] != currentPlayer)
            return !isBlocked(currentPlayer, move);
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
