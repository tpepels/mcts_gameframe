package ai.ISMCTS;

import ai.mcts.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;

import java.util.ArrayList;

public class ISMCTSPlayer implements AIPlayer, Runnable {

    private ArrayList<double[]> allData = new ArrayList(1000);
    private boolean interrupted = false, retry = false, parallel = false;
    public TreeNode root;
    private IBoard board;
    private MoveCallback callback;
    private IMove bestMove;
    private int myPlayer, nMoves = 0;
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
        this.myPlayer = myPlayer;

        // Reset the MAST arrays
        if (options.history)
            options.resetHistory(board.getMaxUniqueMoveId());

        // Create a new root, or reuse the old tree
        if (!options.treeReuse || root == null || root.getArity() == 0 || lastMove == null) {
            root = new TreeNode(myPlayer, options);
        } else if (options.treeReuse) {
            // Get the opponents last move from the root's children
            for (TreeNode t : root.getChildren()) {
                // Don't select the virtual node
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
        // Reset the nodes' stats
        TreeNode.moveStats[0].reset();
        TreeNode.moveStats[1].reset();
        TreeNode.qualityStats[0].reset();
        TreeNode.qualityStats[1].reset();
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

        int simulations = 0;
        boolean qb = options.qualityBonus;
        boolean rb = options.relativeBonus;
        if (qb && nMoves == 0)
            options.qualityBonus = false;
        if (rb && nMoves == 0)
            options.relativeBonus = false;

        if (!options.fixedSimulations) {
            // Search for timeInterval seconds
            long endTime = System.currentTimeMillis() + options.timeInterval;
            // Run the MCTS algorithm while time allows it
            while (!interrupted) {
                simulations++;
                options.simsLeft--;
                board.newDeterminization(myPlayer);
                // Make one simulation from root to leaf.
                // Note: stats at root node are in view of the root player (also never used)
                root.MCTS(board, 0);
                if (System.currentTimeMillis() >= endTime)
                    break;

            }
        } else {
            options.numSimulations = options.simulations;
            options.simsLeft = options.numSimulations;
            // Run as many simulations as allowed
            while (simulations <= options.simulations) {
                simulations++;
                options.simsLeft--;
                board.newDeterminization(myPlayer);
                // Make one simulation from root to leaf.
                // Note: stats at the root node are in view of the root player (also never used)
                root.MCTS(board, 0);
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
            //
            if (options.relativeBonus) {
                System.out.println("Average P1 moves  : " + TreeNode.moveStats[0].true_mean() + " variance: " + TreeNode.moveStats[0].variance());
                System.out.println("Average P1 moves  : " + TreeNode.moveStats[1].true_mean() + " variance: " + TreeNode.moveStats[1].variance());
                System.out.println("c*                : " + options.moveCov.getCovariance() / options.moveCov.variance2());
                System.out.println("c*                : " + options.moveCov1.getCovariance() / options.moveCov1.variance2());
            }
            if (options.qualityBonus) {
                System.out.println("Average P1 quality: " + TreeNode.qualityStats[0].true_mean() + " variance: " + TreeNode.qualityStats[0].variance());
                System.out.println("Average P2 quality: " + TreeNode.qualityStats[1].true_mean() + " variance: " + TreeNode.qualityStats[1].variance());
                System.out.println("c*                : " + options.qualityCov.getCovariance() / options.qualityCov.variance2());
            }
        }
        // Turn the qb/rb/sw-uct back on
        options.qualityBonus = qb;
        options.relativeBonus = rb;

        //options.moveCov.reset();
        //options.qualityCov.reset();

        nMoves++;
        // Set the root to the best child, so in the next move, the opponent's move can become the new root
        if (options.treeReuse)
            root = bestChild;
        else
            root = null;
        // Release the board's memory
        board = null;
        // Make the move in the GUI, if parallel
        if (!interrupted && parallel && callback != null)
            callback.makeMove(bestChild.getMove());
    }

    public void setOptions(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public void newGame(int myPlayer, String game) {
        root = new TreeNode(myPlayer, options);
        TreeNode.moveStats[0].reset();
        TreeNode.moveStats[1].reset();
        TreeNode.qualityStats[0].reset();
        TreeNode.qualityStats[1].reset();
        options.qualityCov.reset();
        options.moveCov.reset();
        nMoves = 0;
        //
        if (!options.fixedSimulations)
            options.resetSimulations(game);
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

