package mcts_tt.MCTS_SR;

import ai.FastLog;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import ai.mcts.MCTSOptions;
import mcts_tt.transpos.State;
import mcts_tt.transpos.TransposTable;

import java.text.DecimalFormat;
import java.util.*;

public class SRNode {
    public static int maxDepth = 0;
    private static final MoveList[] movesMade = {new MoveList(500), new MoveList(500)};
    private static final MoveList mastMoves = new MoveList(1000);
    //
    private boolean expanded = false, simulated = false;
    private List<SRNode> C, S;
    private SRNode bestArm;
    private MCTSOptions options;
    private int player;
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
        double result;
        SRNode child = null;
        // First add some nodes if required
        if (isLeaf())
            child = expand(board);

        if (child != null) {
            if (solverCheck(child.getValue(), board))
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
        //<editor-fold desc="SHOT">
        if (options.shot && budget == 1) {
            result = playOut(board);
            // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
            plStats[0]++;
            plStats[3]++;
            if (result != IBoard.DRAW) {
                plStats[(int) result]++;
            }
            updateStats(plStats);
            return 0;
        }
        // The current node has some unvisited children
        if (options.shot && getBudgetNode() <= S.size()) {
            for (SRNode n : S) {
                if (n.simulated || n.isSolved())
                    continue;
                // Perform play-outs on all unvisited children
                board.doAIMove(n.getMove(), player);
                result = n.playOut(board);
                board.undoMove();
                //
                int[] pl = {1, 0, 0, 0};
                if (result != IBoard.DRAW) {
                    pl[(int) result]++;
                }
                // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
                plStats[0]++;
                plStats[1] += pl[1];
                plStats[2] += pl[2];
                plStats[3]++;
                // Update the child and current node
                n.updateStats(pl);
                updateStats(pl);
                // Don't go over budget
                if (plStats[3] >= budget)
                    return 0;
            }
        }
        // Don't start any rounds if there is only 1 child
        if (options.shot && S.size() == 1) {
            int[] pl = {0, 0, 0, 0};
            child = S.get(0);
            if (!child.isSolved()) {
                // :: Recursion
                board.doAIMove(child.getMove(), player);
                result = -child.MCTS_SR(board, depth + 1, budget, pl);
                board.undoMove();
                // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
                plStats[0] += pl[0];
                plStats[1] += pl[1];
                plStats[2] += pl[2];
                plStats[3] += pl[3];
            } else {
                result = child.getValue();
            }
            // The only arm is the best
            bestArm = S.get(0);
            // :: Solver
            if (Math.abs(result) == State.INF)
                solverCheck(result, board);
            else
                updateStats(pl);
            //
            return result;
        }
        //</editor-fold>
        int init_s = S.size();
        if (options.UBLB && init_s > 1) {
            double lb = S.get(0).getValue() - Math.sqrt(FastLog.log(getVisits()) / S.get(0).getVisits());
            for (int i = s - 1; i > 0; i--) {
                if (S.get(i).getValue() + Math.sqrt(FastLog.log(getVisits()) / S.get(i).getVisits()) < lb) {
                    init_s--;
                } else
                    break;
            }
            s = init_s;
        }
        int b = getBudget(getBudgetNode(), budget, init_s, init_s);
        // :: UCT Hybrid
        if (!options.shot && depth > 0 && b < options.bl) {
            // Run UCT budget times
            for (int i = 0; i < budget; i++) {
                int[] pl = {0, 0, 0, 0};
                result = UCT_MCTS(board, pl);
                // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
                plStats[0] += pl[0];
                plStats[1] += pl[1];
                plStats[2] += pl[2];
                plStats[3] += pl[3];
                // :: Solver
                if (Math.abs(result) == State.INF) {
                    return result;
                }
            }
            return 0;
        }
        // :: Simple regret
        if (options.debug && depth > maxDepth)
            maxDepth = depth;
        // Sort S such that the best node is always the first
        if (getVisits() > S.size())
            Collections.sort(S, (!options.history) ? comparator : phComparator);

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
                    // Compare the upper bound of this child to the lower bound of the best child
//                    if (options.UBLB && getVisits() > S.size() && n > 1) {
//                        if (child.getValue() + options.uctC * Math.sqrt(FastLog.log(getVisits()) / child.getVisits()) < lb) {
//                            b_s += b_b;
//                            continue; // Don't go into the recursion, but skip the node
//                        }
//                    }
                    // :: Recursion
                    board.doAIMove(child.getMove(), player);
                    if (options.history)
                        movesMade[player - 1].add(child.getMove());
                    result = -child.MCTS_SR(board, depth + 1, b_b, pl);
                    board.undoMove();
                    if (options.history)
                        movesMade[player - 1].clearLast(1);
                    // 0: playouts, 1: player1, 2: player2, 3: budgetUsed
                    plStats[0] += pl[0];
                    plStats[1] += pl[1];
                    plStats[2] += pl[2];
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
                    if (solverCheck(result, board)) {   // Returns true if node is solved
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
//                else if (options.UBLB && n == 1) {
//                    // The lower bound for the best child
//                    lb = child.getValue() - options.uctC * Math.sqrt(FastLog.log(getVisits()) / child.getVisits());
//                }
                // Make sure we don't go over budget
                if (plStats[3] >= budget)
                    break;
            }
            if (options.solver) {
                for (Iterator<SRNode> iterator = S.iterator(); iterator.hasNext(); ) {
                    SRNode node = iterator.next();
                    if (node.isSolved()) {
                        iterator.remove();
                    }
                }
            }
            // :: Removal policy: Sorting
            if (S.size() > 0)
                Collections.sort(S.subList(0, Math.min(Math.max(s, Math.min(S.size(), 2)), S.size())), (!options.history) ? comparator : phComparator);
            // :: Removal policy: Reduction
            s -= (int) Math.floor(s / (double) options.rc);
            // For the solver
            s = Math.min(S.size(), s);
            //
            if (options.UBLB && s > 1) {
                double lb = S.get(0).getValue() - Math.sqrt(FastLog.log(getVisits()) / S.get(0).getVisits());
                for (int i = s - 1; i > 0; i--) {
                    if (S.get(i).getValue() + Math.sqrt(FastLog.log(getVisits()) / S.get(i).getVisits()) < lb)
                        s--;
                    else
                        break;
                }
            }
            //
            if (s == 1)
                b += budget - plStats[3];
            else {
                b += getBudget(getBudgetNode(), budget, s, init_s);
                // Add any skipped budget from this round
                b += Math.ceil(b_s / (double) s);
            }
        } while (plStats[3] < budget);

