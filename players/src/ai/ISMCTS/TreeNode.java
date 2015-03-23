package ai.ISMCTS;

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

public class TreeNode {
    public static final double INF = 999999;
    private static final MoveList[] movesMade = {new MoveList(500), new MoveList(500)};
    private static final MoveList mastMoves = new MoveList(100);
    public static StatCounter[] moveStats = {new StatCounter(), new StatCounter()};
    public static StatCounter[] qualityStats = {new StatCounter(), new StatCounter()};
    public static int myPlayer = 0;
    public int player, nPrime = 0;
    private final MCTSOptions options;
    public StatCounter stats;
    private ArrayList<TreeNode> children;
    private IMove move;
    private boolean simulated = false;

    /**
     * Constructor for the root
     */
    public TreeNode(int player, MCTSOptions options) {
        this.player = player;
        this.options = options;
        TreeNode.myPlayer = player;
        stats = new StatCounter();
    }

    /**
     * Constructor for internal node
     */
    public TreeNode(int player, IMove move, MCTSOptions options) {
        this.player = player;
        this.move = move;
        this.options = options;
        stats = new StatCounter();
    }

    public double MCTS(IBoard board, int depth) {
        if (depth == 0) {
            movesMade[0].clear();
            movesMade[1].clear();
        }
        // Expand returns an expanded leaf if any was added to the tree
        TreeNode child = expand(board);
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal())
                child = this;
            else // Do UCT selection over the children
                child = select(board);
        }
        //
        if (child.player < 0)
            throw new RuntimeException("Child player weird!");

        // Execute the move represented by the child
        if (!isTerminal())
            board.doAIMove(child.getMove(), player);

        if (options.history)
            movesMade[player - 1].add(child.getMove());

        double result;
        // When a leaf is reached return the result of the play-out
        if (!child.simulated || child.isTerminal()) {
            result = child.playOut(board, depth + 1);
            child.updateStats(-result);
            child.simulated = true;
        } else {
            result = -child.MCTS(board, depth + 1);
        }
        updateStats(result);
        return result;
    }

    /**
     * Adds a single node to the tree
     *
     * @param board The Board
     * @return The expanded node
     */
    private TreeNode expand(IBoard board) {
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        TreeNode newNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        moves.shuffle();
        if (children == null)
            children = new ArrayList(moves.size() * 2);

        int winner = board.checkWin();
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // No move-checking for partial observable games
            // Also, the legality of the move depends on the determinization
            boolean exists = false;
            // Check here if the move is already in the set of children
            for(TreeNode node : children)
                if(node.move.equals(moves.get(i)))
                    exists = true;
            if(!exists) {
                newNode = new TreeNode(nextPlayer, moves.get(i), options);
                children.add(newNode);
                newNode.nPrime++;
                // We have a new node, no need to look further.
                break;
            }
        }
        return newNode;
    }

    private TreeNode select(IBoard board) {
        TreeNode selected = null;
        double bestValue = Double.NEGATIVE_INFINITY, uctValue;

        // For a chance-move, select a random child
        if (move != null && move.isChance())
            return children.get(MCTSOptions.r.nextInt(children.size()));

        // Select a child according to the UCT Selection policy
        for (TreeNode c : children) {
            // If the game is partially observable, moves in the tree may not be legal
            if (board.isPartialObservable() && !board.isLegal(c.getMove()))
                continue;

            if (c.getnVisits() == 0) {
                // First, visit all children at least once
                uctValue = INF + MCTSOptions.r.nextDouble();
            } else {
                // Compute the uct value with the (new) average value
                uctValue = c.stats.mean() + options.uctC * Math.sqrt(FastLog.log(c.nPrime) / c.getnVisits());
            }
            // Number of times this node was available
            c.nPrime++;
            // Remember the highest UCT value
            if (uctValue > bestValue) {
                selected = c;
                bestValue = uctValue;
            }
        }
        return selected;
    }

    @SuppressWarnings("ConstantConditions")
    private double playOut(IBoard board, int depth) {
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove();
        double mastMax, mastVal, nMoves = 0;
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
                    if (board.noMovesIsDraw())
                        winner = IBoard.DRAW;                                   // Pentalath, Lost Cities
                    else                                                        // Last player to move:
                        winner = board.getOpponent(board.getPlayerToMove());    // Cannon, Amazons, Chinese Checkers, Checkers
                    break;
                }

                if (options.MAST && MCTSOptions.r.nextDouble() < options.mastEps) {
                    mastMoves.clear();
                    mastMax = Double.NEGATIVE_INFINITY;
                    // Select the move with the highest MAST value
                    for (int i = 0; i < moves.size(); i++) {
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
                    currentMove = mastMoves.get(MCTSOptions.r.nextInt(mastMoves.size()));
                } else {
                    // Choose randomly
                    currentMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
                }

                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {

                    // Keep track of moves made
                    if (options.history && !options.to_history)
                        movesMade[currentPlayer - 1].add(currentMove);
                    nMoves++;
                    moveMade = true;
                    winner = board.checkPlayoutWin();
                    gameEnded = winner != IBoard.NONE_WIN;
                    currentPlayer = board.getPlayerToMove();
                } else {
                    // The move was illegal, remove it from the list.
                    moveMade = false;
                    moves.remove(currentMove);
                }
            }
        }

        double score;
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
                    score += Math.signum(score) * .25 * FastSigm.sigm(-options.kr * x);
                    //score += Math.signum(score) * cStar * FastSigm.sigm(-options.kr * x);
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
                    score += Math.signum(score) * .25 * FastSigm.sigm(-options.kq * qb);
                    //score += Math.signum(score) * cStar * FastSigm.sigm(-options.kq * qb);
                }
                qualityStats[w].push(q);
                options.qualityCov.push((winner == myPlayer) ? q : 0, q);
            } else if (options.fullQuality) {
                double q = board.getQuality();
                score += Math.signum(score) * FastSigm.sigm(-options.kq * q);
            }
        }

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

    public TreeNode getBestChild(IBoard board) {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : children) {
            // If the game is partial observable, moves in the tree may not be illegal
            if (board.isPartialObservable() && !board.isLegal(t.getMove()))
                continue;
            // For partial observable games, use the visit count, not the values.
            value = t.getnVisits();
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
        stats.push(value);
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public IMove getMove() {
        return move;
    }

    public double getnVisits() {
        return stats.visits();
    }

    public int getArity() {
        return (children != null) ? children.size() : 0;
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("###,##0.00000");
        return move + "\tValue: " + df2.format(stats.mean()) + "\tVisits: " + getnVisits() + "\tn': " + nPrime;
    }

    @Override
    public int hashCode() {
        return move.hashCode();
    }

    @Override
    public boolean equals(Object t2) {
        return move.equals(((TreeNode)t2).move);
    }
}
