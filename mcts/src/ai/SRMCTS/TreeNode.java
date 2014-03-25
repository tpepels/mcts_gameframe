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
            if ((options.depth_limited && depth == options.sr_depth) ||
                    (!options.depth_limited && (!child.simulated && depth > options.sr_depth)) ||
                    child.isTerminal()) {
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
                    if (tn.stats.mean() != result) {
                        if (A != null) {
                            // Fix the value of rc to account for removed arms
                            rc = (int) (A.size() / (double) options.rc);
                            if (rc == 0)
                                rc = 1;
                            resetRound();
                        }
                        // Return a single loss, if not all children are a loss
                        updateStats(1);
                        return -1;
                    }
                }
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
                    if (value == INF)
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
            rc = (int) (A.size() / (double) options.rc);
            if (rc == 0)
                rc = 1;
            newRound();
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
        checkNode();
        // New round, remove an arm
        if (round == 0 && A != null) {
            // Removal policy
            if (A.size() > rc) {
                newSelection(A.size() - rc, options.remove);
                rc = (int) (A.size() / (double) options.rc);
                if (rc == 0)
                    rc = 1;
            }
            newRound();
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
                if (t.A != null) {
                    t.resetRound();
                }
            }
        }

    }

    private void newRound() {

        if (S.size() > 1) {
            if (move == null)
                round = (int) Math.ceil((rc * totalBudget) / (S.size() - 1));
            else
                round = (int) Math.ceil((rc * totalBudget) / S.size());
        } else
            round = totalBudget;

        // Reset the arms
        for (TreeNode t : A) {
            if (t.A != null) {
                // Return all children to A
                t.A.clear();
                t.A.addAll(t.S); // S contains all non-solved and unvisited solved children
            }
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
            // Reset the total budget for this round
            // totalBudget does not change during the round
            t.totalBudget = t.budget;
            if (t.A != null) {
                // Reset the remove counter
                t.rc = (int) (t.A.size() / (double) options.rc);
                if (t.rc == 0)
                    t.rc = 1;
                // Start a new round in all children recursively
                t.newRound();
            }
        }
    }

    private void newSelection(int n, boolean remove) {
        if (Math.abs(stats.mean()) == INF)
            return;
        if (!remove) {
            Collections.sort(S, new Comparator<TreeNode>() {
                @Override
                public int compare(TreeNode o1, TreeNode o2) {
                    double v1 = o1.stats.mean(), v2 = o2.stats.mean();
                    if (o1.totVisits == 0)
                        v1 = 1;
                    if (o2.totVisits == 0)
                        v2 = 1;

                    return Double.compare(v2, v1);
                }
            });
            A.clear();
            int i = 0, index = 0;
            while (i < n && index < S.size()) {
                A.add(S.get(index));
                index++;
                i++;
            }
        } else {
            Collections.sort(A, new Comparator<TreeNode>() {
                @Override
                public int compare(TreeNode o1, TreeNode o2) {
                    double v1 = o1.stats.mean(), v2 = o2.stats.mean();
                    if (o1.totVisits == 0)
                        v1 = 1;
                    if (o2.totVisits == 0)
                        v2 = 1;

                    return Double.compare(v2, v1);
                }
            });
            for (int i = n; i < A.size(); i++)
                A.remove(i);
        }
    }

    private void removeSolvedArm(TreeNode arm) {
        S.remove(arm);
        if (A.remove(arm)) {
            // See if we can return an arm to A
            if (A.size() < S.size()) {
                double maxVisits = -1;
                TreeNode returnArm = null;
                for (TreeNode a : S) {
                    // Return a non-solved arm
                    if (!A.contains(a)) {
                        if (a.getTotalVisits() > maxVisits) {
                            maxVisits = a.getTotalVisits();
                            returnArm = a;
                        }
                    }
                }
                // We can return an arm to A that was previously discarded
                if (returnArm != null)
                    A.add(returnArm);
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
//        List<TreeNode> l = (getTotalVisits() > 0. && A.size() > 0) ? A : children;
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

    private void checkNode() {
        if (A != null && parent != null && A.size() > 0) {
            int c = 0, sum = 0, nv = 0;
            for (TreeNode t : A) {
                c += t.budget;
                nv += t.stats.totalVisits();
                sum += t.stats.m_sum;
            }
//            double diff = Math.abs(nv - stats.totalVisits());
//            if (diff >= 1.)
//                System.out.println("Visits: " + nv + " mine: " + stats.totalVisits());
//            diff = Math.abs(sum + stats.m_sum);
//            if (diff >= 1.)
//                System.out.println("Sum: " + sum + " mine: " + -stats.m_sum);
            if (c != round)
                System.err.println("??");
        }
    }

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
