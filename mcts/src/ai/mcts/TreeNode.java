package ai.mcts;

import ai.FastLog;
import ai.FastSigm;
import ai.StatCounter;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TreeNode {
    public static final double INF = 999999;
    private static final Stack<IMove> movesMade = new Stack<IMove>();
    public static StatCounter[] moveStats = {new StatCounter(), new StatCounter()};
    public static StatCounter[] qualityStats = {new StatCounter(), new StatCounter()};
    public static int myPlayer = 0;
    //
    private final boolean virtual;
    private final MCTSOptions options;
    public int player;
    public StatCounter stats;
    //
    private boolean expanded = false;
    private List<TreeNode> children;
    private IMove move;
    private double velocity = 1.;
    private double imVal = 0.; // implicit minimax value (in view of parent)
    private double imAlpha = -INF - 1; // implicit lower bound (in view of me)
    private double imBeta = +INF + 1;  // implicit upper bound (in view of me)
    private double heval = 0.; // heuristic evaluation for prog. bias (in view of parent)

    /**
     * Constructor for the rootnode
     */
    public TreeNode(int player, MCTSOptions options) {
        this.player = player;
        this.virtual = false;
        this.options = options;
        TreeNode.myPlayer = player;
        stats = new StatCounter();
    }

    /**
     * Initialize a TreeNode with sliding swUCT UCT
     */
    public TreeNode(int player, IMove move, MCTSOptions options, boolean windowed) {
        this.player = player;
        this.move = move;
        this.virtual = false;
        this.options = options;
        stats = new StatCounter(windowed, options);
    }

    public TreeNode(int player, IMove move, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.virtual = false;
        this.options = options;
        stats = new StatCounter();
    }

    public TreeNode(int player, IMove move, final boolean virtual, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.virtual = virtual;
        this.options = options;
        stats = new StatCounter();
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
            child = expand(board, depth + 1);
        }
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal()) // Game is terminal, no more moves can be played
                child = this;
            else
                child = select(board, depth + 1);
        }
        //
        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.stats.mean()) != INF && !child.isTerminal()) {
            // Execute the move represented by the child
            board.doAIMove(child.getMove(), player);
            // When a leaf is reached return the result of the playout
            if (child.getnVisits() == 0) {
                result = child.playOut(board, depth + 1);
                child.updateStats(-result);
            } else {
                // The next child
                result = -child.MCTS(board, depth + 1);
            }
            // Update the MAST value for the move, use original value not the altered reward (signum)
            if (options.useHeuristics && options.MAST)
                options.updateMast(player, child.getMove().getUniqueId(), -1 * Math.signum(result)); // It's the child's reward that counts, hence -result
            // set the board back to its previous configuration
            board.undoMove();
        } else {
            result = child.stats.mean();
        }

        // (Solver) If one of the children is a win, then I'm a win
        if (result == INF) {
            stats.setValue(-INF);
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
                    TreeNode winner = tn.expand(board, depth + 2);
                    board.undoMove();
                    // We found a winning node below the child, this means the child is a loss.
                    if (winner != null) {
                        tn.stats.setValue(-INF);
                    }
                }
                // Are all children a loss?
                if (tn.stats.mean() != result) {
                    // (AUCT) Update the virtual node with a loss
                    if (options.auct && children.get(0).isVirtual()) {
                        TreeNode virtChild = children.get(0);
                        virtChild.stats.push(-1);
                    }
                    // Return a single loss, if not all children are a loss
                    updateStats(1);
                    return -1;
                }
            }
            // (Solver) If all children lead to a loss for the opponent, then I'm a win
            stats.setValue(INF);
            return result;
        }
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
        if (children == null)
            children = new ArrayList<TreeNode>(moves.size());
        // (AUCT) Add an extra virtual node
        if (options.auct) {
            TreeNode vNode = new TreeNode(nextPlayer, null, true, options);
            vNode.stats = stats.copyInv();
            vNode.velocity = velocity;
            children.add(vNode);
        }

        int winner;
        double value, best_imVal = -INF;

        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (!board.isPartialObservable() && board.doAIMove(moves.get(i), player)) {
                TreeNode child;
                // Initialize the child
                if (options.swUCT && depth >= options.minSWDepth)
                    child = new TreeNode(nextPlayer, moves.get(i), options, true);
                else
                    child = new TreeNode(nextPlayer, moves.get(i), options);
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
                // implicit minimax
                if (options.implicitMM) {
                    child.imVal = board.evaluate(player); // view of parent
                    child.imAlpha = -INF - 1;
                    child.imBeta = +INF + 1;
                }
                // prog. bias
                if (options.progBias) {
                    // must be strictly a bonus
                    child.heval = board.evaluate(player);
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
        if (options.implicitMM) {
            this.imVal = -best_imVal;
            this.imAlpha = -INF - 1;
            this.imBeta = +INF + 1;
        }
        // prog. bias
        if (options.progBias) this.heval = -board.evaluate(player);
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private TreeNode select(IBoard board, int depth) {
        TreeNode selected = null;
        double bestValue = Double.NEGATIVE_INFINITY, uctValue, avgValue, ucbVar, Np, Nc;

        // For a chance-move, select a random child
        if (move != null && move.isChance()) {
            return children.get(MCTSOptions.r.nextInt(children.size()));
        }

        // Select a child according to the UCT Selection policy
        for (TreeNode c : children) {
            // Skip virtual nodes
            if (options.auct && c.isVirtual())
                continue;
            // If the game is partial observable, moves in the tree may not be legal
            if (board.isPartialObservable() && !board.isLegal(c.getMove()))
                continue;

            if (c.getnVisits() == 0 || c.stats.mean() == INF) {
                // First, visit all children at least once
                uctValue = INF + MCTSOptions.r.nextDouble();
            } else {
                avgValue = c.stats.mean();
                // Depth discount changes the average value
                if (options.depthDiscount && Math.abs(avgValue) != INF)
                    avgValue *= (1. - Math.pow(options.depthD, depth));
                // Implicit minimax
                if (options.implicitMM) {
                    // changed to be consistent with Mark + Nathan
                    avgValue = (1. - options.imAlpha) * avgValue + (options.imAlpha * c.imVal);
                    // pruning: if the child tree is wasteful (according to the bound info), add a large negative value
                    if (options.imPruning && c.imAlpha >= (c.imBeta - 0.000001)) {
                        //double penalty = (-10 + MCTSOptions.r.nextDouble()*(-50)); 
                        //double penalty = (-10 + MCTSOptions.r.nextDouble()*(-50)); 
                        double penalty = MCTSOptions.r.nextDouble() * (-0.5);
                        avgValue += penalty;
                    }
                }
                // Parent visits can be altered for windowed UCT
                Np = getnVisits();
                Nc = c.getnVisits();
                if (options.swUCT) {
                    if (c.stats.hasWindow() && !stats.hasWindow()) {
                        Np = Math.min(Np, c.stats.windowSize());
                    } else if (!c.stats.hasWindow() && stats.hasWindow()) {
                        Np = stats.totalVisits();
                    }
                }
                // Progressive bias
                if (options.progBias) {
                    avgValue += options.progBiasWeight * c.heval;
                    //avgValue += options.progBiasWeight * c.heval / (c.getnVisits() + 1); 
                    //avgValue += 0.5*c.heval; // <-- this is not prog. bias, but I'm calling it that for now
                }
                //
                if (options.ucbTuned) {
                    ucbVar = c.stats.variance() + Math.sqrt((2. * FastLog.log(Np)) / Nc);
                    uctValue = avgValue + Math.sqrt((Math.min(options.maxVar, ucbVar) * FastLog.log(Np)) / Nc);
                } else {
                    // Compute the uct value with the (new) average value
                    uctValue = avgValue + options.uctC * Math.sqrt(FastLog.log(Np) / Nc);
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

    private int chooseEGreedyEval(IBoard board, List<IMove> moves, int currentPlayer) {
        double roll = MCTSOptions.r.nextDouble();
        double tolerance = 0.0001;

        if (roll < options.egeEpsilon) {
            return MCTSOptions.r.nextInt(moves.size());
        }

        List<IMove> myMoves = new ArrayList<IMove>();
        myMoves.addAll(moves);

        ArrayList<Integer> bestMoveIndices = new ArrayList<Integer>();
        double bestValue = -INF - 1;

        for (int i = 0; i < myMoves.size(); i++) {
            IMove move = myMoves.get(i);
            boolean success = board.doAIMove(move, currentPlayer);

            if (!success)
                continue;

            double eval = board.evaluate(currentPlayer);
            board.undoMove();

            if (eval > bestValue + tolerance) {
                // a clearly better move
                bestMoveIndices.clear();
                bestMoveIndices.add(i);
                bestValue = eval;
            } else if (eval >= (bestValue - tolerance) && eval <= (bestValue + tolerance)) {
                // a tie
                bestMoveIndices.add(i);
                if (eval > bestValue)
                    bestValue = eval;
            }
        }

        assert (bestMoveIndices.size() > 0);
        int idx = MCTSOptions.r.nextInt(bestMoveIndices.size());
        return bestMoveIndices.get(idx);
    }

    @SuppressWarnings("ConstantConditions")
    private double playOut(IBoard board, int depth) {
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove(), moveIndex = -1;
        double mastMax, mastVal, nMoves = 0;
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

                // Select a move from the available ones
                if (options.epsGreedyEval) {
                    // If epsilon greedy play-outs, choose the highest eval
                    moveIndex = chooseEGreedyEval(board, moves, currentPlayer);
                } else if (options.useHeuristics && options.MAST && MCTSOptions.r.nextDouble() < (1. - options.mastEps)) {
                    mastMax = Double.NEGATIVE_INFINITY;
                    // Select the move with the highest MAST value
                    for (int i = 0; i < moves.size(); i++) {
                        mastVal = options.getMastValue(currentPlayer, moves.get(i).getUniqueId());
                        // If bigger, we have a winner, if equal, flip a coin
                        if (mastVal > mastMax || (mastVal == mastMax && MCTSOptions.r.nextDouble() < .5)) {
                            mastMax = mastVal;
                            moveIndex = i;
                        }
                    }
                } else {
                    // Choose randomly
                    moveIndex = MCTSOptions.r.nextInt(moves.size());
                }
                currentMove = moves.get(moveIndex);
                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {

                    // Keep track of moves made for MAST
                    if (options.useHeuristics && options.MAST && !options.TO_MAST)
                        movesMade.push(currentMove);

                    nMoves++;
                    moveMade = true;
                    winner = board.checkPlayoutWin();
                    gameEnded = winner != IBoard.NONE_WIN;
                    currentPlayer = board.getOpponent(currentPlayer);
                    // Check if pdepth is reached
                    if (options.earlyEval && nMoves >= options.pdepth) {
                        terminateEarly = true;
                        break;
                    }
                } else {
                    // The move was illegal, remove it from the list.
                    moveMade = false;
                    moves.remove(moveIndex);
                }
            }
        }

        double score;
        if (gameEnded) {

            if (winner == player) score = 1.0;
            else if (winner == IBoard.DRAW) score = 0.0;
            else score = -1;

            // Update the mast values for the moves made during playout
            if (options.useHeuristics && options.MAST && !options.TO_MAST) {
                double value;
                while (!movesMade.empty()) {
                    currentMove = movesMade.pop();
                    currentPlayer = board.getOpponent(currentPlayer);
                    value = (currentPlayer == player) ? score : -score;
                    options.updateMast(currentPlayer, currentMove.getUniqueId(), value);
                }
            }
            // Alter the score using the relative bonus
            if (winner != IBoard.DRAW) {
                int w = winner - 1;
                // Relative bonus
                double l = board.getNMovesMade();
                // Apply the relative bonus
                if (options.relativeBonus) {
                    if (moveStats[w].variance() > 0. && moveStats[w].totalVisits() >= 50 && options.currentCov.getN() >= 100) {
                        double cStar = -(options.currentCov.getCovariance() / options.currentCov.variance2());
                        score += Math.signum(score) * cStar * ((l - moveStats[w].mean()) / (moveStats[w].stddev()));
                    }
                    // Maintain the average number of moves per play-out
                    moveStats[w].push(l);
                }
                options.currentCov.push((winner == myPlayer) ? l : 0, l);
                // Qualitative bonus
                if (options.qualityBonus) {
                    // Only compute the quality if QB is active, since it may be costly to do so
                    double q = board.getQuality();
                    if (qualityStats[w].variance() > 0) {
                        double qb = q - qualityStats[w].mean();
                        qb /= qualityStats[w].stddev();
                        score += Math.signum(score) * FastSigm.sigm(-options.k * qb);
                    }
                    qualityStats[w].push(q);
                }
            }
        } else if (options.earlyEval && terminateEarly) {
            // playout terminated by nMoves surpassing pdepth

            // FIXME: relative bonus will not work with pdepth
            score = board.evaluate(player);
        } else {
            throw new RuntimeException("Game end error in playOut");
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
            // If the game is partial observable, moves in the tree may not be illegal
            if (board.isPartialObservable() && !board.isLegal(t.getMove()))
                continue;
            // For partial observable games, use the visit count, not the values.
            if (board.isPartialObservable()) {
                value = t.getnVisits();
            } else {
                // If there are children with INF value, choose one of them
                if (t.stats.mean() == INF)
                    value = INF + MCTSOptions.r.nextDouble();
                else if (t.stats.mean() == -INF)
                    value = -INF + t.stats.totalVisits() + MCTSOptions.r.nextDouble();
                else {
                    value = t.stats.totalVisits();
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
        if (!options.auct || isLeaf()) {
            stats.push(value);
        } else {
            // Compute the auct win ratio
            double sum_v = 0., sum_v_r = 0.;
            for (TreeNode c : children) {
                // Due to the solver, there may be loss-nodes,
                // these should not be considered in the average node value
                if (c.stats.mean() == -INF)
                    continue;
                sum_v += c.velocity;
                sum_v_r += c.velocity * c.stats.mean();
            }
            stats.setValue(-1 * (sum_v_r / sum_v));
        }

        // implicit minimax backups
        if (options.implicitMM && children != null) {
            double bestAlpha = -INF - 1;
            double bestBeta = -INF - 1;
            double bestVal = -INF - 1;

            for (TreeNode c : children) {
                if (c.imVal > bestVal) bestVal = c.imVal;
                if ((-c.imBeta) > bestAlpha) bestAlpha = (-c.imBeta);
                if ((-c.imAlpha) > bestBeta) bestBeta = (-c.imAlpha);
            }

            this.imVal = -bestVal;       // view of parent
            this.imAlpha = bestAlpha;    // view of me
            this.imBeta = bestBeta;      // view of me
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
        return children == null || !expanded;
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
        return stats.visits();
    }

    @Override
    public String toString() {
        return move + "\tVisits: " + getnVisits() + "\tValue: " + stats.mean() + "\tvar: " + stats.variance() + "\t\t" + stats.toString();
    }
}
