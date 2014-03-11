package ai.SRMCTS;

import ai.FastLog;
import ai.StatCounter;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import ai.mcts.MCTSOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    public static final double INF = 999999;
    public static int myPlayer = 0;
    //
    private final MCTSOptions options;
    private final UCT uct;
    public final int player;
    public StatCounter stats;
    //
    private boolean expanded = false, simulated = false, newRound = false;
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
    }

    /**
     * Constructor for internal node
     */
    public TreeNode(int player, IMove move, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.stats = new StatCounter();
        this.uct = new UCT(options);
    }

    /**
     * Run the MCTS algorithm on the given node
     */
    public double MCTS(IBoard board, int depth) {
        // First add some leafs if required
        if (isLeaf()) {
            expand(board, depth);
        }
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
            // When a leaf is reached return the result of the playout
            if (!child.simulated && depth >= options.sr_depth) {
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
        // (Solver) If one of the children is a win, then I'm a win
        if (result == INF) {
            budget--;
            // Remove from list of unsolved nodes
            if (Au != null)
                removeSolvedArm(child);
            if (As != null)
                As.add(child);
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
                    if (winner != null && winner.stats.mean() == -INF) {
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
            // (Solver) If all children lead to a loss for the opponent, then I'm a win
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
                TreeNode child = new TreeNode(nextPlayer, moves.get(i), options);
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
            log_k = .0;
            for (int i = 2; i <= K; i++) {
                log_k += 1. / i;
            }
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private double log_k, k = 1;
    private int K, ctr = -1, rootCtr = 0;

    private TreeNode select(int depth) {
        if (depth == 0) {
            TreeNode arm;
            for (int i = 0; i < Au.size(); i++) {
                arm = Au.get(rootCtr % Au.size());
                rootCtr++;
                // Make sure the budget per arm is spent
                if (arm.budget > 0)
                    return arm;
            }
            newRound = true;
            // Make sure we don't go over the arity
            if (k == children.size())
                k = (double) getArity() - 1;
            // If no child was returned, the budget for each arm is spent
            budget = (int) Math.ceil((1. / log_k) * ((totalSimulations - K) / (K + 1 - k)));
            k++;
            //
            if (k > 2 && A.size() > 2) {
                removeMinArm(false);
                resetStats(depth);
            }
            //
            if (As.size() > 0)
                return As.get(MCTSOptions.r.nextInt(As.size()));
            //
            if (Au.size() == 0) {
                // Abandon ship, all is lost!
                return children.get(0);
            }
            divideBudget(budget);
            // Do the selection again, this time an arm will be selected
            return select(depth);
        } else if (depth <= options.sr_depth) {
            // When a new round starts, redistribute the budget
            if (newRound) {
                divideBudget(budget);
                newRound = false;
            }
            TreeNode arm = null;
            for (int i = 0; i < Au.size(); i++) {
                arm = Au.get(rootCtr % Au.size());
                rootCtr++;
                // Make sure the budget per arm is spent
                if (arm.budget > 0)
                    break;
            }
            //
            if (budget == 1)
                k++;
            //
            if (totVisits > k * Au.size()) {
                if (Au.size() > options.sr_c && k % options.sr_c == 0) {
                    for (int i = 0; i < options.sr_c; i++) {
                        removeMinArm(false);
                    }
                    resetStats(depth);
                } else if (Au.size() > 1 && Au.size() < options.sr_c) {
                    removeMinArm(false);
                    resetStats(depth);
                }
            }
            return arm;
        } else {
            return uct.select(children, totVisits);
        }
    }

    private void removeSolvedArm(TreeNode arm) {
        if (Au.remove(arm)) {
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
                arm.budget = 0;
                returnArm.newRound = true;
            } else if (arm.budget > 0) {
                // Divide the rest of the budget if no arm was returned
                divideBudget(arm.budget);
                arm.budget = 0;
            }
        }
    }

    private void divideBudget(int b) {
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
            if (newRound)
                arm.roundSimulations = 0;
            arm.budget++;
            b--;
            arm.newRound = true;
            if (b == 0)
                break;
        }
        // Divide the budget, for the first round, start at a random position
        ctr = (ctr < 0) ? MCTSOptions.r.nextInt(Au.size()) : ctr;
        TreeNode arm;
        // Set new budgets for the arms
        while (b > 0) {
            arm = Au.get(ctr % Au.size());
            ctr++;
            // Skip over solved arms, they already have some budget
            if (Au.size() > As.size() && arm.stats.mean() == INF)
                continue;
            //
            if (newRound)
                arm.roundSimulations = 0;
            //
            arm.budget++;
            b--;
            arm.newRound = true;
        }
    }

    private void removeMinArm(boolean ucb) {
        // Remove an arm
        TreeNode minArm = null;
        double minVal = Double.POSITIVE_INFINITY, value;
        List<TreeNode> l = (Au.size() > 0) ? Au : A;
        for (TreeNode arm : l) {
            // Throw out solved arms first, these will not be selected anyway
            if (arm.stats.mean() == -INF) {
                minArm = arm;
                break;
            } else if (arm.stats.visits() > 0) {
                if (ucb)
                    value = arm.stats.mean() + Math.sqrt(FastLog.log(totVisits) / arm.getnVisits());
                else
                    value = arm.stats.mean();
                if (value < minVal) {
                    minArm = arm;
                    minVal = value;
                }
            }

            roundSimulations = 0;
        }
        A.remove(minArm);
        Au.remove(minArm);
    }

    private void resetStats(int depth) {
        // Don't reset the learned statistics
        if (depth == options.sr_depth)
            return;
        // Reset the stats for the arms that are not solved
        for (TreeNode arm : A) {
            if (Math.abs(arm.stats.mean()) != INF)
                arm.stats.reset();
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
        return score;
    }

    public TreeNode selectBestMove() {
        // Return a solved node
        if (As.size() > 0)
            return As.get(MCTSOptions.r.nextInt(As.size()));
        // Select from the non-solved arms
        TreeNode bestChild = null;
        double max = Double.NEGATIVE_INFINITY, value;
        List<TreeNode> l = (getnVisits() > 0. && Au.size() > 0) ? Au : getChildren();
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

    private void updateStats(double value) {
        stats.push(value);
        totVisits++;
        roundSimulations++;
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
