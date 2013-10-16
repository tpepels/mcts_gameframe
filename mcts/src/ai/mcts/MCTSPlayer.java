package ai.mcts;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveCallback;

public class MCTSPlayer implements AIPlayer, Runnable {

    private boolean interrupted = false, parallel = true;
    private TreeNode root;
    private IBoard board;
    private MoveCallback callback;
    private IMove bestMove;
    private int myPlayer;
    //
    private MCTSOptions options;

    public void setOptions(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        this.board = board;
        this.callback = callback;
        this.parallel = parallel;
        this.myPlayer = myPlayer;
        // Create a new root, or reuse the old tree
        if (!options.treeReuse || root == null || root.getArity() == 0 || lastMove == null) {
            root = new TreeNode(myPlayer, options);
        } else if (options.treeReuse) {
            root.resetVelocities();
            // Get the opponents last move from the root's children
            for (TreeNode t : root.getChildren()) {
                // Don't select the virtual node
                if (t.isVirtual())
                    continue;
                if (t.getMove().equals(lastMove)) {
                    root = t;
                    break;
                }
            }
        }
        if (root.player != myPlayer) {
            System.err.println("Root error! Using new root node");
            root = new TreeNode(myPlayer, options);
        }
        // Discount all values in the tree
        if (options.treeDecay)
            root.discountValues(options.discount);
        //
        interrupted = false;
        if (parallel) {
            // Start the search in a new Thread.
            Thread t = new Thread(this);
            t.start();
        } else {
            run();
        }
    }

    @Override
    public void run() {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        // Search for timeInterval seconds
        long endTime = System.currentTimeMillis() + options.timeInterval;
        int simulations = 0;
        // Run the MCTS algorithm while time allows it
        while (!interrupted) {
            simulations++;
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            //
            board.newDeterminization(myPlayer);
            // Make one simulation from root to leaf.
            root.MCTS(board, root, 0);
        }
        // Return the best move found
        TreeNode bestChild = root.getBestChild(board);
        bestMove = bestChild.getMove();
        if (!interrupted && parallel)
            callback.makeMove(bestChild.getMove());
        // Set the root to the best child, so in the next move
        // the opponent's move can become the new root
        if (options.treeReuse)
            root = bestChild;
        // show information on the best move
        if (options.debug) {
            System.out.println("Did " + simulations + " simulations");
            System.out.println("Best child: " + bestChild);
        }
    }

    @Override
    public void stop() {
        interrupted = true;
    }

    @Override
    public IMove getBestMove() {
        return bestMove;
    }
}
