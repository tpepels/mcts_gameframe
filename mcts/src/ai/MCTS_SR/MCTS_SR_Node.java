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
    //
    private boolean expanded = false, simulated = false;
    private List<MCTS_SR_Node> C, S;
    private MCTS_SR_Node bestArm;
    private MCTSOptions options;
    private int player, localVisits = 0, cycles = 0, sr_visits = 0;
    private final StatCounter stats;
    private IMove move;

    public MCTS_SR_Node(int player, IMove move, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.stats = new StatCounter();
    }

    /**
     * Run the MCTS algorithm on the given node
     */
    public double MCTS_SR(IBoard board, int depth, int budget, int[] playOuts) {
        double result;
        // First add some nodes if required
        if (isLeaf())
            expand(board);
        // :: Recursive reduction
        int r_s_t = S.size(), s_t = S.size(), init_s_t = S.size(), s = s_t, rr = cycles - 1;
        for (int i = 0; i < rr; i++)
            r_s_t -= (int) Math.floor(r_s_t / (double) options.rc);
        // System.out.println(depth + " " + cycles + " " + s_t);
        if (options.rec_halving) {
            s_t = r_s_t;
            s = r_s_t;
            init_s_t = r_s_t;
        }

        if (depth > 1 || isTerminal() || (depth > 0 && sr_visits < s_t)) { // || Math.floor((sr_visits + budget) / (log2((options.rc / 2.) * s_t))) < 3. * s_t)) {
            // Run UCT MCTS budget times
            for (int i = 0; i < budget; i++) {
                result = UCT_MCTS(board, depth);
                playOuts[0]++;
                localVisits++;
                sr_visits++;
                // :: Solver
                if (Math.abs(stats.mean()) == INF)
                    return result;
            }
        } else {
            // :: Initial Budget
            int init_vis = sr_visits, budgetUsed = 0, n;
            int b = (int) Math.max(1, Math.floor((init_vis + budget) / (s * Math.ceil((options.rc / 2.) * log2(s_t)))));
            // Sort S such that proven losses are at the end, and unvisited nodes at the front
            if (options.remove || depth > 0)
                Collections.sort(S.subList(0, s_t), comparator);
            else
                Collections.sort(S, comparator);
            // Keep track of the number of cycles at each node
            cycles = (int) Math.min(++cycles, Math.ceil((options.rc / 2.) * log2(S.size())));
            MCTS_SR_Node arm;
            // :: Cycle
            while (s > 1 && budgetUsed < budget) {
                for (MCTS_SR_Node a : S)
                    a.localVisits = 0;
                n = 0;
                // :: Round
                while (n < s) {
                    arm = S.get(n);
                    n++;
                    // :: Solver win
                    if (arm.stats.mean() == INF) {
                        bestArm = arm;
                        stats.setValue(-INF);
                        return INF;
                    }
                    // Determine the actual budget to be used
                    if (b <= arm.sr_visits)
                        continue;
                    int b_b = b - arm.sr_visits;
//                    if(move == null && s == 2 && n == 0)
//                        b_b = budget - budgetUsed - (b - S.get(1).sr_visits);
                    b_b = Math.min(b_b, budget - budgetUsed) - arm.localVisits;
                    // :: Recursion
                    board.doAIMove(arm.getMove(), player);
                    int[] pl = {0};
                    result = -arm.MCTS_SR(board, depth + 1, b_b, pl);
                    budgetUsed += pl[0];
                    playOuts[0] += pl[0];
                    sr_visits += pl[0];
                    board.undoMove();
                    // :: Solver recursion
                    if (Math.abs(result) == INF) {
                        if (solverCheck(result, board)) {   // Returns true if node is solved
                            if (result == INF)
                                bestArm = arm;
                            return result;
                        } else {
                            // :: Solver: Recompute the budget for the rest of the arms
                            if (Math.abs(result) == INF) {
                                // :: Solver: Resume the round with reduced S
                                init_s_t = Math.min(S.size(), init_s_t);
                                r_s_t = Math.min(S.size(), r_s_t);
                                s_t = Math.min(S.size(), init_s_t);
                                s = Math.min(s, s_t);
                                b = (int) Math.max(1, Math.floor(budget / (s * Math.ceil((options.rc / 2.) * log2(s_t)))));
                                // Restart at the first arm to redistribute the budget
                                n = 0;
                            }
                        }
                    }
                    // Make sure we don't go over budget
                    if (budgetUsed >= budget)
                        break;
                }

                // :: Removal policy: Sorting
                if (options.remove || depth > 0)
                    Collections.sort(S.subList(0, s), comparator);
                else
                    Collections.sort(S, comparator);

                // :: Removal policy: Reduction
                s -= (int) Math.floor(s / (double) options.rc);

                // :: Re-budgeting
                b += (int) Math.max(1, Math.floor((init_vis + budget) / (s * Math.ceil((options.rc / 2.) * log2(s_t)))));
            }

            // :: Final arm selection
            if (!S.isEmpty())
                bestArm = S.get(0);

            // :: SR Back propagation
            if (Math.abs(stats.mean()) != INF) {
                stats.reset();
                if (options.stat_reset) {
                    for (int i = 0; i < r_s_t; i++)
                        stats.add(S.get(i).stats, true);
                } else if (options.max_back && bestArm != null) {
                    stats.add(bestArm.stats, true);
                } else {
                    for (int i = 0; i < S.size(); i++)
                        stats.add(S.get(i).stats, true);
                }
            }
        }
        return 0;
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
        if (isLeaf())
            expand(board);
        double result;
        MCTS_SR_Node child = null;
        if (isTerminal()) {
            int score = (board.checkWin() == player) ? -1 : 1;
            updateStats(score);
            return score;
        } else
            child = uct_select();
        child.sr_visits++;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.stats.mean()) != INF) {
            board.doAIMove(child.getMove(), player);
            if (child.isTerminal() || !child.simulated) {
                // :: Play-out
                result = child.playOut(board);
                child.updateStats(-result);
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
                uctValue = -INF;
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

    private double playOut(IBoard board) {
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
            // For debugging, print the node
            if (options.debug)
                System.out.println(t);
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
