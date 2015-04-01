package ai.ISMCTS;

import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;

public class ISMCTSPlayer implements AIPlayer, Runnable {

    private boolean interrupted = false, parallel = false;
    public TreeNode root;
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

        this.board = board;
        this.callback = callback;
        this.myPlayer = myPlayer;
        // Create a new root, or reuse the old tree
        root = new TreeNode(myPlayer, 2, options);
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
        IBoard playBoard;
        if (!options.fixedSimulations) {
            // Search for timeInterval seconds
            long startTime = System.currentTimeMillis();
            long endTime = startTime + options.timeInterval;
            // Run the MCTS algorithm while time allows it
            while (!interrupted) {
                simulations++;
                options.simsLeft--;
                playBoard = board.copy();
                playBoard.newDeterminization(myPlayer);
                // Make one simulation from root to leaf.
                // Note: stats at root node are in view of the root player (also never used)
                root.MCTS(playBoard);
                if (System.currentTimeMillis() >= endTime)
                    break;
            }
        } else {
            //int detInterval = options.simulations / 10;
            // Run as many simulations as allowed
            while (simulations <= options.simulations) {
                simulations++;
                options.simsLeft--;
                //if (simulations % detInterval == 0)
                playBoard = board.copy();
                playBoard.newDeterminization(myPlayer);
                // Make one simulation from root to leaf.
                // Note: stats at the root node are in view of the root player (also never used)
                root.MCTS(playBoard);
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
        }
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
        root = new TreeNode(myPlayer, 2, options);
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

