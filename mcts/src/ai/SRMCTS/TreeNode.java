package ai.SRMCTS;

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
    private int totVisits = 0, totalSimulations = 0, roundSimulations = 0, budget = 0, startIndex = -1;
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
        }
        // (Solver) If one of the children is a win, then I'm a win
        if (result == INF) {
            budget--;
            // Remove from list of unsolved nodes
            if (Au != null)
                removeSolvedArm(child);
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
                if (tn.isLeaf()) {
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
                //
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
                    if(value == INF)
                        As.add(child);
                }
                //
                children.add(child);
                // reset the board
                board.undoMove();
            }
        }
        // After expanding the root, set the Successive Rejects parameters
        if (depth == 0) {
            K = getArity();
            k = 1;
            log_k = .0;
            for (int i = 2; i <= K; i++) {
                log_k += 1. / i;
            }
        }
        startIndex = children.indexOf(winNode);
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private double log_k, k;
    private int K;

    private TreeNode select(int depth) {
        if (depth == 0) {
            for (TreeNode arm : Au) {
                // Make sure the budget per arm is spent
                if (arm.budget > 0) {
                    return arm;
                }
            }
            // If no child was returned, the budget for each arm is spent
            budget = (int) Math.ceil((1. / log_k) * ((totalSimulations - K) / (K + 1 - k)));
            k++;
            if (k > 2 && A.size() > 1) {
                removeMinArm();
            }
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
            TreeNode selected = null;
            for (TreeNode arm : Au) {
                // Make sure the budget per arm is spent
                if (arm.budget > 0) {
                    selected = arm;
                    break;
                }
            }
            if (budget == 1 && Au.size() > 1) { // && totVisits > children.size()) {
                removeMinArm();
            }
            return selected;
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
                arm.budget = 0;
                // Divide the rest of the budget if no arm was returned
                divideBudget(budget);
            }
        }
    }

    private void divideBudget(int budget) {
        // All children are solved!
        if (Au.size() == 0 || budget == 0)
            return;
        if (!newRound) {
            // An arm was removed by the solver
            for (TreeNode a : Au) {
                a.budget = 0;
                a.newRound = true;
            }
        }
        //
        int b = budget;
        // First divide over solved nodes
        for(TreeNode arm : As) {
            arm.roundSimulations = 0;
            arm.budget++;
            b--;
            arm.newRound = true;
            if(budget == 0)
                break;
        }
        // Divide the budget at a random position, make sure winning nodes are selected first
        int ctr = MCTSOptions.r.nextInt(Au.size());
        TreeNode arm;
        // Set new budgets for the arms
        while (b > 0) {
            arm = Au.get(ctr % Au.size());
            // Skip over solved arms
            if(Math.abs(arm.stats.mean()) == INF)
                continue;
            arm.roundSimulations = 0;
            arm.budget++;
            b--;
            arm.newRound = true;
            ctr++;
        }
    }

    private void removeMinArm() {
        // Remove an arm
        TreeNode minArm = null;
        double minVal = Double.POSITIVE_INFINITY, val;
        for (TreeNode arm : A) {
            // Throw out solved arms first, these will not be selected anyway
            val = (Math.abs(arm.stats.mean()) == INF) ? -10. : arm.stats.mean();
            if (val < minVal && (Math.abs(arm.stats.mean()) == INF || arm.stats.visits() > 0)) {
                minArm = arm;
                minVal = arm.stats.mean();
            }
            // Reset the stats for the next round
//            arm.stats.reset();
        }
        A.remove(minArm);
        Au.remove(minArm);
        // TODO for debug only
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
        TreeNode bestChild = null;
        double max = Double.NEGATIVE_INFINITY, value;
        List<TreeNode> l = (getnVisits() > 0.) ? A : getChildren();
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
        if (Au != null && getMove() != null) {
            int c = 0;
            for (TreeNode arm : Au) {
                c += arm.budget;
            }
            if (c != budget)
                System.out.println("--");
        }
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
