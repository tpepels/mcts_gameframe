package ai.SRMCTS;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveCallback;
import ai.mcts.MCTSOptions;

public class SRMCTSPlayer implements AIPlayer, Runnable {
    private boolean interrupted = false, parallel = true;
    private TreeNode root;
    private IBoard board;
    private MoveCallback callback;
    private IMove bestMove;
    private int myPlayer;
    // Fields that must be set
    private MCTSOptions options = null;

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove) {
        if (options == null)
            throw new RuntimeException("MCTS Options or selection policy not set.");
        this.board = board;
        this.callback = callback;
        this.parallel = parallel;
        this.myPlayer = myPlayer;

        if (options.fixedSimulations) {
            options.numSimulations = options.simulations;
        }
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
        root = new TreeNode(myPlayer, options, options.numSimulations);
        int simulations = 0;
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

                board.newDeterminization(myPlayer);

                // Make one simulation from root to leaf.
                if (Math.abs(root.MCTS(board, 0)) == TreeNode.INF)
                    break; // Break if you find a winning move
            }

            options.numSimulations = simulations + (int) (0.05 * simulations);
        } else {
            options.numSimulations = options.simulations;
            // Run as many simulations as allowed
            while (simulations <= options.simulations) {
                simulations++;
                options.simsLeft--;
                board.newDeterminization(myPlayer);
                // Make one simulation from root to leaf.
                if (Math.abs(root.MCTS(board, 0)) == TreeNode.INF)
                    break; // Break if you find a winning move
            }
        }
        if(simulations < options.simulations)
            System.out.print("");
        // Return the best move found
        TreeNode bestChild = root.selectBestMove();
        bestMove = bestChild.getMove();
        System.out.println("Did " + simulations + " simulations");
        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);

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
        if (options == null)
            throw new RuntimeException("MCTS Options or selection policy not set.");
        root = new TreeNode(myPlayer, options, options.numSimulations);
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

