package mcts2e.BRUEi;

import ai.StatCounter;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import ai.mcts.MCTSOptions;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    public static int myPlayer = 0, sigma = 0;
    public static boolean retract = false;
    //
    private final MCTSOptions options;
    public final int player;
    public StatCounter stats;
    //
    private boolean expanded = false;
    private List<TreeNode> children;
    private IMove move;

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

    /**
     * Run the MCTS algorithm on the given node
     */
    public double MCTS2e(IBoard board, int depth) {
        // We've gone deep enough, evaluate the position
        if (endOfProbe(depth, board)) {
            double result = playOut(board);
            updateStats(result);
            // Back-propagate the result
            return result;
        }
        TreeNode child;
        // Select a child
        child = select(board, depth);
        // Execute the move represented by the child
        board.doAIMove(child.getMove(), player);
        // The next child
        double result = child.MCTS2e(board, depth + 1);
        // set the board back to its previous configuration
        board.undoMove();
        // Only update at sigma level
        if (depth == sigma) updateStats(result);
        // Back-propagate the result
        return result;
    }

    private boolean endOfProbe(int depth, IBoard board) {
        // First add some leafs if required
        if (isLeaf())
            expand(board);
        // Don't continue if there are no more moves available
        if (isTerminal()) return true;
        // Visited node
        if (stats.visits() > 0) return false;
        // Leaf, check if we went too far
        if (depth <= sigma) {
            //sigma = -1;
            retract = true;
        }
        return true;
    }

    private void expand(IBoard board) {
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (children == null)
            children = new ArrayList<>(moves.size());
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            children.add(new TreeNode(nextPlayer, moves.get(i), options));
        }
        expanded = true;
    }

    private TreeNode select(IBoard board, int d) {
        TreeNode selected = null;
        // For a chance-move, select a random child
        if (move != null && move.isChance()) {
            return children.get(MCTSOptions.r.nextInt(children.size()));
        }
        // Estimation policy
        if (d > sigma) {
            double max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY;
            for (TreeNode c : children) {
                if (board.getPlayerToMove() == myPlayer) {
                    if (c.stats.mean() > max) {
                        max = c.stats.mean();
                        selected = c;
                    }
                } else {
                    if (c.stats.mean() < min) {
                        min = c.stats.mean();
                        selected = c;
                    }
                }
            }
        }
        // Exploration policy
        if (d <= sigma || selected == null) {
            selected = children.get(MCTSOptions.r.nextInt(children.size()));
        }
        return selected;
    }

    @SuppressWarnings("ConstantConditions")
    private double playOut(IBoard board) {
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove(), moveIndex, nMoves = 0;

        List<IMove> moves;
        int winner = board.checkWin();
        gameEnded = (winner != IBoard.NONE_WIN);
        IMove currentMove;

        while (!gameEnded) {

            moves = board.getPlayoutMoves(options.useHeuristics);
            moveMade = false;

            while (!moveMade) {
                // All moves were discarded
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
                } else {
                    // The move was illegal, remove it from the list.
                    moveMade = false;
                    moves.remove(moveIndex);
                }
            }
        }

        double score;

        if (winner == myPlayer) score = 1.0;
        else if (winner == IBoard.DRAW) score = 0.0;
        else score = -1;

        // Undo the moves done in the playout
        for (int i = 0; i < nMoves; i++)
            board.undoMove();

        return score;
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
        double bestValue = Double.NEGATIVE_INFINITY;

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

    public TreeNode getBestChild() {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : children) {
            value = t.stats.mean();
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

    public boolean isLeaf() {
        return children == null || !expanded;
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    public IMove getMove() {
        return move;
    }

    public double getnVisits() {
        return stats.visits();
    }

    @Override
    public String toString() {
        return move + "\tVisits: " + getnVisits() + "\tValue: " + stats.mean() + "\tvar: " + stats.variance();
    }
}
