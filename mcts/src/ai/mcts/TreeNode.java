package ai.mcts;

import ai.FastLog;
import ai.FastRandom;
import ai.framework.IBoard;
import ai.framework.IMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TreeNode {
    static final Random r = new FastRandom();
    static final FastLog l = new FastLog();
    static final double epsilon = 1e-6;
    static final double INF = 999999;
    //
    private final boolean virtual;
    private final MCTSOptions options;
    public int player;
    private List<TreeNode> children;
    private IMove move;
    private double nVisits, totValue, avgValue, velocity = 1.;

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

    /**
     * Run the MCTS algorithm on the given node.
     *
     * @param board The current board
     * @param n     the current tree node
     * @return the currently evaluated rollout value of the node
     */
    public static double MCTS(IBoard board, TreeNode n) {
        TreeNode child = null;
        // First add some leafs if required
        if (n.isLeaf()) {
            // Expand returns any node that leads to a win
            child = n.expand(board);
        }
        n.nVisits++;
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (n.isTerminal()) // Game is a draw
                child = n;
            else
                child = n.select();
        }
        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.avgValue) != INF && !child.isTerminal()) {
            // Execute the move represented by the child
            board.doAIMove(child.getMove(), n.player);
            // When a leaf is reached return the result of the playout
            if (child.nVisits == 0) {
                result = child.playOut(board.copy());
                child.nVisits++;
                child.updateStats(-result);
            } else { // Select the next child in the tree
                result = -MCTS(board, child);
            }
            // set the board back to its previous configuration
            board.undoMove();
        } else {
            result = child.avgValue;
        }

        // (Solver) If one of the children is a win, then I'm a win
        if (result == INF) {
            n.avgValue = -INF;
            return result;
        } else if (result == -INF) {
            // (Solver) Check if all children are a loss
            for (TreeNode tn : n.children) {
                // (AUCT) Skip virtual child
                if (tn.isVirtual())
                    continue;
                // Are all children a loss?
                if (tn.avgValue != result) {
                    // (AUCT) Update the virtual node with a loss
                    TreeNode virtChild = n.children.get(0);
                    if (virtChild.isVirtual()) {
                        virtChild.totValue--;
                        virtChild.nVisits++;
                        virtChild.avgValue = virtChild.totValue / virtChild.nVisits;
                    }
                    // Return a single loss, if not all children are a loss
                    n.updateStats(-1);
                    return -1;
                }
            }
            // (Solver) If all children lead to a loss, then I'm a loss
            n.avgValue = INF;
            return result;

        }
        // Update the results for the current node
        n.updateStats(result);
        return result;
    }

    private TreeNode expand(IBoard board) {
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        TreeNode winNode = null;
        // Generate all moves
        IMove[] moves = board.getExpandMoves();
        children = new ArrayList<TreeNode>(moves.length);

        // (AUCT) Add an extra virtual node
        if (options.accelerated) {
            TreeNode vNode = new TreeNode(nextPlayer, null, true, options);
            vNode.totValue = -totValue; // Switch wins / losses
            vNode.nVisits = nVisits;
            vNode.avgValue = vNode.totValue / vNode.nVisits;
            vNode.velocity = velocity;
            children.add(vNode);
        }

        // Add all moves as children to the current node
        for (IMove move1 : moves) {
            double value = 0;
            if (board.doAIMove(move1, player)) {
                TreeNode child = new TreeNode(nextPlayer, move1, options);
                // Check for a winner, (Solver)
                final int winner = board.checkWin();
                // Only the player to move can win
                if (winner == player) {
                    value = INF;
                    // This is a win for the expanding node
                    winNode = child;
                }
                // Set the value of the child (0 = nothing, +/-INF win/loss)
                child.totValue = value;
                child.avgValue = value;
                children.add(child);
                // reset the board
                board.undoMove();
            }
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private TreeNode select() {
        TreeNode selected = null;
        // Below a threshold, select a random child
        if (nVisits < children.size()) {
            selected = children.get(r.nextInt(children.size()));
            if (options.accelerated) {
                while (selected.isVirtual()) {
                    selected = children.get(r.nextInt(children.size()));
                }
            }
            return selected;
        }
        //
        double bestValue = Double.NEGATIVE_INFINITY;
        // Select a child according to the UCT Selection policy
        for (TreeNode c : children) {
            // Skip virtual nodes
            if (options.accelerated && c.isVirtual())
                continue;

            double uctValue = c.avgValue + Math.sqrt(l.log(nVisits + 1) / (c.nVisits + epsilon));
            //
            if (uctValue > bestValue) {
                selected = c;
                bestValue = uctValue;
            }
        }
        if (options.accelerated && selected != null) {
            // (AUCT) Update all the children's velocities
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
        int winner, currentPlayer = board.getPlayerToMove(), moveIndex;
        IMove currentMove;
        //
        List<IMove> moves;
        winner = board.checkWin();
        gameEnded = winner != IBoard.NONE_WIN;
        while (!gameEnded) {
            moves = board.getPlayoutMoves();
            moveMade = false;
            while (!moveMade) {
                // All moves were thrown away, the pentalath.game is a draw
                if (moves.size() == 0) {
                    gameEnded = true;
                    // The current player has no moves left
                    // TODO, different games have different rules for this
                    if (board.drawPossible())
                        winner = IBoard.DRAW;                               // Pentalath (,chinese checkers) ?
                    else
                        winner = board.getOpponent(board.getPlayerToMove()); // Cannon
                    break;
                }
                moveIndex = r.nextInt(moves.size());
                currentMove = moves.get(moveIndex);
                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {
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

        if (winner == player) {
            return 1;
        } else if (winner == IBoard.DRAW) {
            return 0;
        } else {
            return -1;
        }
    }

    public TreeNode getBestChild() {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : children) {
            // (AUCT) Skip virtual children
            if (options.accelerated && t.isVirtual())
                continue;
            // Add a small number to the visits in case nVisits = 0
            value = t.avgValue + (1. / Math.sqrt(t.nVisits + epsilon));
            if (value > max) {
                max = value;
                bestChild = t;
            }
            // For debugging
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

    public void discountValues(double discount) {
        if (Math.abs(avgValue) != INF) {
            // In auct we only need to update leafs or virtual nodes
            if ((options.accelerated && (isVirtual() || isLeaf())) || !options.accelerated) {
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
        return "Move: " + move + " Visits: " + nVisits + " Value: " + avgValue + " Velocity: "
                + velocity;
    }
}