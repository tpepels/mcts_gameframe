package ai.mcts;

import ai.FastLog;
import ai.FastRandom;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class TreeNode {
    static final Random r = new FastRandom();
    static final FastLog l = new FastLog();
    static final double epsilon = 1e-6;
    static final double INF = 999999;
    static final Stack<IMove> movesMade = new Stack<IMove>();
    //
    private final boolean virtual;
    private final MCTSOptions options;
    //
    public int player;
    private List<TreeNode> children;
    private IMove move;
    private double nVisits, totValue, avgValue, velocity = 1., age = 0.;
    private int moveIndex;
    private double nMoves = 0.;

    public TreeNode(int player, MCTSOptions options) {
        this.player = player;
        this.virtual = false;
        this.options = options;
    }

    public TreeNode(int player, IMove move, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.virtual = false;
        this.options = options;
    }

    public TreeNode(int player, IMove move, final boolean virtual, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.virtual = virtual;
        this.options = options;
    }

    private static double log(double x, int base) {
        if (x == 0)
            return 0;
        return Math.log(x) / Math.log(base);
    }

    public double getnVisits() {
        return nVisits;
    }

    /**
     * Run the MCTS algorithm on the given node.
     *
     * @param board The current board
     * @return the currently evaluated playout value of the node
     */
    public double MCTS(IBoard board, int depth) {
        TreeNode child = null;
        // First add some leafs if required
        if (isLeaf()) {
            // Expand returns any node that leads to a win
            child = expand(board);
        }
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal()) // Game is terminal, no more moves can be played
                child = this;
            else
                child = select(board, depth + 1);
        }
        nVisits++;
        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.avgValue) != INF && !child.isTerminal()) {
            // Execute the move represented by the child
            board.doAIMove(child.getMove(), player);
            // When a leaf is reached return the result of the playout
            if (child.nVisits == 0) {
                result = child.playOut(board.copy());
                // Apply the relative bonus
                if (options.relativeBonus && child.nMoves > 0) {
                    if (options.includeDepth)
                        result += Math.signum(result) / (options.k * l.log(((double) depth + 1.) * (child.nMoves + 1.)));
                    else
                        result += Math.signum(result) / (options.k * l.log(child.nMoves + 1.));
                }
                //
                child.nVisits++;
                child.updateStats(-result);
            } else {
                // The next child
                result = -child.MCTS(board, depth + 1);
            }
            // Update the mastEnabled value for the move
            if (options.mastEnabled)
                options.updateMast(player, child.getMove().getUniqueId(), -result); // It's the child's reward that counts, hence -result

            // set the board back to its previous configuration
            board.undoMove();
        } else {
            result = child.avgValue;
        }

        // (Solver) If one of the children is a win, then I'm a win
        if (result == INF) {
            avgValue = -INF;
            return result;
        } else if (result == -INF) {
            // (Solver) Check if all children are a loss
            for (TreeNode tn : children) {
                // (AUCT) Skip virtual child
                if (tn.isVirtual())
                    continue;

                // If the child is not expanded, make sure it is
                if (options.solverFix && tn.isLeaf()) {
                    // Execute the move represented by the child
                    board.doAIMove(tn.getMove(), player);
                    TreeNode winner = tn.expand(board);
                    board.undoMove();
                    // We found a winning node below the child,
                    // this means the child is a loss.
                    if (winner != null) {
                        tn.avgValue = -INF;
                    }
                }

                // Are all children a loss?
                if (tn.avgValue != result) {
                    // (AUCT) Update the virtual node with a loss
                    TreeNode virtChild = children.get(0);
                    if (virtChild.isVirtual()) {
                        virtChild.totValue--;
                        virtChild.nVisits++;
                        virtChild.avgValue = virtChild.totValue / virtChild.nVisits;
                    }
                    // Return a single loss, if not all children are a loss
                    updateStats(1);
                    return -1;
                }
            }
            // (Solver) If all children lead to a loss for the opponent, then I'm a win
            avgValue = INF;
            return result;

        }
        // Update the results for the current node
        updateStats(result);
        // Back-propagate the result
        return result;
    }

    private TreeNode expand(IBoard board) {
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        TreeNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        children = new ArrayList<TreeNode>(moves.size());
        // (AUCT) Add an extra virtual node
        if (options.accelerated) {
            TreeNode vNode = new TreeNode(nextPlayer, null, true, options);
            vNode.totValue = -totValue; // Switch wins / losses
            vNode.nVisits = nVisits;
            vNode.avgValue = vNode.totValue / vNode.nVisits;
            vNode.velocity = velocity;
            children.add(vNode);
        }
        double value;
        int winner;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (!board.isPartialObservable() && board.doAIMove(moves.get(i), player)) {
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
                child.totValue = value;
                child.avgValue = value;
                children.add(child);
                // reset the board
                board.undoMove();
            } else if (board.isPartialObservable()) {
                // No move-checking for partial observable games
                // Also, the legality of the move depends on the determinization
                children.add(new TreeNode(nextPlayer, moves.get(i), options));
            }
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private TreeNode select(IBoard board, int depth) {
        TreeNode selected = null;
        double bestValue = Double.NEGATIVE_INFINITY, uctValue, avgValue;
        // Select a child according to the UCT Selection policy
        for (TreeNode c : children) {
            // Skip virtual nodes
            if (options.accelerated && c.isVirtual())
                continue;
            // If the game is partial observable, moves in the tree may not be legal
            if (board.isPartialObservable() && !board.isLegal(c.getMove()))
                continue;
            // First, visit all children at least once
            if (c.nVisits == 0) {
                uctValue = INF + r.nextDouble();
            } else {
                // Depth discount changes the average value
                if (options.depthDiscount && Math.abs(c.avgValue) != INF)
                    avgValue = c.avgValue * (1. - Math.pow(options.depthD, depth));
                else
                    avgValue = c.avgValue;

                // Decay based on the age of the node, older node --> more exploration
                if (options.ageDecay)
                    avgValue *= Math.pow(options.treeDiscount, age);

                // Compute the uct value with the (new) average value
                uctValue = avgValue + (options.uctC * Math.sqrt(l.log(nVisits + 1) / (c.nVisits + epsilon)));
            }
            //
            if (uctValue > bestValue) {
                selected = c;
                bestValue = uctValue;
            }
        }
        // (AUCT) Update/decay the velocities
        if (options.accelerated && selected != null) {
            double sel;
            for (TreeNode c1 : children) {
                sel = (selected.equals(c1)) ? 1. : 0.;
                // Update the node's velocity
                c1.velocity = c1.velocity * options.lambda + sel;
            }
        }
        return selected;
    }

    private int playOut(IBoard board) {
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove();
        nMoves = 0;
        //
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
                    if (board.drawPossible())
                        winner = IBoard.DRAW;                                   // Pentalath, Lost Cities
                    else                                                        // Last player to move:
                        winner = board.getOpponent(board.getPlayerToMove());    // Cannon, Amazons, Chinese Checkers
                    break;
                }
                // Select a move according to the MAST values
                if (options.mastEnabled && (r.nextDouble() < (1. - options.mastEpsilon))) {
                    double mastMax = Double.NEGATIVE_INFINITY;
                    // Select the move with the highest MAST value
                    for (int i = 0; i < moves.size(); i++) {
                        double mastVal = options.getMastValue(currentPlayer, moves.get(i).getUniqueId());
                        // If bigger, we have a winner, if equal, flip a coin
                        if (mastVal > mastMax || (mastVal == mastMax && r.nextDouble() < .5)) {
                            mastMax = mastVal;
                            moveIndex = i;
                        }
                    }
                } else {
                    // Select a random move from the available ones
                    moveIndex = r.nextInt(moves.size());
                }
                currentMove = moves.get(moveIndex);
                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {
                    // Remember the moves made, to update their mast values later
                    if (options.mastEnabled && !options.treeOnlyMast)
                        movesMade.push(currentMove);
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
        int val;
        if (winner == player) {
            val = 1;
        } else if (winner == IBoard.DRAW) {
            val = 0;
        } else {
            val = -1;
        }
        // Update the mast values for the moves made during playout
        if (options.mastEnabled && !options.treeOnlyMast) {
            double value;
            while (!movesMade.empty()) {
                currentMove = movesMade.pop();
                currentPlayer = board.getOpponent(currentPlayer);
                value = (currentPlayer == player) ? val : -val;
                options.updateMast(currentPlayer, currentMove.getUniqueId(), value);
            }
        }
        return val;
    }

    public TreeNode getBestChild(IBoard board) {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : children) {
            // (AUCT) Skip virtual children
            if (options.accelerated && t.isVirtual())
                continue;
            // If the game is partial observable, moves in the tree may not be legal
            if (board.isPartialObservable() && !board.isLegal(t.getMove()))
                continue;
            // For partial observable games, use the visit count, not the values.
            if (board.isPartialObservable()) {
                value = t.nVisits;
            } else {
                // If there are children with INF value, choose on of them
                if (t.avgValue == INF)
                    value = INF + r.nextDouble();
                else
                    //value = t.nVisits;
                    // For MCTS solver (Though I still prefer to look at the visits (Tom))
                    value = t.avgValue + (1. / Math.sqrt(t.nVisits + epsilon));
            }
            //
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
        // If we are not using AUCT simply add the total value
        if (isLeaf() || !options.accelerated) {
            totValue += value;
            avgValue = totValue / nVisits;
        } else {
            // Compute the accelerated win ratio
            double sum_v = 0., sum_v_r = 0.;
            for (TreeNode c : children) {
                // Due to the solver, there may be loss-nodes,
                // these should not be considered in the average node value
                if (c.avgValue == -INF)
                    continue;
                sum_v += c.velocity;
                sum_v_r += c.velocity * c.avgValue;
            }
            avgValue = -1 * (sum_v_r / sum_v);
        }
    }

    public void ageSubtree() {
        // Increase the age of this node
        age++;
        // And of its children
        if (children != null) {
            for (TreeNode c : children) {
                c.ageSubtree();
            }
        }
    }

    public void discountValues(double discount) {
        if (Math.abs(avgValue) != INF) {
            // In auct we only need to update leafs or virtual nodes
            if (!options.accelerated || (isVirtual() || isLeaf())) {
                totValue *= discount;
                nVisits *= discount;
            }
        }
        if (children != null) {
            for (TreeNode c : children) {
                c.discountValues(discount);
            }
            // Due to velocities being reset
            if (options.accelerated)
                updateStats(0);
        }
    }

    private double getEntropy(double value) {
        return (-value * log(value, 2)) - ((1. - value) * log(1. - value, 2));
    }

    public void resetVelocities() {
        velocity = 1.;
        if (children != null) {
            for (TreeNode c : children) {
                c.resetVelocities();
            }
            // Due to velocities being reset
            if (!isLeaf())
                updateStats(0);
        }
    }

    public boolean isLeaf() {
        return children == null;
    }

    public boolean isTerminal() {
        if (!options.accelerated)
            return children != null && children.size() == 0;
        else
            return children != null && children.size() == 1;
    }

    public int getArity() {
        return children == null ? 0 : children.size();
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public IMove getMove() {
        return move;
    }

    public boolean isVirtual() {
        return virtual;
    }

    @Override
    public String toString() {
        return move + "\tVisits: " + nVisits + "\tValue: " + avgValue + "\tVelocity: "
                + velocity;
    }
}