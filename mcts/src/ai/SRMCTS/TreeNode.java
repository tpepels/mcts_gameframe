package ai.SRMCTS;

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

public class TreeNode {
    public static final double INF = 999999;
    public static int myPlayer = 0, rootRounds = 0;
    private static final MoveList mastMoves = new MoveList(100);
    private static final MoveList[] movesMade = {new MoveList(500), new MoveList(500)};
    //
    private final MCTSOptions options;
    private final TreeNode parent;
    private final UCT uct;
    public final int player, ply;
    public StatCounter stats;
    //
    private boolean expanded = false, simulated = false;
    public List<TreeNode> children, A, As, S;
    private int totVisits = 0, totalBudget = 0, roundSimulations = 0, budget = 0;
    private IMove move;

    /**
     * Constructor for the root
     */
    public TreeNode(int player, MCTSOptions options, int totalBudget) {
        this.player = player;
        this.options = options;
        this.totalBudget = totalBudget;
        TreeNode.myPlayer = player;
        stats = new StatCounter();
        this.uct = new UCT(options);
        this.parent = null;
        this.ply = 0;
    }

    /**
     * Constructor for internal node
     */
    public TreeNode(int player, IMove move, MCTSOptions options, TreeNode parent) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.stats = new StatCounter();
        this.uct = new UCT(options);
        this.parent = parent;
        this.ply = parent.ply + 1;
    }

    /**
     * Run the MCTS algorithm on the given node
     */
    public double MCTS(IBoard board, int depth) {
        if (depth == 0) {
            movesMade[0].clear();
            movesMade[1].clear();
        }
        // First add some nodes if required
        if (isLeaf())
            expand(board, depth);
        //
        TreeNode child;
        if (isTerminal()) {       // Game is terminal, no more moves can be played
            child = this;
            //
            child.budget++;
            child.round++;
        } else
            child = select(depth);
        //
        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.stats.mean()) != INF) {
            // Execute the move represented by the child
            if (!isTerminal())
                board.doAIMove(child.getMove(), player);
            //
            if (options.history)
                movesMade[player - 1].add(child.getMove());

            // When a leaf is reached return the result of the playout
            if (child.isTerminal() || ((!child.simulated && depth > options.sr_depth)) || (options.shot && (budget == 1 || !child.simulated))) {
                result = child.playOut(board);
                child.budget--;
                child.round--;
                child.updateStats(-result);
                child.simulated = true;
            } else {
                // The next child
                result = -child.MCTS(board, depth + 1);
            }
            // set the board back to its previous configuration
            if (!isTerminal())
                board.undoMove();
        } else {
            result = child.stats.mean();
            child.budget--;
            child.round--;
        }
        // Budget for the current round for this arm
        budget--;
        round--;
        if (options.solver) {
            // (Solver) If one of the children is a win, then I'm a loss for the opponent
            if (result == INF) {
                stats.setValue(-INF);
                return result;
            } else if (result == -INF) {
                // Remove from list of unsolved nodes
                if (A != null)
                    removeSolvedArm(child);
                boolean allLoss = true;
                // (Solver) Check if all children are a loss
                for (TreeNode tn : children) {
                    // If the child is not expanded, make sure it is
                    if (tn.isLeaf() && tn.stats.mean() != INF) {
                        // Execute the move represented by the child
                        board.doAIMove(tn.getMove(), player);
                        TreeNode winner = tn.expand(board, depth + 1);
                        board.undoMove();
                        // We found a winning node below the child, this means the child is a loss.
                        if (winner != null) {
                            tn.stats.setValue(-INF);
                            // Remove from list of unsolved nodes
                            if (A != null)
                                removeSolvedArm(tn);
                        }
                    }
                    // Are all children a loss?
                    if (tn.stats.mean() != result)
                        allLoss = false;
                }
                // Are all children a loss?
                if (!allLoss) {
                    // Return a single loss, if not all children are a loss
                    updateStats(1);
                    if (A != null) {
                        // Fix the value of rc to account for removed arms
                        rc = (int) Math.max(1., Math.floor(A.size() / (double) options.rc));
                        resetRound();
                    }
                    return -1;
                }
                // Add the node to the solved children for the parent
                if (parent != null && parent.As != null)
                    parent.As.add(this);
                // (Solver) If all children lead to a loss for me, then I'm a win for the opponent
                stats.setValue(INF);
                return result;
            }
        }
        // Update the results for the current node
        updateStats(result);
        // Back-propagate the result
        return result;
    }

    private TreeNode expand(IBoard board, int depth) {
        expanded = true;
        int winner = board.checkWin();
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        TreeNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        //
        if (children == null)
            children = new ArrayList<TreeNode>(moves.size());
        //
        if (depth <= options.sr_depth) {
            A = new ArrayList<TreeNode>(moves.size());      // Selected nodes
            As = new ArrayList<TreeNode>(moves.size());     // Solved nodes
            S = new ArrayList<TreeNode>(moves.size());      // Unsolved nodes
        }
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        double value;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (board.doAIMove(moves.get(i), player)) {
                TreeNode child = new TreeNode(nextPlayer, moves.get(i), options, this);
                value = 0.;
                if (options.solver) {
                    // Check for a winner, (Solver)
                    winner = board.checkWin();
                    if (winner == player) {
                        value = INF;
                        // This is a win for the expanding node
                        winNode = child;
                    } else if (winner == nextPlayer) {
                        value = -INF;
                    }
                    // Set the value of the child (0 = nothing, +/-INF win/loss)
                    child.stats.setValue(value);
                }
                //
                if (depth <= options.sr_depth) {
                    A.add(child);
                    if (value == INF) // Add solved nodes to the solved set
                        As.add(child);
                    S.add(child);
                }
                children.add(child);
                // reset the board
                board.undoMove();
            }
        }
        //
        if (A != null) {
            // Set the initial remove-count
            rc = (int) Math.max(1., Math.floor(A.size() / (double) options.rc));
            if (options.top_offs)
                newRound();
            else
                newRound_forget();
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private int rootCtr = 0, rc, round = 0;

    private TreeNode select(int depth) {
        if (depth <= options.sr_depth) {
            // Select solved arm first
            for (TreeNode arm : As) {
                if (arm.budget > 0)
                    return arm;
            }
            TreeNode arm;
            // Select any arm with budget > 0
            for (int i = 0; i < A.size(); i++) {
                arm = A.get(rootCtr % A.size());
                rootCtr++;
                if (arm.budget > 0)
                    return arm;
            }
            return null;
        } else {
            return uct.select(children, totVisits);
        }
    }

    private void updateStats(double value) {
        stats.push(value);
        totVisits++;
        roundSimulations++;
        // New round, remove an arm
        if (round == 0 && A != null) {
            // Reset the arms
            for (TreeNode t : A) {
                if (t.A != null) {
                    // Reduce the size of S so unwanted children are not revisited
                    if (options.rec_halving) {
                        // Reduce the size of S
                        int selection = t.children.size(), rr = rootRounds - t.ply;
                        for (int i = 0; i < rr; i++)
                            selection -= (int) Math.max(1., Math.floor(selection / (double) options.rc));
                        // Reduce S if the reduction is smaller than its current size
                        if (selection < t.S.size()) {
                            newSelection(t.S, (options.remove) ? t.S : t.children, selection);
                            // Reset the statistics to match the new set S
                            if (options.stat_reset) {
                                stats.reset();
                                for (TreeNode arm : S)
                                    stats.add(arm.stats, true);
                            }
//                            System.out.println(rootRounds + " " + t.S.size());
                        }
                    } else {
                        Collections.sort(t.S, comparator);
                    }
                    // Return all children to A
                    t.A.clear();
                    t.A.addAll(t.S); // S contains all non-solved and unvisited solved children

                }
                if(options.max_back && rootRounds - t.ply > 0) {
                    stats.reset();
                    double max = Double.NEGATIVE_INFINITY;
                    TreeNode maxT = null;
                    for(TreeNode arm: A) {
                        if(arm.stats.mean() > max) {
                            maxT = arm;
                            max = arm.stats.mean();
                        }
                    }
                    stats.add(maxT.stats, true);
                }
            }
            // Removal policy
            if (A.size() > rc && totVisits >= A.size()) { // Only remove if we have enough visits
                newSelection(A, (options.remove) ? A : S, A.size() - rc);
                rootCtr = 0;
                // New remove count
                rc = (int) Math.max(1., Math.floor(A.size() / (double) options.rc));
            }
            // Start a new round
            if (options.top_offs)
                newRound();
            else
                newRound_forget();
            if(totalBudget == 0)
                throw new RuntimeException("wut");
        }
    }

    private void newSelection(List<TreeNode> C, List<TreeNode> P, int n) {
        Collections.sort(P, comparator);
        if (!options.remove) {
            C.clear();
            int i = 0;
            while (i < n)
                C.add(P.get(i++));
        } else {
            int i = n, N = P.size();
            while (i < N) {
                C.remove(P.size() - 1);
                i++;
            }
        }
    }

    private void resetRound() {
        int b = round;
        // Reset the total budgets for the arms, and the A's
        for (TreeNode t : A)
            b -= t.budget;
        // Divide the budget for the round over the children
        if (b > 0 && A.size() > 0) {
            // Divide the budget over the available arms
            int ctr = rootCtr;
            TreeNode arm;
            // Set new budgets for the arms
            while (b > 0) {
                arm = A.get(ctr % A.size());
                ctr++;
                arm.budget++;
                arm.round++;
                b--;
            }
            // Reset the budgets for the arms
            for (TreeNode t : A) {
                t.totalBudget = t.budget;
                if (t.A != null) {
                    t.resetRound();
                }
            }
        }
    }

    private int initBudget = 0, bPerArm = 0;

    private void newRound() {
        if (A.size() == 1 || S.size() == 1) {
            round = totalBudget;
            bPerArm = totVisits + totalBudget;
        } else {
            if (move == null) {
                round = (int) Math.max(1, Math.floor(totalBudget / Math.ceil((options.rc / 2.) * log2(S.size() - 1.))));
                bPerArm += (int) Math.max(1, Math.floor((initBudget + totalBudget) / (A.size() * Math.ceil((options.rc / 2.) * log2(S.size() - 1.)))));
                rootRounds++;
            } else {
                round = (int) Math.max(1, Math.floor(totalBudget / (log2((options.rc / 2.) * S.size()))));
                bPerArm += (int) Math.max(1, Math.floor((initBudget + totalBudget) / (A.size() * Math.ceil((options.rc / 2.) * log2(S.size())))));
            }
        }
        for (TreeNode t : A) {
            // Reset the budgets of the children
            t.budget = 0;
            t.roundSimulations = 0;
        }
        // Divide the budget for the round over the children
        if (round > 0 && (A.size() > 0 || As.size() > 0)) {
            // Divide the budget over the available arms
            int b = round;
            // First divide over solved nodes that were not seen before
            for (TreeNode arm : As) {
                if (arm.getTotalVisits() > 0)
                    continue;
                arm.budget++;
                b--;
                if (b == 0)
                    break;
            }
            boolean vis = true;
            TreeNode arm;
            int ctr = rootCtr;
            // Set new budgets for the arms
            while (b > 0 && vis) {
                vis = false;
                for (int i = 0; i < A.size(); i++) {
                    arm = A.get(ctr % A.size());
                    ctr++;
                    // Skip over solved arms, they already have some budget
                    if (A.size() > As.size() && arm.stats.mean() == INF)
                        continue;
                    if (arm.totVisits + arm.budget >= bPerArm)
                        continue;
                    vis = true;
                    arm.budget++;
                    b--;
                    if (b == 0)
                        break;
                }
            }
            // Don't spend the rest of the budget
            round -= b;
            //budget -= b;

            if(round == 0) {
                newRound();
                return;
            }
//            if (rootRounds > 1) {
//                // Give the rest of the budget to the empirically best arm
//                A.get(0).budget += b;
//                if(b > 0)
//                    System.out.println(b);
//            } else {
//                // Split the rest evenly
//                ctr = 0;
//                while (b > 0) {
//                    arm = A.get(ctr % A.size());
//                    ctr++;
//                    // Skip over solved arms, they already have some budget
//                    if (A.size() > As.size() && arm.stats.mean() == INF)
//                        continue;
//                    arm.budget++;
//                    b--;
//                    if (b == 0)
//                        break;
//                }
//            }
        }
        // Reset the total budgets for the arms, and the A's
        for (TreeNode t : A) {
            // Reset the total budget for this round
            // totalBudget does not change during the round
            t.totalBudget = t.budget;
            t.initBudget = t.totVisits;
            t.bPerArm = 0;
            if (t.A != null) {
                // Reset the remove counter
                t.rc = (int) Math.floor(t.A.size() / (double) options.rc);
                if (t.rc == 0)
                    t.rc = 1;
                // Start a new round in all children recursively
                t.newRound();
            }
        }
    }

    private void newRound_forget() {
        if (A.size() == 1 || S.size() == 1) {
            round = totalBudget;
            bPerArm = totVisits + totalBudget;
        } else {
            if (move == null) {
                round = (int) Math.max(1, Math.floor(totalBudget / Math.ceil((options.rc / 2.) * log2(S.size() - 1.))));
                rootRounds++;
            } else {
                round = (int) Math.max(1, Math.floor(totalBudget / (log2((options.rc / 2.) * S.size()))));
            }
        }
        // Reset the arms
        for (TreeNode t : A) {
            // Reset the budgets of the children just in case
            t.budget = 0;
            t.roundSimulations = 0;
        }
        // Divide the budget for the round over the children
        if (round > 0 && (A.size() > 0 || As.size() > 0)) {
            int b = round;
            // First divide over solved nodes that were not seen before
            for (TreeNode arm : As) {
                if (arm.getTotalVisits() > 0)
                    continue;
                arm.budget++;
                b--;
                if (b == 0)
                    break;
            }
            // Divide the budget over the available arms
            int ctr = rootCtr;
            TreeNode arm;
            // Set new budgets for the arms
            while (b > 0) {
                arm = A.get(ctr % A.size());
                ctr++;
                // Skip over solved arms, they already have some budget
                if (A.size() > As.size() && arm.stats.mean() == INF)
                    continue;
                arm.budget++;
                b--;
            }
        }
        // Reset the total budgets for the arms, and the A's
        for (TreeNode t : A) {
            // Reset the total budget for this round totalBudget does not change during the round
            t.totalBudget = t.budget;
            if (t.A != null) {
                // Reset the remove counter
                t.rc = (int) Math.max(1., Math.floor(A.size() / (double) options.rc));
                // Start a new round in all children recursively
                t.newRound_forget();
            }
        }
    }

    private double log2(double x) {
        return (Math.log(x) / Math.log(2));
    }

    private final Comparator<TreeNode> comparator = new Comparator<TreeNode>() {
        @Override
        public int compare(TreeNode o1, TreeNode o2) {
            double v1 = o1.stats.mean(), v2 = o2.stats.mean();
            // Place unvisited nodes in the front
            if (o1.totVisits == 0)
                v1 = INF;
            if (o2.totVisits == 0)
                v2 = INF;

            return Double.compare(v2, v1);
        }
    };

    private void removeSolvedArm(TreeNode arm) {
        S.remove(arm);
        if (A.remove(arm)) {
            // See if we can return an arm to A
            if (A.size() + 1 < children.size()) {
                double score = -INF + 1;
                TreeNode returnArm = null;
                for (TreeNode a : children) {
                    // Return a non-solved arm
                    if (!A.contains(a)) {
                        if (a.stats.mean() > score) {
                            score = a.stats.mean();
                            returnArm = a;
                        }
                    }
                }
                // We can return an arm to A that was previously discarded
                if (returnArm != null) {
                    A.add(returnArm);
                    if (!S.contains(returnArm))
                        S.add(returnArm);
                }
            }
        }
    }

    private double playOut(IBoard board) {
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove(), nMoves = 0;
        List<IMove> moves;
        int winner = board.checkWin();
        gameEnded = (winner != IBoard.NONE_WIN);
        IMove currentMove;
        double mastMax, mastVal;
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
                if (options.useHeuristics && options.MAST && MCTSOptions.r.nextDouble() < options.mastEps) {
                    mastMoves.clear();
                    mastMax = Double.NEGATIVE_INFINITY;
                    // Select the move with the highest MAST value
                    for (int i = 0; i < moves.size(); i++) {
                        mastVal = moves.get(i).getHistoryVal(currentPlayer, options);
                        // If bigger, we have a winner, if equal, flip a coin
                        if (mastVal > mastMax) {
                            mastMoves.clear();
                            mastMax = mastVal;
                            mastMoves.add(moves.get(i));
                        } else if (mastVal == mastMax) {
                            mastMoves.add(moves.get(i));
                        }
                    }
                    currentMove = mastMoves.get(MCTSOptions.r.nextInt(mastMoves.size()));
                }

                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {

                    // Keep track of moves made
                    if (options.history && !options.to_history)
                        movesMade[currentPlayer - 1].add(currentMove);

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

        // Update the history values for the moves made during the match
        if (options.history) {
            double p1Score = (winner == IBoard.P1_WIN) ? Math.signum(score) : -Math.signum(score);
            for (int i = 0; i < movesMade[0].size(); i++) {
                options.resetHistory(1, movesMade[0].get(i).getUniqueId(), p1Score);
            }
            for (int i = 0; i < movesMade[1].size(); i++) {
                options.resetHistory(2, movesMade[1].get(i).getUniqueId(), -p1Score);
            }
            // Clear the lists
            movesMade[0].clear();
            movesMade[1].clear();
        }

        return score;
    }

    public TreeNode selectBestMove() {
        // Return a solved node
        if (As.size() > 0)
            return As.get(MCTSOptions.r.nextInt(As.size()));
        // Select from the non-solved arms
        TreeNode bestChild = null;
        double max = Double.NEGATIVE_INFINITY, value;
        List<TreeNode> l = (getTotalVisits() > 0. && A.size() > 0 && options.remove) ? A : children;
        for (TreeNode t : l) {
            if (t.stats.mean() == TreeNode.INF)
                value = TreeNode.INF + MCTSOptions.r.nextDouble();
            else if (t.stats.mean() == -TreeNode.INF)
                value = -TreeNode.INF + t.stats.totalVisits() + MCTSOptions.r.nextDouble();
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

//    private void checkNode(List<TreeNode> list, StatCounter stats) {
//        if (list != null && parent != null && list.size() > 0) {
//            int c = 0, sum = 0, nv = 0;
//            for (TreeNode t : list) {
//                c += t.budget;
//                nv += t.stats.totalVisits();
//                sum += t.stats.m_sum;
//            }
//            double diff = Math.abs(nv - stats.totalVisits());
//            if (diff > 1.)
//                System.out.println("Visits: " + nv + " mine: " + stats.totalVisits());
//            diff = Math.abs(sum + stats.m_sum);
//            if (diff > 1.)
//                System.out.println("Sum: " + sum + " mine: " + -stats.m_sum);
////            if (c != round)
////                System.err.println("??");
//        }
//    }

    public boolean isLeaf() {
        return children == null || !expanded;
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    public IMove getMove() {
        return move;
    }

    public int getArity() {
        return children == null ? 0 : children.size();
    }

    public double getTotalVisits() {
        return totVisits;
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("###,##0.00000");
        return move + "\tVal: " + df2.format(stats.mean()) + "\tVis: " + getTotalVisits() + "\tRVis: " + roundSimulations + "\tBudget: " + budget;
    }
}
