package ai.mcts;

import ai.MCTSOptions;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastLog;
import framework.util.FastSigm;
import framework.util.StatCounter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TreeNode {
    public static final double INF = 999999;
//    private static final MoveList[] movesMade = {new MoveList(500), new MoveList(500)};
//    private static final MoveList mastMoves = new MoveList(100);
    public static StatCounter[] moveStats = {new StatCounter(), new StatCounter()};
    public static StatCounter[] qualityStats = {new StatCounter(), new StatCounter()};
    public static int myPlayer = 0;
    public int player, ply;
    //
    private final boolean virtual;
    private final MCTSOptions options;
    public StatCounter stats;
    //
    private boolean expanded = false, simulated = false;
    private List<TreeNode> children;
    private IMove move;
    private double velocity = 1.;
    private double imVal = 0.; // implicit minimax value (in view of parent)
    private double imAlpha = -INF - 1; // implicit lower bound (in view of me)
    private double imBeta = +INF + 1;  // implicit upper bound (in view of me)
    private double heval = 0.; // heuristic evaluation for prog. bias (in view of parent)

    /**
     * Constructor for the root
     */
    public TreeNode(int player, MCTSOptions options) {
        this.player = player;
        this.virtual = false;
        this.options = options;
        this.ply = 0;
        TreeNode.myPlayer = player;
        stats = new StatCounter();
    }

    /**
     * Constructor for internal node
     */
    public TreeNode(int player, int ply, IMove move, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.virtual = false;
        this.options = options;
        this.ply = ply;
        stats = new StatCounter();
    }

    /**
     * Initialize a TreeNode with sliding swUCT UCT
     */
    public TreeNode(int player, int ply, IMove move, MCTSOptions options, boolean windowed) {
        this.player = player;
        this.move = move;
        this.virtual = false;
        this.options = options;
        this.ply = ply;
        stats = new StatCounter(windowed, options);
    }

    /**
     * Initialize a virtual TreeNode for AUCT
     */
    public TreeNode(int player, int ply, IMove move, final boolean virtual, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.virtual = virtual;
        this.options = options;
        this.ply = ply;
        stats = new StatCounter();
    }

    /**
     * Run the MCTS algorithm on the given node.
     *
     * @param board The current board
     * @return the currently evaluated playout value of the node
     */
    public double MCTS(IBoard board, int depth, int previousPlayer) {
        TreeNode child = null;
//        if (depth == 0) {
//            movesMade[0].clear();
//            movesMade[1].clear();
//        }
        // First add some leafs if required
        if (isLeaf()) {
            // Expand returns any node that leads to a win
            child = expand(board, depth + 1, previousPlayer);
        }
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal())
                child = this;
            else
                child = select(board, depth + 1);
        }
        //
        if (child.player < 0)
            throw new RuntimeException("Child player weird!");
        //
        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.stats.mean()) != INF) {
            // Execute the move represented by the child
            if (!isTerminal())
                board.doAIMove(child.getMove(), player);

//            if (options.history)
//                movesMade[player - 1].add(child.getMove());

            // When a leaf is reached return the result of the playout
            if (!child.isSimulated() || child.isTerminal()) {
                result = child.playOut(board, depth + 1);
                // check for non-negamax
                // here, result is in view of the child
                if (this.player != child.player) {
                    child.updateStats(-result, this.player, depth + 1);
                    // get result in view of me
                    result = -result;
                } else {
                    child.updateStats(result, this.player, depth + 1);
                    // result already in view of me
                }
                child.simulated = true;
            } else {
                // The next child
                // check for non-negamax
                if (this.player != child.player)
                    result = -child.MCTS(board, depth + 1, this.player);
                else
                    result = child.MCTS(board, depth + 1, this.player);
            }
            // set the board back to its previous configuration
            if (!isTerminal())
                board.undoMove();
        } else {
            result = child.stats.mean();
        }

        // result is now in view of me in all cases
        if (options.solver) {
            // (Solver) If one of the children is a win, then I'm a win
            if (result == INF) {
                // If I have a win, my parent has a loss.
                // check for non-negamax
                if (previousPlayer != this.player)
                    stats.setValue(-INF);
                else
                    stats.setValue(INF);
                return result;
            } else if (result == -INF) {
                // (Solver) Check if all children are a loss
                for (TreeNode tn : children) {
                    // (AUCT) Skip virtual child
                    if (options.auct && tn.isVirtual())
                        continue;
                    // If the child is not expanded or solved, make sure it is expanded
                    if (options.solverFix && tn.isLeaf() && Math.abs(tn.stats.mean()) != INF) {
                        // Execute the move represented by the child
                        board.doAIMove(tn.getMove(), player);
                        TreeNode winner = tn.expand(board, depth + 2, this.player);
                        board.undoMove();
                        // We found a winning node below the child, this means the child is a loss.
                        if (winner != null) {
                            // check for non-negamax
                            if (player != tn.player)
                                tn.stats.setValue(-INF);
                            else
                                tn.stats.setValue(INF);
                        }
                    }
                    // Are all children a loss?
                    if (tn.stats.mean() != result) {
                        // (AUCT) Update the virtual node with a loss
                        if (options.auct && children.get(0).isVirtual()) {
                            TreeNode virtChild = children.get(0);
                            // check for non-negamax
                            if (player != virtChild.player)
                                virtChild.stats.push(-1);
                            else
                                virtChild.stats.push(1);
                        }
                        // Return a single loss, if not all children are a loss
                        // check for non-negamax; can't explain why the return value has a different sign
                        if (previousPlayer != this.player) {
                            updateStats(1, previousPlayer, depth);  // in view of parent
                            return -1;                       // view of me.
                        } else {
                            updateStats(-1, previousPlayer, depth);  // view of parent
                            return -1;                        // view of me
                        }
                    }
                }
                // (Solver) If all children lead to a loss for the opponent, then I'm a win
                // check for non-negamax
                if (previousPlayer != this.player)
                    stats.setValue(INF);
                else
                    stats.setValue(-INF);

                return result; // always return in view of me
            }
        }
        // Why is result not negated? For the negamax case, should be -result; check with Tom
        // before: updateStats(result); 

        // Update the results for the current node
        // check for non-negamax
        if (previousPlayer != this.player)
            updateStats(-result, previousPlayer, depth);
        else
            updateStats(result, previousPlayer, depth);
        // Back-propagate the result
        // always return in view of me
        if(options.test)
            return result;
        else
            return -result;
    }

    public TreeNode expand(IBoard board, int depth, int parentPlayer) {
        expanded = true;
        // check for non-negamax. will sent this later below!
        // If one of the nodes is a win, we don't have to select
        int nextPlayer = board.getPlayerToMove();
        TreeNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (children == null)
            children = new CopyOnWriteArrayList<>();
        // (AUCT) Add an extra virtual node
        if (options.auct) {
            // FIXME: non-negamax games
            TreeNode vNode = new TreeNode(nextPlayer, depth, null, true, options);
            vNode.stats = stats.copyInv();
            vNode.velocity = velocity;
            children.add(vNode);
        }
        double value, best_imVal = -INF, best_pbVal = -INF, best_maxBackpropQs = -INF;
        int winner = board.checkWin();
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (!board.isPartialObservable() && board.doAIMove(moves.get(i), player)) {
                TreeNode child;
                // Initialize the child
                if (options.swUCT && depth >= options.minSWDepth && depth <= options.maxSWDepth)
                    child = new TreeNode(nextPlayer, depth, moves.get(i), options, true);
                else
                    child = new TreeNode(nextPlayer, depth, moves.get(i), options);

                winner = IBoard.NONE_WIN;
                if (options.solver) {
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
                }
                // implicit minimax
                if (options.implicitMM) {
                    // check for non-negamax
                    if (player != nextPlayer)
                        child.imVal = -board.evaluate(nextPlayer, options.efVer); // view of parent
                    else
                        child.imVal = board.evaluate(nextPlayer, options.efVer); // view of parent

                    if (child.imVal > best_imVal)
                        best_imVal = child.imVal;

                    child.imAlpha = -INF - 1;
                    child.imBeta = +INF + 1;
                }
                // node priors
                if (winner != player && winner != nextPlayer && options.nodePriors) {
                    board.initNodePriors(player, child.stats, moves.get(i), options.nodePriorsVisits);
                }
                // prog. bias
                if (options.progBias) {
                    // must be strictly a bonus
                    child.heval = -board.evaluate(nextPlayer, options.efVer);

                    if (options.pbDecay && child.heval > best_pbVal)
                        best_pbVal = child.heval;
                }
                children.add(child);
                // reset the board
                board.undoMove();
            } else if (board.isPartialObservable()) {
                // No move-checking for partial observable games
                // Also, the legality of the move depends on the determinization
                children.add(new TreeNode(nextPlayer, ply, moves.get(i), options));
            }
        }
        // implicit minimax
        if (options.implicitMM) {
            // check for non-negamax; 
            if (player != parentPlayer)
                this.imVal = -best_imVal;
            else
                this.imVal = best_imVal;
            this.imAlpha = -INF - 1;
            this.imBeta = +INF + 1;
        }
        if (options.progBias && options.pbDecay) {
            this.imVal = -best_pbVal;
        }
        // prog. bias
        if (options.progBias) {
            if (!options.pbDecay)
                this.heval = -board.evaluate(player, options.efVer);
            else
                this.heval = -best_pbVal;
        }

        // If one of the nodes is a win, return it.
        return winNode;
    }

    private TreeNode select(IBoard board, int depth) {
        TreeNode selected = null;
        double bestValue = Double.NEGATIVE_INFINITY, uctValue, avgValue, ucbVar, Np, Nc, sumcvisits = 0;

        // For a chance-move, select a random child
        if (move != null && move.isChance()) {
            return children.get(MCTSOptions.r.nextInt(children.size()));
        }

        for (TreeNode c : children)
            sumcvisits += c.getnVisits();

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

                if (options.nodePriors)
                    Np = sumcvisits;
                //Np += (children.size()-1)*options.nodePriorsVisits;

                // with node priors, must add all the children's initial visits

                if (options.swUCT) {
                    if (c.stats.hasWindow() && !stats.hasWindow()) {
                        Np = Math.min(Np, c.stats.windowSize());
                    } else if (!c.stats.hasWindow() && stats.hasWindow()) {
                        Np = stats.totalVisits();
                    }
                }
                // Progressive bias
                if (options.progBias) {
                    if (options.pbDecay) {
                        // decay
                        avgValue += options.progBiasWeight * c.heval / (Nc + 1);
                    } else {
                        avgValue += options.progBiasWeight * c.heval;
                    }
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

            double eval = board.evaluate(currentPlayer, options.efVer);
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
        double detScore = 0;
        int currentPlayer = board.getPlayerToMove(), moveIndex = -1;
        double mastMax, mastVal, nMoves = 0;
        int nMovesInt = 0;
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
                    currentMove = moves.get(moveIndex);
//                } else if (options.useHeuristics && options.MAST && MCTSOptions.r.nextDouble() < options.mastEps) {
//                    mastMoves.clear();
//                    mastMax = Double.NEGATIVE_INFINITY;
//                    // Select the move with the highest MAST value
//                    for (int i = 0; i < moves.size(); i++) {
//                        mastVal = moves.get(i).getHistoryVal(currentPlayer, options);
//                        // If bigger, we have a winner, if equal, flip a coin
//                        if (mastVal > mastMax) {
//                            mastMoves.clear();
//                            mastMax = mastVal;
//                            mastMoves.add(moves.get(i));
//                        } else if (mastVal == mastMax) {
//                            mastMoves.add(moves.get(i));
//                        }
//                    }
//                    currentMove = mastMoves.get(MCTSOptions.r.nextInt(mastMoves.size()));
                } else {
                    // Choose randomly
                    currentMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
                }

                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {

//                    // Keep track of moves made
//                    if (options.history && !options.to_history)
//                        movesMade[currentPlayer - 1].add(currentMove);

                    nMoves++;
                    nMovesInt++;
                    moveMade = true;
                    winner = board.checkPlayoutWin();
                    gameEnded = winner != IBoard.NONE_WIN;

                    // non-negamax
                    // currentPlayer = board.getOpponent(currentPlayer);
                    currentPlayer = board.getPlayerToMove();

                    // Check if pdepth is reached
                    if (options.earlyEval && nMoves >= options.pdepth) {
                        terminateEarly = true;
                        break;
                    }
                    // Check if dynamic early termination satisfied 
                    if (options.detEnabled && nMovesInt % options.detFreq == 0) {
                        detScore = board.evaluate(player, options.efVer);
                        //detScore = board.evaluate(player, 1); 
                        if (detScore > options.detThreshold || detScore < -options.detThreshold) {
                            terminateEarly = true;
                            break;
                        }
                    }
                } else {
                    // The move was illegal, remove it from the list.
                    moveMade = false;
                    moves.remove(currentMove);
                }
            }
        }

        double score;
        if (gameEnded) {

            if (winner == player) score = 1.0;
            else if (winner == IBoard.DRAW) score = 0.0;
            else score = -1;

            // Alter the score using the relative bonus
            if (winner != IBoard.DRAW) {
                int w = winner - 1;

                // Relative bonus
                if (options.relativeBonus && (nMoves + depth) > 0) {
                    if (options.moveCov.variance2() > 0. && moveStats[w].variance() > 0. && moveStats[w].totalVisits() >= 50) {
                        double x = (moveStats[w].mean() - (nMoves + depth)) / moveStats[w].stddev();
                        double cStar = options.moveCov.getCovariance() / options.moveCov.variance2();
//                        score += Math.signum(score) * .25 * FastSigm.sigm(-options.kr * x);
                        score += Math.signum(score) * cStar * FastSigm.sigm(-options.kr * x);
                    }
                    // Maintain the average number of moves per play-out
                    moveStats[w].push(nMoves + depth);
                    int nm = board.getNMovesMade();
                    int n = (winner == player) ? nm : 0;
                    options.moveCov.push(n, nm);
                }

                // Qualitative bonus
                if (options.qualityBonus) {
                    // Only compute the quality if QB is active, since it may be costly to do so
                    double q = board.getQuality();
                    if (options.qualityCov.getCovariance() > 0. && qualityStats[w].variance() > 0. && qualityStats[w].totalVisits() >= 50) {
                        double qb = (q - qualityStats[w].mean()) / qualityStats[w].stddev();
                        double cStar = options.qualityCov.getCovariance() / options.qualityCov.variance2();
//                        score += Math.signum(score) * .25 * FastSigm.sigm(-options.kq * qb);
                        score += Math.signum(score) * cStar * FastSigm.sigm(-options.kq * qb);
                    }
                    qualityStats[w].push(q);
                    options.qualityCov.push((winner == myPlayer) ? q : 0, q);
                }
            }
        } else if (options.detEnabled && terminateEarly && (detScore > options.detThreshold || detScore < -options.detThreshold)) {
            if (detScore > options.detThreshold)
                score = 1.0;
            else if (detScore < -options.detThreshold)
                score = -1.0;
            else {
                score = 0.0;
                throw new RuntimeException("Should not get here!");
            }
        } else if (options.earlyEval && terminateEarly) {
            // playout terminated by nMoves surpassing pdepth

            // FIXME: relative bonus will not work with pdepth
            score = board.evaluate(player, options.efVer);
        } else {
            throw new RuntimeException("Game end error in playOut");
        }

        // Undo the moves done in the playout
        for (int i = 0; i < nMoves; i++)
            board.undoMove();

        // Update the history values for the moves made during the match
