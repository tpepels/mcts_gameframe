package ai.SRCRMCTS;

import framework.util.FastSigm;
import framework.util.StatCounter;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import ai.mcts.MCTSOptions;

import java.text.DecimalFormat;
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
    private final MCTSOptions options;
    private final SelectionPolicy selectionPolicy;
    public final int player;
    public int simulations;
    public StatCounter stats;
    //
    private boolean expanded = false;
    private List<TreeNode> children;
    private IMove move;

    /**
     * Constructor for the root
     */
    public TreeNode(int player, MCTSOptions options, int simulations, SelectionPolicy selectionPolicy) {
        this.player = player;
        this.options = options;
        this.simulations = simulations;
        TreeNode.myPlayer = player;
        stats = new StatCounter();
        this.selectionPolicy = selectionPolicy;
    }

    /**
     * Constructor for internal node
     */
    public TreeNode(int player, IMove move, MCTSOptions options, SelectionPolicy selectionPolicy) {
        this.player = player;
        this.move = move;
        this.simulations = 0;
        this.options = options;
        this.stats = new StatCounter();
        this.selectionPolicy = selectionPolicy;
    }

    /**
     * Run the MCTS algorithm on the given node
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
            if (isTerminal())       // Game is terminal, no more moves can be played
                child = this;
            else
                child = selectionPolicy.select(this, depth);
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
                options.updateHistory(player, child.getMove().getUniqueId(), -1 * Math.signum(result)); // It's the child's reward that counts, hence -result
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
                // If the child is not expanded, make sure it is
                if (tn.isLeaf()) {
                    // Execute the move represented by the child
                    board.doAIMove(tn.getMove(), player);
                    TreeNode winner = tn.expand(board);
                    board.undoMove();
                    // We found a winning node below the child, this means the child is a loss.
                    if (winner != null) {
                        tn.stats.setValue(-INF);
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
        // Update the results for the current node
        updateStats(result);
        // Back-propagate the result
        return result;
    }

    private TreeNode expand(IBoard board) {
        expanded = true;
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        TreeNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (children == null)
            children = new ArrayList<TreeNode>(moves.size());
        int winner;
        double value;

        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (board.doAIMove(moves.get(i), player)) {
                TreeNode child = new TreeNode(nextPlayer, moves.get(i), options, selectionPolicy);
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
                children.add(child);
                // reset the board
                board.undoMove();
            }
        }
        // If one of the nodes is a win, return it.
        return winNode;
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

            double eval = board.evaluate(currentPlayer, 0);
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
                        mastVal = options.getHistoryValue(currentPlayer, moves.get(i).getUniqueId());
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
        } else if (options.earlyEval && terminateEarly) {
            // playout terminated by nMoves surpassing pdepth

            // FIXME: relative bonus will not work with pdepth
            score = board.evaluate(player, 0);
        } else {
            throw new RuntimeException("Game end error in playOut");
        }

        // Undo the moves done in the playout
        for (int i = 0; i < nMoves; i++)
            board.undoMove();

        return score;
    }

    private void updateStats(double value) {
        stats.push(value);
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
        return stats.visits();
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("###,##0.00000");
        return move + "\tVisits: " + getnVisits() + "\tValue: " + df2.format(stats.mean()) + "\tSims: " + simulations;
    }
}
