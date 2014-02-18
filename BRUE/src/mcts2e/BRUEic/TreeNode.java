package mcts2e.BRUEic;

import ai.*;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import ai.mcts.MCTSOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TreeNode {
    public static final double INF = 999999;
    private static final Stack<IMove> movesMade = new Stack<IMove>();
    public static int myPlayer = 0;
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
    public double MCTS2e(IBoard board, int depth, int sigma) {
        TreeNode child = null;
        // First add some leafs if required
        if (isLeaf()) {
            expand(board);
        }
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal()) // Game is terminal, no more moves can be played
                child = this;
//            else
//                child = select(board, depth + 1);
        }
        //
        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.stats.mean()) != INF && !child.isTerminal()) {
            // Execute the move represented by the child
            board.doAIMove(child.getMove(), player);
            // When a leaf is reached return the result of the playout
            if (child.getnVisits() == 0) {
                result = child.playOut(board);
                child.updateStats(-result);
            } else {
                // The next child
                result = child.MCTS2e(board, depth + 1, sigma);
            }
            // Update the MAST value for the move, use original value not the altered reward (signum)
            if (options.useHeuristics && options.MAST)
                options.updateMast(player, child.getMove().getUniqueId(), result); // It's the child's reward that counts, hence -result
            // set the board back to its previous configuration
            board.undoMove();
        } else {
            result = child.stats.mean();
        }


        // Update the results for the current node
        updateStats(result);
        // Back-propagate the result
        return result;
    }

    private void expand(IBoard board) {
        expanded = true;
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        //
        if (children == null)
            children = new ArrayList<TreeNode>(moves.size());
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            children.add(new TreeNode(nextPlayer, moves.get(i), options));
        }
    }

    private TreeNode select(IBoard board, int depth, int sigma) {
        TreeNode selected = null;
        // For a chance-move, select a random child
        if (move != null && move.isChance()) {
            return children.get(MCTSOptions.r.nextInt(children.size()));
        }

        // Select a child according to the UCT Selection policy
        for (TreeNode c : children) {

        }
        return selected;
    }

    @SuppressWarnings("ConstantConditions")
    private double playOut(IBoard board) {
        boolean gameEnded, moveMade;
        int currentPlayer = board.getPlayerToMove(), moveIndex = -1, nMoves = 0;
        double mastMax, mastVal;

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

    public int getArity() {
        return children == null ? 0 : children.size();
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

    @Override
    public String toString() {
        return move + "\tVisits: " + getnVisits() + "\tValue: " + stats.mean() + "\tvar: " + stats.variance();
    }
}
