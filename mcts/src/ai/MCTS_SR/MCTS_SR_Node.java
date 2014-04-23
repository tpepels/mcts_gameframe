package ai.MCTS_SR;

import ai.FastLog;
import ai.StatCounter;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import ai.mcts.MCTSOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MCTS_SR_Node {
    public static double INF = 999999;
    public static int maxDepth = 0;
    //
    private boolean expanded = false, simulated = false;
    private List<MCTS_SR_Node> C, S;
    private MCTS_SR_Node bestArm;
    private MCTSOptions options;
    private int player, localVisits = 0, cycles = 0, sr_visits = 0;
    private final StatCounter stats, myStats;
    private IMove move;

    public MCTS_SR_Node(int player, IMove move, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.stats = new StatCounter();
        this.myStats = new StatCounter();
    }

    /**
     * Run the MCTS algorithm on the given node
     */
    public double MCTS_SR(IBoard board, int depth, int budget, int[] plStats) {
        if (budget <= 0)
            throw new RuntimeException("Budget is " + budget);
        double result;
        MCTS_SR_Node child = null;
        // First add some nodes if required
        if (isLeaf())
            child = expand(board);

        if (child != null) {  // Child is a winner
            stats.setValue(-INF);
            return INF;
        }
        // :: Recursive reduction
        int r_s_t = S.size(), s_t = S.size(), s = s_t;
        for (int i = 0; i < cycles; i++)
            r_s_t -= (int) Math.floor(r_s_t / (double) options.rc);

        // Node is terminal
        if (Math.abs(stats.mean()) == INF) {                            // Solver
            plStats[0]++;
            localVisits++;
            sr_visits++;
            return stats.mean();
        } else if (isTerminal()) {     // No solver
            // A draw
            int winner = board.checkWin();
            int score = (winner == player) ? -1 : 1;
            if (winner == IBoard.DRAW)
                score = 0;
//            int b = 1;
//            if (score == 1) // In case of win for parent, update budget times, makes more sense...
//                b = budget;
//            for (int i = 0; i < b; i++) {
            for (int i = 0; i < budget; i++) {
                plStats[0]++;
                localVisits++;
                sr_visits++;
                updateStats(score);
                myStats.push(score);
            }
            return 0;
        }
        // SHOT, single budget, do a play-out
        if (options.shot && budget == 1) {
            result = playOut(board);
            plStats[0]++;
            localVisits++;
            sr_visits++;
            updateStats(-result);
            myStats.push(-result);
            return 0;
        }
        // The current node has some unvisited children
        if (options.shot && sr_visits < s_t) {
            for (MCTS_SR_Node n : S) {
                if (n.sr_visits > 0 || Math.abs(n.stats.mean()) == INF)
                    continue;
                // Perform play-outs on all unvisited children
                board.doAIMove(n.getMove(), player);
                result = n.playOut(board);
                board.undoMove();
                // Stats
                plStats[0]++;
                localVisits++;
                sr_visits++;
                updateStats(result);
                // Update the child as well
                n.updateStats(-result);
                n.myStats.push(-result);
                n.sr_visits++;
                // Don't go over budget
                budget--;
                if (budget == 0)
                    return 0;
            }
        }
        // Don't start any rounds if there is only 1 child
        if (S.size() == 1) {
            int[] pl = {0};
            // :: Recursion
            board.doAIMove(S.get(0).getMove(), player);
            result = -S.get(0).MCTS_SR(board, depth + 1, budget, pl);
            board.undoMove();
            plStats[0] += pl[0];
            sr_visits += pl[0];
            localVisits += pl[0];
            // The only arm is the best
            bestArm = S.get(0);
            // :: Solver recursion
            if (Math.abs(result) == INF)
                solverCheck(result, board);
            else {
                stats.reset();
                stats.add(myStats, false);
                stats.add(S.get(0).stats, true);
            }
            return result;
        }

        // Keep track of the number of cycles at each node
        cycles = (int) Math.min(cycles + 1, Math.ceil((options.rc / 2.) * log2(S.size())));
        int b = getBudget(sr_visits, budget, s_t, s_t);
        // :: UCT
        if (!options.shot && depth > 0 && b < options.bl) {
            // Run UCT budget times
            for (int i = 0; i < budget; i++) {
                result = UCT_MCTS(board, depth);
                plStats[0]++;
                localVisits++;
                sr_visits++;
                // :: Solver
                if (Math.abs(stats.mean()) == INF)
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
            for (MCTS_SR_Node a : S)
                a.localVisits = 0;
            n = 0;
            // :: Round
            while (n < s) {
                child = S.get(n);
                n++;
                // :: Solver win
                if (Math.abs(child.stats.mean()) != INF) {
                    // Determine the actual budget to be used
                    if (b <= child.sr_visits)
                        continue;
                    int b_1 = b - child.sr_visits;
                    if (s == 2 && n == 1)
                        b_1 = Math.max(b_1, (budget - budgetUsed) - (b - S.get(1).sr_visits));
                    // Actual budget
                    int b_b = Math.min(b_1, budget - budgetUsed) - child.localVisits;
                    if (b_b <= 0 && child.localVisits == 0)
                        throw new RuntimeException("b_b is " + b_b);
                    else if (b_b <= 0)
                        continue;
                    // :: Recursion
                    int[] pl = {0};
                    board.doAIMove(child.getMove(), player);
                    result = -child.MCTS_SR(board, depth + 1, b_b, pl);
                    board.undoMove();
                    // Many visit stats, wow
                    budgetUsed += pl[0];
                    plStats[0] += pl[0];
                    sr_visits += pl[0];
                    localVisits += pl[0];
                } else {
                    // The node is already solved
                    result = child.stats.mean();
                }
                // :: Solver
                if (Math.abs(result) == INF) {
                    if (solverCheck(result, board)) {   // Returns true if node is solved
                        if (result == INF)
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

        // :: SR Back propagation
        if (Math.abs(stats.mean()) != INF) {
            stats.reset();
            stats.add(myStats, false);
            // :: Backprop algorithm
            if (options.stat_reset) {
                // Rejection backprop
                for (int i = 0; i < r_s_t; i++)
                    stats.add(S.get(i).stats, true);
            } else if (options.max_back && bestArm != null) {
                // Maximum backprop
                stats.add(bestArm.stats, true);
            } else if (options.range_back && bestArm != null && bestArm.stats.mean() > 0) {
                // Average backprop
                for (MCTS_SR_Node arm : S) {
                    if (arm.stats.mean() <= 0) break;
                    stats.add(arm.stats, true);
                }
            } else {
                // Average backprop
                for (MCTS_SR_Node arm : S) stats.add(arm.stats, true);
            }
        }
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
        if (result == INF) {
            stats.setValue(-INF);
            return true;
        } else if (result == -INF) {
            boolean allSolved = true;
            // (Solver) Check if all children are a loss
            for (MCTS_SR_Node tn : C) {
                // If the child is not expanded, make sure it is
                if (tn.isLeaf() && tn.stats.mean() != INF) {
                    // Execute the move represented by the child
                    board.doAIMove(tn.getMove(), player);
                    MCTS_SR_Node winner = tn.expand(board);
                    board.undoMove();
                    // We found a winning node below the child, this means the child is a loss.
                    if (winner != null)
                        tn.stats.setValue(-INF);
                }
                // Are all children a loss?
                if (tn.stats.mean() != result) {
                    allSolved = false;
                } else {
                    S.remove(tn);
                }
            }
            // (Solver) If all children lead to a loss for me, then I'm a win for the opponent
            if (allSolved) {
                stats.setValue(INF);
                return true;
            }
        }
        return false;
    }

    private double UCT_MCTS(IBoard board, int depth) {
        // First add some nodes if required
        MCTS_SR_Node child = null;
        if (isLeaf())
            child = expand(board);
        double result;

        if (child == null) {
            if (isTerminal()) {// Game is terminal, no more moves can be played
                int score = (board.checkWin() == player) ? -1 : 1;
                updateStats(score);      // TODO Only works with alternating games
                return score;
            } else
                child = uct_select();
        }
        child.sr_visits++;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.stats.mean()) != INF) {
            board.doAIMove(child.getMove(), player);
            if (child.isTerminal() || !child.simulated) {
                // :: Play-out
                result = child.playOut(board);
                child.updateStats(-result);
                child.myStats.push(-result);
                child.simulated = true;
            } else // :: Recursion
                result = -child.UCT_MCTS(board, depth + 1);
            board.undoMove();
        } else {
            result = child.stats.mean();
        }
        // :: Solver for UCT tree
        if (Math.abs(result) == INF) {
            boolean solved = solverCheck(result, board);
            if (result == -INF && !solved) { // Not all arms are losses
                updateStats(1);
                return -1;
            } else                                    // Node is solved
                return result;
        }
        // :: Update
        updateStats(result);
        return result;
    }

    private MCTS_SR_Node uct_select() {
        // Otherwise apply the selection policy
        MCTS_SR_Node selected = null;
        double max = Double.NEGATIVE_INFINITY;
        // Use UCT down the tree
        double uctValue, np = Math.max(sr_visits, stats.totalVisits());
        // Select a child according to the UCT Selection policy
        for (MCTS_SR_Node c : C) {
            double nc = Math.max(c.sr_visits, c.stats.totalVisits());
            // Always select a proven win
            if (c.stats.mean() == INF)
                uctValue = INF + MCTSOptions.r.nextDouble();
            else if (c.stats.totalVisits() == 0 && c.stats.mean() != -INF) {
                // First, visit all children at least once
                uctValue = 100 + MCTSOptions.r.nextDouble();
            } else if (c.stats.mean() == -INF) {
                uctValue = -INF + MCTSOptions.r.nextDouble();
            } else {
                // Compute the uct value with the (new) average value
                uctValue = c.stats.mean() + options.uctC * Math.sqrt(FastLog.log(np) / nc);
            }
            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    private MCTS_SR_Node expand(IBoard board) {
        expanded = true;
        int winner = board.checkWin();
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        MCTS_SR_Node winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (S == null)
            S = new ArrayList<MCTS_SR_Node>(moves.size());
        if (C == null)
            C = new ArrayList<MCTS_SR_Node>(moves.size());
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        double value;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (board.doAIMove(moves.get(i), player)) {
                MCTS_SR_Node child = new MCTS_SR_Node(nextPlayer, moves.get(i), options);
                value = 0.;
                if (options.solver) {
                    // Check for a winner, (Solver)
                    winner = board.checkWin();
                    if (winner == player) {
                        value = INF;
                        // This is a win for the expanding node
                        winNode = child;
                    } else if (winner == nextPlayer)
                        value = -INF;
                    // Set the value of the child (0 = nothing, +/-INF win/loss)
                    child.stats.setValue(value);
                }
                //
                C.add(child);
                if (value != -INF)
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

    private void updateStats(double value) {
        stats.push(value);
    }

    private double log2(double x) {
        return (Math.log(x) / Math.log(2));
    }

    private final Comparator<MCTS_SR_Node> comparator = new Comparator<MCTS_SR_Node>() {
        @Override
        public int compare(MCTS_SR_Node o1, MCTS_SR_Node o2) {
            double v1 = o1.stats.mean(), v2 = o2.stats.mean();
            // Place unvisited nodes in the front
            if (o1.stats.totalVisits() == 0 && Math.abs(o1.stats.mean()) != INF)
                v1 = INF;
            if (o2.stats.totalVisits() == 0 && Math.abs(o2.stats.mean()) != INF)
                v2 = INF;

            return Double.compare(v2, v1);
        }
    };

    public static int totalPlayouts = 0;

    private double playOut(IBoard board) {
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
        double score;
        if (winner == player) score = 1.0;
        else if (winner == IBoard.DRAW) score = 0.0;
        else score = -1;

        // Undo the moves done in the playout
        for (int i = 0; i < nMoves; i++)
            board.undoMove();

        return score;
    }

    public MCTS_SR_Node selectBestMove() {
//        double max = Double.NEGATIVE_INFINITY;
//        MCTS_SR_Node maxNode = bestArm;
//        for (MCTS_SR_Node node : S) {
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
            List<MCTS_SR_Node> l = (S.isEmpty()) ? C : S;
            for (MCTS_SR_Node t : l) {
                System.out.println(t);
            }
        }
        if (bestArm != null)
            return bestArm;
        // Select from the non-solved arms
        MCTS_SR_Node bestChild = null;
        double value;
        double max = Double.NEGATIVE_INFINITY;
        for (MCTS_SR_Node t : C) {
            if (t.stats.mean() == INF)
                value = INF + MCTSOptions.r.nextDouble();
            else if (t.stats.mean() == -INF)
                value = -INF + t.stats.totalVisits() + MCTSOptions.r.nextDouble();
            else {
                // Select the child with the highest value
                value = t.stats.mean();
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
        return C != null && C.size() == 0;
    }

    public IMove getMove() {
        return move;
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("###,##0.00000");
        return move + "\tVal: " + df2.format(stats.mean()) + "\tStatVis: " + stats.totalVisits() +
                "\tSRVisits: " + sr_visits + "\tCylces: " + cycles;
    }
}