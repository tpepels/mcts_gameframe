package mcts_tt.uct;

import ai.FastLog;
import ai.FastSigm;
import ai.StatCounter;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import ai.mcts.MCTSOptions;
import mcts_tt.transpos.State;
import mcts_tt.transpos.TransposTable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UCTNode {
    public static int nodesSimulated = 0;
    private static final MoveList[] movesMade = {new MoveList(500), new MoveList(500)};
    private static final MoveList mastMoves = new MoveList(1000);
    public static StatCounter[] moveStats = {new StatCounter(), new StatCounter()};
    public static StatCounter[] qualityStats = {new StatCounter(), new StatCounter()};
    public static int myPlayer = 0;
    public int player, ply, visits = 0;
    private long hash;
    //
    private final MCTSOptions options;
    //
    private boolean expanded = false, simulated = false;
    private List<UCTNode> children;
    private final TransposTable tt;
    private IMove move;
    private State state;

    /**
     * Constructor for the root
     */
    public UCTNode(int player, MCTSOptions options, IBoard board, TransposTable tt) {
        this.player = player;
        this.options = options;
        this.ply = 0;
        UCTNode.myPlayer = player;
        this.tt = tt;
        this.hash = board.hash();
        this.state = tt.getState(hash, true);
    }

    /**
     * Constructor for internal node
     */
    public UCTNode(int player, int ply, IMove move, MCTSOptions options, IBoard board, TransposTable tt) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.ply = ply;
        this.tt = tt;
        this.hash = board.hash();
        this.state = tt.getState(hash, true);
    }

    /**
     * Run the MCTS algorithm on the given node.
     *
     * @param board The current board
     * @return the currently evaluated playout value of the node
     */
    public double MCTS(IBoard board, int depth) {
        if (board.hash() != hash)
            throw new RuntimeException("Incorrect hash");
        UCTNode child = null;
        if (depth == 0) {
            movesMade[0].clear();
            movesMade[1].clear();
        }
        // First add some leafs if required
        if (isLeaf()) {
            // Expand returns any node that leads to a win
            child = expand(board, depth + 1);
        }
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal())
                child = this;
            else
                child = select();
        }
        //
        if (child.player < 0)
            throw new RuntimeException("Child player weird!");

        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.getValue()) != State.INF) {
            // Execute the move represented by the child
            if (!isTerminal())
                board.doAIMove(child.getMove(), player);

            if (options.history)
                movesMade[player - 1].add(child.getMove());

            // When a leaf is reached return the result of the playout
            if (!child.isSimulated() || child.isTerminal()) {
                result = child.playOut(board, depth + 1);
                child.updateStats(-result);
                child.simulated = true;
                nodesSimulated++;
            } else {
                result = -child.MCTS(board, depth + 1);
            }
            // set the board back to its previous configuration
            if (!isTerminal())
                board.undoMove();
        } else {
            result = child.getValue();
        }
        // result is now in view of me in all cases
        if (options.solver) {
            // (Solver) If one of the children is a win, then I'm a win
            if (result == State.INF) {
                // If I have a win, my parent has a loss.
                setSolved(false);
                return result;
            } else if (result == -State.INF) {
                // (Solver) Check if all children are a loss
                for (UCTNode tn : children) {
                    // If the child is not expanded or solved, make sure it is expanded
                    if (tn.isLeaf() && Math.abs(tn.getValue()) != State.INF) {
                        // Execute the move represented by the child
                        board.doAIMove(tn.getMove(), player);
                        UCTNode winner = tn.expand(board, depth + 2);
                        board.undoMove();
                        // We found a winning node below the child, this means the child is a loss.
                        if (winner != null)
                            tn.setSolved(false);
                    }
                    // Are all children a loss?
                    if (tn.getValue() != result) {
                        // Return a single loss, if not all children are a loss
                        updateStats(1);
                        return -1;
                    }
                }
                // (Solver) If all children lead to a loss for the opponent, then I'm a win
                setSolved(true);
                return result; // always return in view of me
            }
        }
        if (board.hash() != hash)
            throw new RuntimeException("Incorrect hash");
        // Update the results for the current node
        updateStats(result);
        // Back-propagate the result always return in view of me
        return result;
    }

    private UCTNode expand(IBoard board, int depth) {
        expanded = true;
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        UCTNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (children == null)
            children = new ArrayList<>(moves.size());
        //
        int winner = board.checkWin();
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (board.doAIMove(moves.get(i), player)) {
                UCTNode child;
                // Initialize the child
                child = new UCTNode(nextPlayer, depth, moves.get(i), options, board, tt);
                if (options.solver) {
                    // Check for a winner, (Solver)
                    winner = board.checkWin();
                    //
                    if (winner == player) {
                        winNode = child;
                        child.setSolved(true);
                    } else if (winner == nextPlayer) {
                        child.setSolved(false);
                    }
                }
                children.add(child);
                // reset the board
                board.undoMove();
            }
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private UCTNode select() {
        UCTNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        // Use UCT down the tree
        double uctValue, np = getVisits(), value = 0.;
        // Select a child according to the UCT Selection policy
        for (UCTNode c : children) {
            double nc = c.getVisits();
            // Always select a proven win
            if (c.getValue() == State.INF)
                uctValue = State.INF + MCTSOptions.r.nextDouble();
            else if (c.getVisits() == 0 && c.getValue() != -State.INF) {
                // First, visit all children at least once
                uctValue = 100 + MCTSOptions.r.nextDouble();
            } else if (c.getValue() == -State.INF) {
                uctValue = -State.INF + MCTSOptions.r.nextDouble();
            } else {
                value = c.getValue();
                // Compute the uct value with the (new) average value
                uctValue = value + options.uctC * Math.sqrt(FastLog.log(np) / nc);
                if (options.progHistory) {
                    uctValue += c.move.getHistoryVal(player, options) * (options.phW / (getVisits() - getWins() + 1));
                }
            }
            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    @SuppressWarnings("ConstantConditions")
    private double playOut(IBoard board, int depth) {
        boolean gameEnded, moveMade;
        double detScore = 0;
        int currentPlayer = board.getPlayerToMove();
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
                currentMove = null;
                if (options.MAST && MCTSOptions.r.nextDouble() < options.mastEps) {
                    mastMoves.clear();
                    mastMax = Double.NEGATIVE_INFINITY;
                    IMove m = null;
                    // Select the move with the highest MAST value
                    for (int i = 0; i < moves.size(); i++) {
                        m = moves.get(i);
                        if (m.getHistoryVis(currentPlayer, options) == 0)
                            continue;
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
                    if (mastMoves.size() > 0)
                        currentMove = mastMoves.get(MCTSOptions.r.nextInt(mastMoves.size()));
                }
                if (currentMove == null) {
                    // Choose randomly
                    currentMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
                }

                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {

                    // Keep track of moves made
                    if (options.history && !options.to_history)
                        movesMade[currentPlayer - 1].add(currentMove);

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
            score = board.evaluate(player, options.efVer);
        } else {
            throw new RuntimeException("Game end error in playOut");
        }

        // Undo the moves done in the playout
        for (int i = 0; i < nMoves; i++)
            board.undoMove();

        // Update the history values for the moves made during the match
        if (options.history) {
            double p1Score = (winner == IBoard.P1_WIN) ? Math.signum(score) : -Math.signum(score);
            for (int i = 0; i < movesMade[0].size(); i++) {
                options.updateHistory(1, movesMade[0].get(i).getUniqueId(), p1Score);
            }
            for (int i = 0; i < movesMade[1].size(); i++) {
                options.updateHistory(2, movesMade[1].get(i).getUniqueId(), -p1Score);
            }
            // Clear the lists
            movesMade[0].clear();
            movesMade[1].clear();
        }
        return score;
    }

    public UCTNode getBestChild() {
        double max = Double.NEGATIVE_INFINITY, value;
        UCTNode bestChild = null;
        for (UCTNode t : children) {
            // If there are children with INF value, choose one of them
            if (t.getValue() == State.INF)
                value = State.INF + MCTSOptions.r.nextDouble();
            else if (t.getValue() == -State.INF)
                value = -State.INF + t.getVisits() + MCTSOptions.r.nextDouble();
            else {
                value = t.getValue();
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
        if (state == null)
            state = tt.getState(hash, false);
        if (value == -1)
            state.updateStats(player);
        else
            state.updateStats(3 - player);
        visits++;
    }

    private void setSolved(boolean win) {
        if (state == null)
            state = tt.getState(hash, false);
        if (win)
            state.setSolved(3 - player);
        else
            state.setSolved(player);
    }

    /**
     * @return The value of this node with respect to the parent
     */
    public double getValue() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getMean(3 - player);
    }

    private double getWins() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getWins(3 - player);
    }

    /**
     * @return The number of visits of the transposition
     */
    private double getVisits() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getVisits();
    }

    public boolean isLeaf() {
        return children == null || !expanded;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    public IMove getMove() {
        return move;
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("###,##0.00000");
        return move + "\tValue: " + df2.format(getValue()) + "\tVisits: " + getVisits();
    }
}
