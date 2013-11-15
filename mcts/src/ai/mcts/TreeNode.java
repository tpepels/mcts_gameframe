package ai.mcts;

import ai.FastLog;
import ai.MovingAverage;
import ai.StatCounter;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    static final FastLog l = new FastLog();
    static final MovingAverage ma = new MovingAverage(250);
    static final double epsilon = 1e-6;
    static final double INF = 999999;
    public static StatCounter moveStats = new StatCounter();
    //
    private final boolean virtual;
    private final MCTSOptions options;
    //
    public int player;
    private List<TreeNode> children;
    private IMove move;
    private StatCounter stats = new StatCounter();
    private double nVisits, totValue, avgValue, velocity = 1., age = 0.;
    private double nMoves = 0.;
    private double imVal = 0.; // implicit minimax value (in view of parent)

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
                result = child.playOut(board);
                // Apply the relative bonus
                if (options.relativeBonus && child.nMoves > 0) {
                    if (child.nMoves < Math.floor(moveStats.mean())) {
                        int x = (int) ((moveStats.mean() - child.nMoves) / stats.stddev());
                        if (x > 0) {
                            result += Math.signum(result) / (options.f(x));
                        }
                    }
                }
                child.nVisits++;
                child.updateStats(-result);
            } else {
                // The next child
                result = -child.MCTS(board, depth + 1);
            }
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
                if (options.auct && tn.isVirtual())
                    continue;
                // If the child is not expanded, make sure it is
                if (options.solverFix && tn.isLeaf()) {
                    // Execute the move represented by the child
                    board.doAIMove(tn.getMove(), player);
                    TreeNode winner = tn.expand(board);
                    board.undoMove();
                    // We found a winning node below the child, this means the child is a loss.
                    if (winner != null) {
                        tn.avgValue = -INF;
                    }
                }
                // Are all children a loss?
                if (tn.avgValue != result) {
                    // (AUCT) Update the virtual node with a loss
                    if (options.auct && children.get(0).isVirtual()) {
                        TreeNode virtChild = children.get(0);
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
        if (options.auct) {
            TreeNode vNode = new TreeNode(nextPlayer, null, true, options);
            vNode.totValue = -totValue; // Switch wins / losses
            vNode.nVisits = nVisits;
            vNode.avgValue = vNode.totValue / vNode.nVisits;
            vNode.velocity = velocity;
            children.add(vNode);
        }
        double value;
        int winner;
        double best_imVal = -INF;
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
                // implicit minimax
                if (options.implicitMM) {
                    child.imVal = board.evaluate(player);
                    if (child.imVal > best_imVal) best_imVal = child.imVal;
                }
                children.add(child);
                // reset the board
                board.undoMove();
            } else if (board.isPartialObservable()) {
                // No move-checking for partial observable games
                // Also, the legality of the move depends on the determinization
                children.add(new TreeNode(nextPlayer, moves.get(i), options));
            }
        }
        // implicit minimax
        if (options.implicitMM) this.imVal = -best_imVal;
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private TreeNode select(IBoard board, int depth) {
        TreeNode selected = null;
        double bestValue = Double.NEGATIVE_INFINITY, uctValue, avgValue, ucbVar;
        // Select a child according to the UCT Selection policy
        for (TreeNode c : children) {
            // Skip virtual nodes
            if (options.auct && c.isVirtual())
                continue;
            // If the game is partial observable, moves in the tree may not be legal
            if (board.isPartialObservable() && !board.isLegal(c.getMove()))
                continue;

            if (c.nVisits == 0) {
                // First, visit all children at least once
                uctValue = INF + options.r.nextDouble();
            } else {
                avgValue = c.avgValue;
                // Depth discount changes the average value
                if (options.depthDiscount && Math.abs(c.avgValue) != INF)
                    avgValue = c.avgValue * (1. - Math.pow(options.depthD, depth));
                // Decay based on the age of the node, older node --> more exploration
                if (options.ageDecay)
                    avgValue *= Math.pow(options.treeDiscount, age);
                // Implicit minimax
                if (options.implicitMM)
                    avgValue = c.avgValue + c.imVal;

                if (!options.ucbTuned) {
                    // Compute the uct value with the (new) average value
                    uctValue = avgValue + (options.uctC * Math.sqrt(l.log(nVisits) / c.nVisits));
                } else {
                    ucbVar = c.stats.variance() + Math.sqrt((2. * l.log(nVisits)) / c.nVisits);
                    uctValue = avgValue + Math.sqrt((Math.min(options.maxVar, ucbVar) * l.log(nVisits)) / c.nVisits);
                }
            }
            // Remember the highest UCT value
            if (uctValue > bestValue) {
                selected = c;
                bestValue = uctValue;
            }
        }
        // (AUCT) Update/decay the velocities
        if (options.auct && selected != null) {
            double sel;
            for (TreeNode c1 : children) {
                sel = (selected.equals(c1)) ? 1. : 0.;
                // Update the node's velocity
                c1.velocity = c1.velocity * options.lambda + sel;
            }
        }
        return selected;
    }

    private double playOut(IBoard board) {
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove(), moveIndex;
        nMoves = 0;
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
                // Select a random move from the available ones
                moveIndex = options.r.nextInt(moves.size());
                currentMove = moves.get(moveIndex);
                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {
                    nMoves++;
                    moveMade = true;
                    winner = board.checkPlayoutWin();
                    gameEnded = winner != IBoard.NONE_WIN;
                    currentPlayer = board.getOpponent(currentPlayer);
                    // Check if pdepth is reached
                    if (options.earlyEval && nMoves >= options.pdepth)
                        break;
                } else {
                    // The move was illegal, remove it from the list.
                    moveMade = false;
                    moves.remove(moveIndex);
                }
            }
        }

        double score = 0;

        if (gameEnded) {
            ma.add(nMoves);
            // Keep track of the average number of moves per play-out
            moveStats.push(nMoves);
            if (winner == player) score = 1.0;
            else if (winner == IBoard.DRAW) score = 0.0;
            else score = -1;
        } else if (options.earlyEval) {
            // playout terminated by nMoves surpassing pdepth

            // FIXME: relative bonus will not work with pdepth
            score = board.evaluate(player);
        }

        // Undo the moves done in the playout
        for (int i = 0; i < nMoves; i++)
            board.undoMove();

        return score;
    }

    public TreeNode getBestChild(IBoard board) {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : children) {
            // (AUCT) Skip virtual children
            if (options.auct && t.isVirtual())
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
                    value = INF + options.r.nextDouble();
                else if (t.avgValue == -INF)
                    value = -INF + t.nVisits;
                else { 
                    value = t.nVisits;
                    // For MCTS solver (Though I still prefer to look at the visits (Tom))
                    //value = t.avgValue + (1. / Math.sqrt(t.nVisits + epsilon));
                }
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
        if (isLeaf() || !options.auct) {
            totValue += value;
            avgValue = totValue / nVisits;
            stats.push(value);
        } else {
            // Compute the auct win ratio
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

        // implicit minimax backups
        if (options.implicitMM && children != null) {
            double bestVal = -INF;
            for (TreeNode c : children)
                if (c.imVal > bestVal) bestVal = c.imVal;
            this.imVal = -bestVal;
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
            if (!options.auct || (isVirtual() || isLeaf())) {
                totValue *= discount;
                nVisits *= discount;
            }
        }
        if (children != null) {
            for (TreeNode c : children) {
                c.discountValues(discount);
            }
            // Due to velocities being reset
            if (options.auct)
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
        if (!options.auct)
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

    public double getnVisits() {
        return nVisits;
    }

    @Override
    public String toString() {
        return move + "\tVisits: " + nVisits + "\tValue: " + avgValue + "\tVelocity: "
                + velocity;
    }
}
