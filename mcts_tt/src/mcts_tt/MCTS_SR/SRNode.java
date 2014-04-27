package mcts_tt.MCTS_SR;

import ai.FastLog;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import ai.mcts.MCTSOptions;
import mcts_tt.transpos.State;
import mcts_tt.transpos.TransposTable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SRNode {
    public static int maxDepth = 0;
    //
    private boolean expanded = false, simulated = false;
    private List<SRNode> C, S;
    private SRNode bestArm;
    private MCTSOptions options;
    private int player, localVisits = 0, cycles = 0, sr_visits = 0;
    private IMove move;
    private TransposTable tt;
    private long hash;
    private State state;

    public SRNode(int player, IMove move, MCTSOptions options, long hash, TransposTable tt) {
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
    public double MCTS_SR(IBoard board, int depth, int budget, int[] plStats) {
        if (budget <= 0)
            throw new RuntimeException("Budget is " + budget);
        if (board.hash() != hash)
            throw new RuntimeException("Incorrect hash");
        //
        double result;
        SRNode child = null;
        // First add some nodes if required
        if (isLeaf())
            child = expand(board);
        if (child != null) {  // Child is a winner
            setSolved(false);
            return State.INF;
        }
        // :: Recursive reduction
        int r_s_t = S.size(), s_t = S.size(), s = s_t;
        for (int i = 0; i < cycles; i++)
            r_s_t -= (int) Math.floor(r_s_t / (double) options.rc);

        // Node is terminal
        if (Math.abs(getValue()) == State.INF) {                            // Solver
            return getValue();
        } else if (isTerminal()) {     // No solver
            // A draw
            int winner = board.checkWin();
//            int b = 1;
//            if (score == 1) // In case of win for parent, update budget times, makes more sense...
//                b = budget;
//            for (int i = 0; i < b; i++) {
            for (int i = 0; i < budget; i++) {
                plStats[0]++;
                if (winner != IBoard.DRAW)
                    plStats[winner]++;
            }
            updateStats(plStats);
            return 0;
        }
        // SHOT, single budget, do a play-out
        if (options.shot && budget == 1) {
            result = playOut(board);
            plStats[0]++;
            if (result != IBoard.DRAW)
                // 0: playouts, 1: player1, 2: player2
                plStats[(int) result]++;
            updateStats(plStats);
            return 0;
        }
        // The current node has some unvisited children
        if (options.shot && sr_visits < s_t) {
            for (SRNode n : S) {
                if (n.getVisits() > 0 || Math.abs(n.getValue()) == State.INF)
                    continue;
                // Perform play-outs on all unvisited children
                board.doAIMove(n.getMove(), player);
                result = n.playOut(board);
                board.undoMove();
                //
                int[] pl = {1, 0, 0};
                if (result != IBoard.DRAW)
                    // 0: playouts, 1: player1, 2: player2
                    pl[(int) result]++;
                // Stats
                plStats[0]++;
                plStats[1] += pl[1];
                plStats[2] += pl[2];
                // Update the child as well
                n.updateStats(pl);
                // Don't go over budget
                budget--;
                if (budget == 0)
                    return 0;
            }
            updateStats(plStats);
        }
        // Don't start any rounds if there is only 1 child
        if (S.size() == 1) {
            int[] pl = {0, 0, 0};
            // :: Recursion
            board.doAIMove(S.get(0).getMove(), player);
            result = -S.get(0).MCTS_SR(board, depth + 1, budget, pl);
            board.undoMove();
            //
            plStats[0] += pl[0];
            plStats[1] += pl[1];
            plStats[2] += pl[2];
            // The only arm is the best
            bestArm = S.get(0);
            // :: Solver recursion
            if (Math.abs(result) == State.INF)
                solverCheck(result, board);
            else
                updateStats(pl);
            //
            return result;
        }

        // Keep track of the number of cycles at each node
        cycles = (int) Math.min(cycles + 1, Math.ceil((options.rc / 2.) * log2(S.size())));
        int b = getBudget((int) getVisits(), budget, s_t, s_t);
        // :: UCT
        if (!options.shot && depth > 0 && b < options.bl) {
            // Run UCT budget times
            for (int i = 0; i < budget; i++) {
                int[] pl = {0, 0, 0};
                result = UCT_MCTS(board, pl);
                plStats[0] += pl[0];
                plStats[1] += pl[1];
                plStats[2] += pl[2];
                // :: Solver
                if (Math.abs(result) == State.INF)
                    return result;
            }
            return 0;
        }

        // :: Simple regret
        if (options.debug && depth > maxDepth)
            maxDepth = depth;

        // :: Initial Budget
        int initVis = sr_visits, budgetUsed = 0, n;
        // Sort S such that proven losses are at the end, and unvisited nodes in the front
        Collections.sort(S, comparator);
        // :: Cycle
        while (s > 1 && budgetUsed < budget) {
            // Local visits are used as memory for the solver
            for (SRNode a : S)
                a.localVisits = 0;
            n = 0;
            // :: Round
            while (n < s) {
                child = S.get(n);
                n++;
                // :: Solver win
                if (Math.abs(child.getValue()) != State.INF) {
                    // Determine the actual budget to be used
                    if (b <= child.getVisits())
                        continue;
                    int b_1 = b - (int) child.getVisits();
                    if (s == 2 && n == 1)
                        b_1 = Math.max(b_1, (budget - budgetUsed) - (b - (int) S.get(1).getVisits()));
                    // Actual budget
                    int b_b = Math.min(b_1, budget - budgetUsed) - child.localVisits;
                    if (b_b <= 0)
                        continue;
                    // :: Recursion
                    int[] pl = {0, 0, 0};   // This will store the results of the recursion
                    board.doAIMove(child.getMove(), player);
                    result = -child.MCTS_SR(board, depth + 1, b_b, pl);
                    board.undoMove();
                    // Many stats, wow
                    budgetUsed += pl[0];
                    plStats[0] += pl[0];
                    plStats[1] += pl[1];
                    plStats[2] += pl[2];
                    // :: SR Back propagation
                    updateStats(pl);
                } else {
                    // The node is already solved
                    result = child.getValue();
                }
                // :: Solver
                if (Math.abs(result) == State.INF) {
                    if (solverCheck(result, board)) {   // Returns true if node is solved
                        if (result == State.INF)
                            bestArm = child;
                        return result;
                    } else {
                        // :: Solver: Resume the round with reduced S
                        r_s_t = Math.min(S.size(), r_s_t);
                        s_t = Math.min(S.size(), s_t);
                        s = Math.min(S.size(), s);
                        b = getBudget(initVis, budget, s, s_t);
                        // Restart at the first arm to redistribute the budget
                        n = 0;
                    }
                }
                // Make sure we don't go over budget
                if (budgetUsed >= budget)
                    break;
            }
            // :: Removal policy: Sorting
            if (options.remove)
                Collections.sort(S.subList(0, s), comparator);
            else
                Collections.sort(S, comparator);
            // :: Removal policy: Reduction
            if (options.rc > 1)
                s -= (int) Math.floor(s / (double) options.rc);
            else
                s--;
            // :: Re-budgeting
            b += getBudget(initVis, budget, s, s_t);
        }
        // :: Final arm selection
        if (!S.isEmpty())
            bestArm = S.get(0);
        return 0;
    }

    private int getBudget(int initVis, int budget, int subS, int totS) {
        if (options.rc > 1)
            return (int) Math.max(1, Math.floor((initVis + budget) / (subS * Math.ceil((options.rc / 2.) * log2(totS)))));
        else
            return (int) Math.max(1, Math.floor((initVis + budget) / (subS * Math.ceil((totS / 2.) * log2(totS)))));
    }


    private boolean solverCheck(double result, IBoard board) {
        if (!options.solver)
            return false;
        // (Solver) If one of the children is a win, then I'm a loss for the opponent
        if (result == State.INF) {
            setSolved(false);
            return true;
        } else if (result == -State.INF) {
            boolean allSolved = true;
            // (Solver) Check if all children are a loss
            for (SRNode tn : C) {
                // If the child is not expanded, make sure it is
                if (tn.isLeaf() && tn.getValue() != State.INF) {
                    // Execute the move represented by the child
                    board.doAIMove(tn.getMove(), player);
                    SRNode winner = tn.expand(board);
                    board.undoMove();
                    // We found a winning node below the child, this means the child is a loss.
                    if (winner != null)
                        tn.setSolved(false);
                }
                // Are all children a loss?
                if (tn.getValue() != result) {
                    allSolved = false;
                } else {
                    S.remove(tn);
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

    private double UCT_MCTS(IBoard board, int[] plStats) {
        // First add some nodes if required
        SRNode child = null;
        if (isLeaf())
            child = expand(board);
        double result;
        if (child == null) {
            if (isTerminal())
                child = this;
            else
                child = uct_select();
        }
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.getValue()) != State.INF) {
            board.doAIMove(child.getMove(), player);
            if (child.isTerminal() || !child.simulated) {
                // :: Play-out
                result = child.playOut(board);
                plStats[0]++;
                if (result != IBoard.DRAW)
                    // 0: playouts, 1: player1, 2: player2
                    plStats[(int) result]++;
                child.updateStats(plStats);
                child.simulated = true;
            } else // :: Recursion
                result = -child.UCT_MCTS(board, plStats);
            board.undoMove();
        } else {
            result = child.getValue();
        }
        // :: Solver for UCT tree
        if (Math.abs(result) == State.INF) {
            boolean solved = solverCheck(result, board);
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
        return 0;
    }

    private SRNode uct_select() {
        // Otherwise apply the selection policy
        SRNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        // Use UCT down the tree
        double uctValue, np = getVisits();
        // Select a child according to the UCT Selection policy
        for (SRNode c : C) {
            double nc = c.getVisits();
            // Always select a proven win
            if (c.getValue() == State.INF)
                uctValue = State.INF + MCTSOptions.r.nextDouble();
            else if (c.getVisits() == 0 && c.getValue() != -State.INF) {
                // First, visit all children at least once
                uctValue = 100 + MCTSOptions.r.nextDouble();
            } else if (c.getValue() == -State.INF) {
                uctValue = -State.INF + MCTSOptions.r.nextDouble();
            } else {
                // Compute the uct value with the (new) average value
                uctValue = c.getValue() + options.uctC * Math.sqrt(FastLog.log(np) / nc);
            }
            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    private SRNode expand(IBoard board) {
        expanded = true;
        int winner = board.checkWin();
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        SRNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (S == null)
            S = new ArrayList<>(moves.size());
        if (C == null)
            C = new ArrayList<>(moves.size());
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        double value;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (board.doAIMove(moves.get(i), player)) {
                SRNode child = new SRNode(nextPlayer, moves.get(i), options, board.hash(), tt);
                value = 0.;
                if (options.solver) {
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
                if (value != -State.INF)
                    S.add(child);
                // reset the board
                board.undoMove();
            }
        }
        // Make sure S always contains some children
        if (S.size() == 0)
            S.addAll(C);
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private double log2(double x) {
        return (Math.log(x) / Math.log(2));
    }

    private final Comparator<SRNode> comparator = new Comparator<SRNode>() {
        @Override
        public int compare(SRNode o1, SRNode o2) {
            double v1 = o1.getValue(), v2 = o2.getValue();
            // Place unvisited nodes in the front
            if (o1.sr_visits == 0 && Math.abs(o1.getValue()) != State.INF)
                v1 = State.INF;
            if (o2.sr_visits == 0 && Math.abs(o2.getValue()) != State.INF)
                v2 = State.INF;

            return Double.compare(v2, v1);
        }
    };

    public static int totalPlayouts = 0;

    private int playOut(IBoard board) {
        totalPlayouts++;
        simulated = true;
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove(), nMoves = 0;
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
                if (board.doAIMove(currentMove, currentPlayer)) {
                    nMoves++;
                    moveMade = true;
                    winner = board.checkPlayoutWin();
                    gameEnded = winner != IBoard.NONE_WIN;
                    currentPlayer = board.getOpponent(currentPlayer);
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

    public SRNode selectBestMove() {
//        double max = Double.NEGATIVE_State.INFINITY;
//        SRNode maxNode = bestArm;
//        for (SRNode node : S) {
//            if (node.stats.mean() > max) {
//                max = node.stats.mean();
//                maxNode = node;
//            }
//        }
//        if (maxNode != bestArm) {
//            System.out.println("Bestarm not highest score");
//            System.out.println(bestArm);
//            System.out.println(maxNode);
//        }
        // For debugging, print the nodes
        if (options.debug) {
            List<SRNode> l = (S.isEmpty()) ? C : S;
            for (SRNode t : l) {
                System.out.println(t);
            }
        }
        if (bestArm != null)
            return bestArm;
        // Select from the non-solved arms
        SRNode bestChild = null;
        double value;
        double max = Double.NEGATIVE_INFINITY;
        for (SRNode t : C) {
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
        return bestChild;
    }

    public boolean isLeaf() {
        return C == null || !expanded;
    }

    public boolean isTerminal() {
        return expanded && C != null && C.size() == 0;
    }

    private void updateStats(int[] plStats) {
        if (state == null)
            state = tt.getState(hash, false);
        state.updateStats(plStats[0], plStats[1], plStats[2]);
        sr_visits += plStats[0];
        localVisits += plStats[0];
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

    public IMove getMove() {
        return move;
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("##0.####");
        return move + "\t" + state + "\tv:" + df2.format(getValue()) + "\tn: " + sr_visits + "\tc: " + cycles;
    }
}