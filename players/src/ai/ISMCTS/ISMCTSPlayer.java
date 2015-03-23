package ai.ISMCTS;

import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;
import gofish.game.Board;

public class ISMCTSPlayer implements AIPlayer, Runnable {

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
        //
        int simulations = 0;
        boolean qb = options.qualityBonus;
        boolean rb = options.relativeBonus;
        if (qb && nMoves == 0)
            options.qualityBonus = false;
        if (rb && nMoves == 0)
            options.relativeBonus = false;
        //
        int nDeterminizations = 0;
        IBoard playBoard;
        if (!options.fixedSimulations) {
            // Search for timeInterval seconds
            long startTime = System.currentTimeMillis();
            long endTime = startTime + options.timeInterval;
            long detInterval = options.timeInterval / 10;
            long time ;
            boolean[] dets = new boolean[20];

            // Run the MCTS algorithm while time allows it
            while (!interrupted) {
                playBoard = board.copy();
                simulations++;
                options.simsLeft--;
                time = System.currentTimeMillis() - startTime;
                if (simulations == 0 || (time % detInterval == 0 && !dets[(int) (time / detInterval)])) {
                    playBoard.newDeterminization(myPlayer);
                    nDeterminizations++;
                    dets[(int) (time / detInterval)] = true;
                }
                // Make one simulation from root to leaf.
                // Note: stats at root node are in view of the root player (also never used)
                root.MCTS(playBoard, 0);
                if (System.currentTimeMillis() >= endTime)
                    break;
            }
        } else {
            int detInterval = options.simulations / 10;
            // Run as many simulations as allowed
            while (simulations <= options.simulations) {
                playBoard = board.copy();
                simulations++;
                options.simsLeft--;
                if (simulations % detInterval == 0)
                    playBoard.newDeterminization(myPlayer);
                // Make one simulation from root to leaf.
                // Note: stats at the root node are in view of the root player (also never used)
                root.MCTS(playBoard, 0);
            }
        }
        // Return the best move found
        TreeNode bestChild = root.getBestChild(board);
        bestMove = bestChild.getMove();

        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);
            System.out.println("Did " + simulations + " simulations");
            System.out.println("Best child: " + bestChild);
            System.out.println("Root visits: " + root.getnVisits());
            System.out.println("N Determinizations: " + nDeterminizations);
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

