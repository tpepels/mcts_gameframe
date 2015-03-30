package mcts_tt.H_MCTS;

import ai.MCTSOptions;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastLog;
import mcts_tt.transpos.State;
import mcts_tt.transpos.TransposTable;

import java.text.DecimalFormat;
import java.util.*;

public class HybridNode {
    private boolean expanded = false, simulated = false;
    private List<HybridNode> C, S;
    private HybridNode bestArm;
    private MCTSOptions options;
    private int player;
    private IMove move;
    private TransposTable tt;
    private long hash;
    private State state;

    public HybridNode(int player, IMove move, MCTSOptions options, long hash, TransposTable tt) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.tt = tt;
        this.hash = hash;
        this.state = tt.getState(hash, true);
    }

    /**
     * Run the MCTS algorithm on the given node
     */
    public double HybridMCTS(IBoard board, int depth, int budget, int[] plStats) {
        if (budget <= 0)
            throw new RuntimeException("Budget is " + budget);
        if (board.hash() != hash)
            throw new RuntimeException("Incorrect hash");
        //
        double result;
        HybridNode child = null;
        // First add some nodes if required
        if (isLeaf())
            child = expand(board);

        if (child != null) {
            if (solverCheck(child.getValue()))
                return State.INF;
        }

        int s = S.size();
        // Node is terminal
        if (isSolved()) {                           // Solver
            return -getValue();
        } else if (isTerminal()) {                  // No solver
            // A draw
            int winner = board.checkWin();
            // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
            for (int i = 0; i < budget; i++) {
                plStats[0]++;
                if (winner != IBoard.DRAW)
                    plStats[winner]++;
            }
            plStats[3] += budget;
            updateStats(plStats);
            return 0;
        }
        //
        int init_s = S.size();
        int b = getBudget(getBudgetNode(), budget, init_s, init_s);
        // :: UCT Hybrid
        if (depth > 0 && b < options.bl) {
            // Run UCT budget times
            for (int i = 0; i < budget; i++) {
                int[] pl = {0, 0, 0, 0};
                result = UCT(board, pl);
                // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
                plStats[0] += pl[0];
                plStats[1] += pl[1];
                plStats[2] += pl[2];
                plStats[3] += pl[3];
                // :: Solver
                if (Math.abs(result) == State.INF)
                    return result;
            }
            return 0;
        }
        // Sort S such that the best node is always the first
        if (getVisits() > S.size())
            Collections.sort(S, comparator);
        // :: Cycle
        do {
            int n = 0, b_s = 0;
            // :: Round
            while (n < s) {
                child = S.get(n++);
                int[] pl = {0, 0, 0, 0};    // This will store the results of the recursion
                int b_b = 0;                // This is the actual budget assigned to the child
                result = 0;
                // :: Solver win
                if (!child.isSolved()) {
                    // :: Actual budget
                    int b1 = (int) (b - child.getVisits());
                    if (s == 2 && n == 1 && S.size() > 1)
                        b1 = (int) Math.max(b1, budget - plStats[3] - (b - S.get(1).getVisits()));
                    b_b = Math.min(b1, budget - plStats[3]);
                    if (b_b <= 0)
                        continue;
                    // :: Recursion
                    board.doAIMove(child.getMove(), player);
                    result = -child.HybridMCTS(board, depth + 1, b_b, pl);
                    board.undoMove();
                    // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
                    if (!(options.max_back && getBudgetNode() > 0) || n == 1) {
                        // With max backprop, only update results if this is the current best arm
                        plStats[0] += pl[0];
                        plStats[1] += pl[1];
                        plStats[2] += pl[2];
                    }
                    plStats[3] += pl[3];
                    // :: SR Back propagation
                    updateStats(pl);
                }
                if (child.isSolved()) {
                    // The node is already solved
                    result = child.getValue();
                }
                // :: Solver
                if (Math.abs(result) == State.INF) {
                    if (solverCheck(result)) {   // Returns true if node is solved
                        if (result == State.INF)
                            bestArm = child;
                        // Update the budgetSpent
                        state.incrBudgetSpent(plStats[3]);
                        return result;
                    } else {
                        // Redistribute the unspent budget in the next round
                        b_s += b_b - pl[3];
                    }
                }
                // Make sure we don't go over budget
                if (plStats[3] >= budget)
                    break;
            }
            if (options.solver) {
                for (Iterator<HybridNode> iterator = S.iterator(); iterator.hasNext(); ) {
                    HybridNode node = iterator.next();
                    if (node.isSolved()) {
                        iterator.remove();
                    }
                }
            }
            // :: Removal policy: Sorting
            if (S.size() > 0)
                Collections.sort(S.subList(0, Math.min(s, S.size())), comparator);
            // :: Removal policy: Reduction
            s -= (int) Math.floor(s / (double) options.rc);
            // For the solver
            s = Math.min(S.size(), s);
            //
            if (s == 1)
                b += budget - plStats[3];
            else {
                b += getBudget(getBudgetNode(), budget, s, init_s); // Use the original size of S here
                // Add any skipped budget from this round
                b += Math.ceil(b_s / (double) s);
            }
        } while (s > 1 && plStats[3] < budget);

        // Update the budgetSpent value
        updateBudgetSpent(plStats[3]);
        // :: Final arm selection
        if (!S.isEmpty())
            bestArm = S.get(0);
        return 0;
    }

    private int getBudget(int initVis, int budget, int subS, int totS) {
        return (int) Math.max(1, Math.floor((initVis + budget) / (subS * Math.ceil((options.rc / 2.) * log2(totS)))));
    }

    private boolean solverCheck(double result) {
        if (!options.solver)
            return false;
        // (Solver) If one of the children is a win, then I'm a loss for the opponent
        if (result == State.INF) {
            setSolved(false);
            return true;
        } else if (result == -State.INF) {
            boolean allSolved = true;
            // (Solver) Check if all children are a loss
            for (HybridNode tn : C) {
                // Are all children a loss?
                if (tn.getValue() != result) {
                    allSolved = false;
                }
            }
            // (Solver) If all children lead to a loss for me, then I'm a win for the opponent
            if (allSolved) {
                setSolved(true);
                return true;
            }
        }
        return false;
    }

    private double UCT(IBoard board, int[] plStats) {
        HybridNode child = null;
        if (isLeaf())
            child = expand(board);
        double result;
        if (child == null) {
            if (isTerminal()) {
                // A draw
                int winner = board.checkWin();
                // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
                plStats[0]++;
                if (winner != IBoard.DRAW)
                    plStats[winner]++;
                updateStats(plStats);
                updateBudgetSpent(1);
                return 0;
            } else
                child = uct_select();
        }
        // (Solver) Check for proven win / loss / draw
        if (!child.isSolved()) {
            board.doAIMove(child.getMove(), player);
            if (!child.simulated) {
                // :: Play-out
                result = child.playOut(board);
                plStats[0]++;
                plStats[3]++;
                if (result != IBoard.DRAW)
                    // 0: playouts, 1: player1, 2: player2
                    plStats[(int) result]++;
                child.updateStats(plStats);
                child.updateBudgetSpent(1);
                child.simulated = true;
            } else // :: Recursion
                result = -child.UCT(board, plStats);
            board.undoMove();
        } else {
            result = child.getValue();
        }
        // :: Solver for UCT tree
        if (Math.abs(result) == State.INF) {
            boolean solved = solverCheck(result);
            if (result == -State.INF && !solved) { // Not all arms are losses
                plStats[0]++;
                plStats[3 - player]++;
                updateStats(plStats);
                return 0;
            } else                                    // Node is solved
                return result;
        }
        // :: Update
        updateStats(plStats);
        updateBudgetSpent(1);
        return 0;
    }

    private HybridNode uct_select() {
        // Otherwise apply the selection policy
        HybridNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        // Use UCT down the tree
        double uctValue, np = getVisits();
        // Select a child according to the UCT Selection policy
        for (HybridNode c : C) {
            double nc = c.getVisits();
            // Always select a proven win
            if (c.getValue() == State.INF)
                uctValue = State.INF + MCTSOptions.r.nextDouble();
            else if (c.getVisits() == 0 && c.getValue() != -State.INF) {
                // First, visit all children at least once
                uctValue = 100. + MCTSOptions.r.nextDouble();
            } else if (c.getValue() == -State.INF) {
                uctValue = -State.INF + MCTSOptions.r.nextDouble();
            } else {
                // Compute the uct value with the (new) average value
                uctValue = c.getValue() + options.uctC * Math.sqrt(FastLog.log(np + 1.) / nc);
                // Add the progressive history value
                if (options.progHistory)
                    uctValue += c.move.getHistoryVal(player, options) * (options.phW / (getVisits() - getWins() + 1));
            }
            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    private HybridNode expand(IBoard board) {
        expanded = true;
        int winner = board.checkWin();
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        HybridNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (S == null)
            S = new ArrayList(moves.size());
        if (C == null)
            C = new ArrayList(moves.size());
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (board.doAIMove(moves.get(i), player)) {
                HybridNode child = new HybridNode(nextPlayer, moves.get(i), options, board.hash(), tt);
                if (options.solver && !child.isSolved()) {
                    // Check for a winner, (Solver)
                    winner = board.checkWin();
                    if (winner == player) {
                        winNode = child;
                        child.setSolved(true);
                    } else if (winner == nextPlayer) {
                        child.setSolved(false);
                    }
                }
                //
                C.add(child);
                S.add(child);
                // reset the board
                board.undoMove();
            }
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private double log2(double x) {
        return (Math.log(x) / Math.log(2));
    }

    private final Comparator<HybridNode> comparator = new Comparator<HybridNode>() {
        @Override
        public int compare(HybridNode o1, HybridNode o2) {
            return Double.compare(o2.getValue(), o1.getValue());
        }
    };

    public static int totalPlayouts = 0;

    private int playOut(IBoard board) {
        totalPlayouts++;
        simulated = true;
        boolean gameEnded, moveMade;
        int cp = board.getPlayerToMove(), nMoves = 0;
        List<IMove> moves;
        int winner = board.checkWin();
        gameEnded = (winner != IBoard.NONE_WIN);
        IMove currentMove;
        while (!gameEnded) {
            moves = board.getPlayoutMoves(options.useHeuristics);
            moveMade = false;
            while (!moveMade) {
                // All moves were thrown away, the game is a draw
                if (moves.size() == 0) {
                    gameEnded = true;
                    // The current player has no moves left
                    if (board.noMovesIsDraw())
                        winner = IBoard.DRAW;                                   // Pentalath, Lost Cities
                    else                                                        // Last player to move:
                        winner = board.getOpponent(board.getPlayerToMove());    // Cannon, Amazons, Chinese Checkers, Checkers
                    break;
                }
                currentMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, cp)) {
                    nMoves++;
                    moveMade = true;
                    winner = board.checkPlayoutWin();
                    gameEnded = winner != IBoard.NONE_WIN;
                    cp = board.getOpponent(cp);
                } else {
                    // The move was illegal, remove it from the list.
                    moveMade = false;
                    moves.remove(currentMove);
                }
            }
        }
        // Undo the moves done in the playout
        for (int i = 0; i < nMoves; i++)
            board.undoMove();
        return winner;
    }


    public HybridNode selectBestMove() {
        // For debugging, print the nodes
        if (options.debug) {
            List<HybridNode> l = (S.isEmpty()) ? C : S;
            for (HybridNode t : l) {
                System.out.println(t);
            }
        }
        if (bestArm != null)
            return bestArm;
        // Select from the non-solved arms
        HybridNode bestChild = null;
        double value;
        double max = Double.NEGATIVE_INFINITY;
        for (HybridNode t : C) {
            if (t.getValue() == State.INF)
                value = State.INF + MCTSOptions.r.nextDouble();
            else if (t.getValue() == -State.INF)
                value = -State.INF + t.getVisits() + MCTSOptions.r.nextDouble();
            else {
                // Select the child with the highest value
                value = t.getValue();
            }
            if (value > max) {
                max = value;
                bestChild = t;
            }
        }
        if (bestChild == null)
            throw new NullPointerException("bestChild is null, root has " + C.size() + " children");
        return bestChild;
    }

    public boolean isLeaf() {
        return C == null || !expanded;
    }

    public boolean isTerminal() {
        return expanded && C != null && C.size() == 0;
    }

    private void updateBudgetSpent(int n) {
        if (state == null)
            state = tt.getState(hash, false);
        state.incrBudgetSpent(n);
    }

    private void updateStats(int[] plStats) {
        if (state == null)
            state = tt.getState(hash, false);
        state.updateStats(plStats[0], plStats[1], plStats[2]);
    }

    private int getBudgetNode() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0;
        return state.getBudgetSpent();
    }

    private void setSolved(boolean win) {
        if (state == null)
            state = tt.getState(hash, false);
        if (win)
            state.setSolved(3 - player);
        else
            state.setSolved(player);
    }

    /**
     * @return The value of this node with respect its parent
     */
    private double getValue() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getMean(3 - player);
    }

    private double getWins() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getWins(3 - player);
    }

    /**
     * @return The number of visits of the transposition
     */
    private double getVisits() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getVisits();
    }

    private boolean isSolved() {
        return Math.abs(getValue()) == State.INF;
    }

    public IMove getMove() {
        return move;
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("##0.####");
        if (state != null) {
            return move + "\t" + state + "\tv:" + df2.format(getValue()) + "\tn: " + state.getBudgetSpent();
        } else {
            return move.toString();
        }
    }
}