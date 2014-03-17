package ai.SRMCTS;

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

public class TreeNode {
    public static final double INF = 999999;
    public static int myPlayer = 0;
    private static final MoveList[] movesMade = {new MoveList(500), new MoveList(500)};
    //
    private final MCTSOptions options;
    private final TreeNode parent;
    private final UCT uct;
    public final int player;
    public StatCounter stats;
    //
    private boolean expanded = false, simulated = false, newRound = false, removal = false;
    public List<TreeNode> children, A, Au, As;
    private int totVisits = 0, totalSimulations = 0, roundSimulations = 0, budget = 0;
    private IMove move;

    /**
     * Constructor for the root
     */
    public TreeNode(int player, MCTSOptions options, int totalSimulations) {
        this.player = player;
        this.options = options;
        this.totalSimulations = totalSimulations;
        TreeNode.myPlayer = player;
        stats = new StatCounter();
        this.uct = new UCT(options);
        this.parent = null;
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
    }

    /**
     * Run the MCTS algorithm on the given node
     */
    public double MCTS(IBoard board, int depth) {
        // First add some nodes if required
        if (isLeaf())
            expand(board, depth);
        //
        TreeNode child;
        if (isTerminal())       // Game is terminal, no more moves can be played
            child = this;
        else
            child = select(depth);
        //
        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.stats.mean()) != INF && !child.isTerminal()) {
            // Execute the move represented by the child
            board.doAIMove(child.getMove(), player);
            //
            if (options.history)
                movesMade[player - 1].add(child.getMove());
            // When a leaf is reached return the result of the playout
            if (!child.simulated && depth > options.sr_depth) {
                result = child.playOut(board);
                child.updateStats(-result);
                child.simulated = true;
                child.budget--;
            } else {
                // The next child
                result = -child.MCTS(board, depth + 1);
            }
            // set the board back to its previous configuration
            board.undoMove();
        } else {
            result = child.stats.mean();
            child.budget--;
        }
        // (Solver) If one of the children is a win, then I'm a loss for the opponent
        if (result == INF) {
            budget--;
            // Remove from list of unsolved nodes
            if (Au != null) {
                As.add(child);
                removeSolvedArm(child);
            }
            stats.setValue(-INF);
            return result;
        } else if (result == -INF) {
            budget--;
            // Remove from list of unsolved nodes
            if (Au != null)
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
                        if (Au != null)
                            removeSolvedArm(tn);
                    }
                }
                // Are all children a loss?
                if (tn.stats.mean() != result) {
                    // Return a single loss, if not all children are a loss
                    updateStats(1);
                    return -1;
                }
            }
            // (Solver) If all children lead to a loss for me, then I'm a win for the opponent
            stats.setValue(INF);
            return result;
        }
        budget--;
        // Update the results for the current node
        updateStats(result);
        // Back-propagate the result
        return result;
    }

    private TreeNode expand(IBoard board, int depth) {
        expanded = true;
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
            A = new ArrayList<TreeNode>(moves.size());      // All nodes
            Au = new ArrayList<TreeNode>(moves.size());     // Unsolved nodes
            As = new ArrayList<TreeNode>(moves.size());     // Solved nodes
        }
        int winner;
        double value;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (board.doAIMove(moves.get(i), player)) {
                TreeNode child = new TreeNode(nextPlayer, moves.get(i), options, this);
                // Check for a winner, (Solver)
                winner = board.checkWin();
                if (winner == player) {
                    value = INF;
                    // This is a win for the expanding node
                    winNode = child;
                } else if (winner == nextPlayer) {
                    value = -INF;
                } else {
                    value = 0.;
                }
                // Set the value of the child (0 = nothing, +/-INF win/loss)
                child.stats.setValue(value);
                //
                if (depth <= options.sr_depth) {
                    Au.add(child);
                    A.add(child);
                    if (value == INF)
                        As.add(child);
                }
                children.add(child);
                // reset the board
                board.undoMove();
            }
        }
        // After expanding the root, set the Successive Rejects parameters
        if (depth == 0) {
            K = getArity();
            log_k = .5;
            log_n = Math.ceil(FastLog.log(K) / FastLog.log(2));
            if (log_n == 0) // This happens in checkers, when capture is mandatory
                log_n = 1;
            for (int i = 2; i <= K; i++) {
                log_k += 1. / i;
            }
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private double log_k, log_n, k = 1;
    private int K, rootCtr = 0, nk = 0;
    private boolean recursive = false;

    private TreeNode select(int depth) {
        if (depth == 0) {
            TreeNode arm;
            for (int i = 0; i < Au.size(); i++) {
                arm = Au.get(rootCtr % Au.size());
                rootCtr++;
                // Make sure the budget per arm is spent
                if (arm.budget > 0) {
                    recursive = false;
                    return arm;
                }
            }
            newRound = true;
            // If no child was returned, the budget for each arm is spent
            if (k >= getArity())
                budget = 1;
            else if (options.policy == 1) {
                int n = (int) Math.ceil((1. / log_k) * ((totalSimulations - K) / (K + 1 - k)));
                budget = (Au.size() * (n - nk));
                nk = n;
            } else if (options.policy == 2) {
                budget = (int) (totalSimulations / log_n);
            }
            k++;
            // Removal policy
            if (k > 2 && Au.size() > 2 && totVisits > 0) {
                if (options.policy == 1) {
                    if (options.remove) {
                        removeMinArm(false, false);
                    } else {
                        newSelection(Au.size() - 1, true);
                    }
                } else if (options.policy == 2) {
                    if (options.remove) {
                        for (int i = 0; i < (int) (Au.size() / 2.); i++) {
                            removeMinArm(false, false);
                        }
                    } else {
                        newSelection((int) (Au.size() / 2.), true);
                    }
                }
            }
            boolean remove = (k > 2);
            divideBudget(budget, remove);
            if (recursive)
                throw new RuntimeException("Double recursive");
            recursive = true;
            // Do the selection again, this time an arm will be selected
            return select(depth);
        } else if (depth <= options.sr_depth) {
            boolean rem = removal;
            // New round, remove an arm
            if (removal) {
                k++;
                // Removal policy
                if (options.policy == 1 && Au.size() > 1) {
                    if (options.remove) {
                        removeMinArm(false, false);
                    } else {
                        newSelection(Au.size() - 1, true);
                    }
                } else if (options.policy == 2 && Au.size() > 2 && totVisits > (int) (Au.size() / 2.)) {
                    if (options.remove) {
                        for (int i = 0; i < (int) (Au.size() / 2.); i++) {
                            removeMinArm(false, false);
                        }
                    } else {
                        newSelection((int) (Au.size() / 2.), true);
                    }
                }
                removal = false;
            }
            // When a new round starts, redistribute the budget
            if (newRound) {
                divideBudget(budget, rem);
                newRound = false;
            }
            TreeNode arm = null;
            // Select solved arm first
            for (int i = 0; i < As.size(); i++) {
                arm = As.get(i);
                if (arm.budget > 0)
                    break;
            }
            //
            if (arm == null || arm.budget == 0) {
                for (int i = 0; i < Au.size(); i++) {
                    arm = Au.get(rootCtr % Au.size());
                    rootCtr++;
                    // Make sure the budget per arm is spent
                    if (arm.budget > 0)
                        break;
                }
            }
            return arm;
        } else {
            return uct.select(children, totVisits);
        }
    }

    private void removeSolvedArm(TreeNode arm) {
        if (Au.remove(arm)) {
            //stats.subtract(arm.stats);
            double maxVisits = -1;
            TreeNode returnArm = null;
            // See if we can return an arm to Au
            if (Au.size() + 1 < children.size()) {
                for (TreeNode a : children) {
                    // Return a non-solved arm
                    if (Math.abs(a.stats.mean()) != INF && !Au.contains(a)) {
                        if (a.getnVisits() > maxVisits) {
                            maxVisits = a.getnVisits();
                            returnArm = a;
                        }
                    }
                }
            }
            // We can return an arm to Au that was previously discarded
            if (returnArm != null) {
                Au.add(returnArm);
                A.add(returnArm);
                returnArm.budget = arm.budget;
                //stats.add(returnArm.stats);
                arm.budget = 0;
                returnArm.newRound = true;
            } else if (arm.budget > 0) {
                // Divide the rest of the budget if no arm was returned
                divideBudget(arm.budget, false);
                arm.budget = 0;
            }
        }
    }

    private void divideBudget(int b, boolean remove) {
        // All children are solved!
        if (Au.size() == 0 || b == 0)
            return;
        if (newRound) {
            // Only divide the difference
            int c = 0;
            for (TreeNode arm : Au) {
                c += arm.budget;
            }
            b -= c;
        }
        // First divide over solved nodes
        for (TreeNode arm : As) {
            if (arm.getnVisits() > 0)
                continue;
            arm.budget++;
            b--;
            arm.newRound = true;
            if (remove)
                arm.removal = true;
            if (b == 0)
                break;
        }
        // Divide the budget, for the first round, start at a random position
        int ctr = rootCtr;
        TreeNode arm;
        // Set new budgets for the arms
        while (b > 0) {
            arm = Au.get(ctr % Au.size());
            ctr++;
            // Skip over solved arms, they already have some budget
            if (Au.size() > As.size() && arm.stats.mean() == INF)
                continue;
            //
            arm.budget++;
            b--;
            arm.newRound = true;
            if (remove)
                arm.removal = true;
        }
    }

    private void removeMinArm(boolean ucb, boolean skipProtected) {
        // Remove an arm
        TreeNode minArm = null;
        double minVal = Double.POSITIVE_INFINITY, value;
        //
        List<TreeNode> l = (Au.size() > 0) ? Au : A;
        for (TreeNode arm : l) {

            // Throw out solved arms first, these will not be selected anyway
            if (arm.stats.mean() == -INF) {
                minArm = arm;
                break;
            }

            // Skip protected arms
            if (skipProtected && arm.getMove().isProtected())
                continue;

            // Only consider arms we visited this round
            if (arm.totVisits > 0) {
                // To account for different visit counts, we can use UCB
                if (ucb)
                    value = arm.stats.mean() + Math.sqrt(FastLog.log(totVisits) / arm.totVisits);
                else
                    value = arm.stats.mean();
                //
                if (value < minVal) {
                    minArm = arm;
                    minVal = value;
                }
            }
            arm.roundSimulations = 0;
        }
        // Remove from selection
        A.remove(minArm);
        Au.remove(minArm);
        // Subtract the stats of the removed arm from all parents
        TreeNode p = this;
        while (p != null) {
            p.stats.subtract(minArm.stats);
            p = p.parent;
        }
    }

    private void newSelection(int n, final boolean ucb) {
        Collections.sort(children, new Comparator<TreeNode>() {
            @Override
            public int compare(TreeNode o1, TreeNode o2) {
                double v1 = o1.stats.mean(), v2 = o2.stats.mean();
                if(o2.stats.mean() == -INF)
                    return -1;
                if(o1.stats.mean() == -INF)
                    return 1;
                if (ucb) {
                    v1 = v1 + .5 * Math.sqrt(FastLog.log(totVisits) / o1.totVisits);
                    v2 = v2 + .5 * Math.sqrt(FastLog.log(totVisits) / o2.totVisits);
                }
                return Double.compare(v2, v1);
            }
        });
        A.clear();
        Au.clear();
        // stats.reset();
        int i = 0, index = 0;
        while (i < n && index < children.size()) {
            // Skip proven losses
            if(children.get(index).stats.mean() == -INF) {
                index++;
                continue;
            }
            //stats.add(children.get(i).stats);
            A.add(children.get(index));
            Au.add(children.get(index));
            children.get(index).roundSimulations = 0;
            index++;
            i++;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private double playOut(IBoard board) {
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove(), moveIndex, nMoves = 0;
        List<IMove> moves;
        int winner = board.checkWin();
        gameEnded = (winner != IBoard.NONE_WIN);
        IMove currentMove;
        boolean terminateEarly = false;
        while (!gameEnded && !terminateEarly) {
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
                moveIndex = MCTSOptions.r.nextInt(moves.size());
                currentMove = moves.get(moveIndex);
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
                    moves.remove(moveIndex);
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

    private void updateStats(double value) {
        stats.push(value);
        totVisits++;
        roundSimulations++;
    }

    public TreeNode selectBestMove() {
        // Return a solved node
        if (As.size() > 0)
            return As.get(MCTSOptions.r.nextInt(As.size()));
        // Select from the non-solved arms
        TreeNode bestChild = null;
        double max = Double.NEGATIVE_INFINITY, value;
        List<TreeNode> l = (getnVisits() > 0. && Au.size() > 0 && options.remove) ? Au : children;
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

//    private void checkNode() {
//        if (Au != null && parent != null) {
//            int c = 0, sum = 0, nv = 0;
//            for (TreeNode t : Au) {
//                c += t.budget;
//                nv += t.stats.totalVisits();
//                sum += t.stats.m_sum;
//            }
//            double diff = Math.abs(nv - stats.totalVisits());
//            if(diff >= 1.)
//                System.out.println("Visits: " + nv + " mine: " + stats.totalVisits());
//            diff = Math.abs(sum + stats.m_sum);
//            if(diff >= 1.)
//                System.out.println("Sum: " + sum + " mine: " + -stats.m_sum);
//            if (c != budget)
//                System.err.println("??");
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

    public List<TreeNode> getChildren() {
        return children;
    }

    public int getArity() {
        return children == null ? 0 : children.size();
    }

    public double getnVisits() {
        return totVisits;
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("###,##0.00000");
        return move + "\tValue: " + df2.format(stats.mean()) + "\tVisits: " + getnVisits() + "\tRound sims: " + roundSimulations + "\tBudget: " + budget;
    }
}