//        if (options.history) {
//            double p1Score = (winner == IBoard.P1_WIN) ? Math.signum(score) : -Math.signum(score);
//            for (int i = 0; i < movesMade[0].size(); i++) {
//                options.updateHistory(1, movesMade[0].get(i).getUniqueId(), p1Score);
//            }
//            for (int i = 0; i < movesMade[1].size(); i++) {
//                options.updateHistory(2, movesMade[1].get(i).getUniqueId(), -p1Score);
//            }
//            // Clear the lists
//            movesMade[0].clear();
//            movesMade[1].clear();
//        }
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

    private void updateStats(double value, int previousPlayer, int depth) {

        // If we are not using AUCT simply add the total value
        if (options.maxBackprop && children != null && getnVisits() >= options.maxBackpropT) {
            double bestVal = -INF - 1;

            for (TreeNode c : children) {
                //double childVal = getnVisits() * c.maxBackpropQs; 
                //double childVal = getnVisits() * c.maxBackpropQs; 
                double childVal = c.stats.mean();

                if (childVal > bestVal)
                    bestVal = childVal;
            }

            if (previousPlayer != this.player)
                stats.push(-bestVal);
            else
                stats.push(bestVal);
        } else if ((!options.auct || isLeaf())) {
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

            // check for non-negamax; FIXME: implicit pruning does not work for non-negamax

            if (previousPlayer != this.player)
                this.imVal = -bestVal;       // view of parent
            else
                this.imVal = bestVal;        // view of parent

            this.imAlpha = bestAlpha;    // view of me
            this.imBeta = bestBeta;      // view of me
        }

        // prog bias decay
        if (options.progBias && options.pbDecay && children != null) {
            double bestVal = -INF - 1;

            for (TreeNode c : children) {
                if (c.heval > bestVal) bestVal = c.heval;
            }

            if (previousPlayer != this.player)
                this.heval = -bestVal;       // view of parent
            else
                this.heval = heval;        // view of parent
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
                updateStats(0, 0, 0);
        }
    }

    public boolean isLeaf() {
        return children == null || !expanded;
    }

    public boolean isSimulated() {
        return simulated;
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
        DecimalFormat df2 = new DecimalFormat("###,##0.00000");
        return move + "\tValue: " + df2.format(stats.mean()) + "\tVisits: " + getnVisits();
    }
}
