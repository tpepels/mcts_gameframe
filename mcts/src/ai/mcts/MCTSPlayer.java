package ai.mcts;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveCallback;

public class MCTSPlayer implements AIPlayer, Runnable {

    private boolean interrupted = false, parallel = true, retry = false;
    private TreeNode root;
    private IBoard board;
    private MoveCallback callback;
    private IMove bestMove;
    private int myPlayer;
    //
    private MCTSOptions options;

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        this.retry = false;
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
                //
                if (t.getMove().equals(lastMove)) {
                    root = t;
                    break;
                }
            }
        }
        // Possible if new root was not expanded
        if (root.player != myPlayer) {
            if (options.debug && root.getChildren() != null)
                System.err.println("Incorrect player at root, old root has " + root.getArity() + " children");
            // Create a new root
            root = new TreeNode(myPlayer, options);
        }
        TreeNode.moveStats.reset();
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

        if (options.debug)
            System.out.println("Thinking for " + options.timeInterval + " ms");

        int simulations = 0;
        if (!options.fixedSimulations) {
            // Search for timeInterval seconds
            long endTime = System.currentTimeMillis() + options.timeInterval;
            // Run the MCTS algorithm while time allows it
            while (!interrupted) {
                simulations++;
                if (System.currentTimeMillis() >= endTime) {
                    break;
                }
                board.newDeterminization(myPlayer);
                // Make one simulation from root to leaf.
                if (root.MCTS(board, 0) == TreeNode.INF)
                    break; // Break if you find a winning move
            }
            // (SW-UCT) Remember the number of simulations for the next round
            options.numSimulations = simulations;
        } else {
            // Run as many simulations as allowed
            while(simulations < options.simulations) {
                simulations++;
                board.newDeterminization(myPlayer);
                // Make one simulation from root to leaf.
                if (root.MCTS(board, 0) == TreeNode.INF)
                    break; // Break if you find a winning move
            }
        }
        // Return the best move found
        TreeNode bestChild = root.getBestChild(board);
        // This sometimes happens in experiments
        if (bestChild == null) {
            options.debug = true;

            // Print the root's children
            root.getBestChild(board);
            options.debug = false;
            int nChildren = (root.getChildren() == null) ? 0 : root.getChildren().size();
            System.err.println("Null bestMove in MCTS player! Root has " + nChildren + " children.");

            // Try again with a new root
            root = new TreeNode(myPlayer, options);
            if (!parallel && !retry) {
                retry = true;
                interrupted = false;
                run();
            }
        }
        bestMove = bestChild.getMove();
        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);
            System.out.println("Did " + simulations + " simulations");
            System.out.println("Best child: " + bestChild);
            System.out.println("Root visits: " + root.getnVisits());
            System.out.println("Avg playout moves: " + TreeNode.moveStats.true_mean() + " std dev: " + TreeNode.moveStats.stddev());
            System.out.println("Moving Avg playout moves: " + TreeNode.moveStats.window_mean());
        }
        // Set the root to the best child, so in the next move
        // the opponent's move can become the new root
        if (options.treeReuse)
            root = bestChild;
        else
            root = null;

        board = null;
        // Make the move in the GUI, if parallel
        if (!interrupted && parallel)
            callback.makeMove(bestChild.getMove());
    }

    public void setOptions(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public void newGame(int myPlayer) {
        root = new TreeNode(myPlayer, options);
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
