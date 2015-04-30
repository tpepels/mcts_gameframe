package ai.SRCRMCTS;

import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;

public class SRCRMCTSPlayer implements AIPlayer, Runnable {
    private boolean interrupted = false, parallel = true;
    private TreeNode root;
    private IBoard board;
    private MoveCallback callback;
    private IMove bestMove;
    private int myPlayer, nMoves = 0;
    // Fields that must be set
    private MCTSOptions options = null;
    private SelectionPolicy selectionPolicy = null;

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove) {

        if (options == null || selectionPolicy == null)
            throw new RuntimeException("MCTS Options or selection policy not set.");

        this.board = board;
        this.callback = callback;
        this.parallel = parallel;
        this.myPlayer = myPlayer;

        // Reset the MAST arrays
        if (options.MAST)
            options.resetHistory(board.getMaxUniqueMoveId());

        // Create a new root, or reuse the old tree
        if (!options.treeReuse || root == null || root.getArity() == 0 || lastMove == null) {
            root = new TreeNode(myPlayer, options, options.tempSims, selectionPolicy);
        } else if (options.treeReuse) {
            // Get the opponent's last move from the root's children
            for (TreeNode t : root.getChildren()) {
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
            root = new TreeNode(myPlayer, options, options.tempSims, selectionPolicy);
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
        boolean sw = options.swUCT;

        if (qb && nMoves == 0)
            options.qualityBonus = false;
        if (rb && nMoves == 0)
            options.relativeBonus = false;
        if (sw && nMoves == 0)
            options.swUCT = false;

        if (!options.fixedSimulations) {
            // Search for timeInterval seconds
            long endTime = System.currentTimeMillis() + options.timeInterval;
            // Run the MCTS algorithm while time allows it
            while (!interrupted) {
                simulations++;
                options.simsLeft--;
                if (System.currentTimeMillis() >= endTime) {
                    break;
                }
                board.newDeterminization(myPlayer, false);
                // Make one simulation from root to leaf.
                if (root.MCTS(board, 0) == TreeNode.INF)
                    break; // Break if you find a winning move
            }
            options.tempSims = simulations + (int) (0.1 * simulations);
            options.simsLeft = options.tempSims;
        } else {
            options.tempSims = options.simulations;
            options.simsLeft = options.tempSims;
            // Run as many simulations as allowed
            while (simulations <= options.simulations) {
                simulations++;
                options.simsLeft--;
                board.newDeterminization(myPlayer, false);
                // Make one simulation from root to leaf.
                if (root.MCTS(board, 0) == TreeNode.INF)
                    break; // Break if you find a winning move
            }
        }
        // Return the best move found
        TreeNode bestChild = selectionPolicy.selectBestMove(root);
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
        options.swUCT = sw;
        //
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

    public void setSelectionPolicy(SelectionPolicy selectionPolicy) {
        this.selectionPolicy = selectionPolicy;
    }

    public void setOptions(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public void newGame(int myPlayer, String game) {
        if (options == null || selectionPolicy == null)
            throw new RuntimeException("MCTS Options or selection policy not set.");

        root = new TreeNode(myPlayer, options, options.tempSims, selectionPolicy);
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