        // Update the budgetSpent value
        updateBudgetSpent(plStats[3]);
        // :: Final arm selection
        if (!S.isEmpty())
            bestArm = S.get(0);
        // :: SR Max back-propagation
        if (!isSolved() && options.max_back && bestArm != null
                && bestArm.state != null && !bestArm.isSolved()) {
            //
            setValue(bestArm.getState());
        }
        return 0;
    }

    private int getBudget(int initVis, int budget, int subS, int totS) {
        return (int) Math.max(1, Math.floor((initVis + budget) / (subS * Math.ceil((options.rc / 2.) * log2(totS)))));
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

    private double UCT_MCTS(IBoard board, int[] plStats) {
        SRNode child = null;
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

            if (options.history)
                movesMade[player - 1].add(child.getMove());

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
                result = -child.UCT_MCTS(board, plStats);

            board.undoMove();

            if (options.history)
                movesMade[player - 1].clearLast(1);
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
        updateBudgetSpent(1);
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
            return Double.compare(o2.getValue(), o1.getValue());
        }
    };

    private final Comparator<SRNode> phComparator = new Comparator<SRNode>() {
        @Override
        public int compare(SRNode o1, SRNode o2) {
            double v1 = o1.getValue();
            double v2 = o2.getValue();
            v1 += o1.move.getHistoryVal(3 - player, options) * (options.sr_phW / (getVisits() - getWins() + 1));
            v2 += o2.move.getHistoryVal(3 - player, options) * (options.sr_phW / (getVisits() - getWins() + 1));
            return Double.compare(v2, v1);
        }
    };

    public static int totalPlayouts = 0;

    private int playOut(IBoard board) {
        totalPlayouts++;
        simulated = true;
        boolean gameEnded, moveMade;
        int[] pMoves = new int[2];
        double mastMax, mastVal;
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
                // Select a move to play
                currentMove = null;
                if (options.MAST && MCTSOptions.r.nextDouble() < options.mastEps) {
                    mastMoves.clear();
                    mastMax = Double.NEGATIVE_INFINITY;
                    IMove m = null;
                    // Select the move with the highest MAST value
                    for (int i = 0; i < moves.size(); i++) {
                        m = moves.get(i);
                        if (m.getHistoryVis(cp, options) == 0)
                            continue;
                        mastVal = moves.get(i).getHistoryVal(cp, options);
                        // If bigger, we have a winner, if equal, flip a coin
                        if (mastVal > mastMax) {
                            mastMoves.clear();
                            mastMax = mastVal;
                            mastMoves.add(moves.get(i));
                        } else if (mastVal == mastMax) {
                            mastMoves.add(moves.get(i));
                        }
                    }
                    if (mastMoves.size() > 0)
                        currentMove = mastMoves.get(MCTSOptions.r.nextInt(mastMoves.size()));
                }
                if (currentMove == null) {
                    // Choose randomly
                    currentMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
                }
                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, cp)) {
                    nMoves++;
                    pMoves[cp - 1]++;

                    // Keep track of moves made
                    if (options.history && !options.to_history)
                        movesMade[cp - 1].add(currentMove);

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
        // Update the history values for the moves made during the match
        if (options.history) {
            double p1Score = (winner == IBoard.P1_WIN) ? 1 : -1;
            for (int i = 0; i < movesMade[0].size(); i++) {
                options.updateHistory(1, movesMade[0].get(i).getUniqueId(), p1Score);
            }
            for (int i = 0; i < movesMade[1].size(); i++) {
                options.updateHistory(2, movesMade[1].get(i).getUniqueId(), -p1Score);
            }
            // Clear the moves made during the play-out
            movesMade[0].clearLast(pMoves[0]);
            movesMade[1].clearLast(pMoves[1]);
        }
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

    private void setValue(State s) {
        if (state == null)
            state = tt.getState(hash, false);
        state.setValue(s);
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

    private State getState() {
        if (state == null)
            state = tt.getState(hash, false);
        return state;
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