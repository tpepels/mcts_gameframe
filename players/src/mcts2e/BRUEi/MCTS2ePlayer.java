package mcts2e.BRUEi;

import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;

public class MCTS2ePlayer implements AIPlayer, Runnable {
    private static int simulations = 0;
    //
    private int myPlayer = 0;
    private boolean interrupted = false, parallel = false;
    private IBoard board;
    private IMove bestMove;
    private MoveCallback callback = null;
    //
    private MCTSOptions options;
    private TreeNode root;

    @Override
    public void newGame(int myPlayer, String game) {

    }

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel, IMove lastMove) {
        this.myPlayer = myPlayer;
        this.board = board;
        this.parallel = parallel;
        this.callback = callback;
        this.bestMove = null;
        //
        interrupted = false;
        simulations = 0;
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
        root = new TreeNode(myPlayer, options);
        // Search for timeInterval seconds
        long endTime = System.currentTimeMillis() + options.timeInterval;
        // Run the MCTS algorithm while time allows it
        while (!interrupted) {
            simulations++;
            // Switching function
            if (TreeNode.retract) {
                TreeNode.sigma = 0;
                TreeNode.retract = false;
            } else
                TreeNode.sigma++;
            board.newDeterminization(myPlayer);
            root.MCTS2e(board, 0);
            // Check for stopping conditions
            if (!options.fixedSimulations && System.currentTimeMillis() >= endTime)
                break;
            if (options.fixedSimulations && simulations == options.simulations)
                break;
        }
        TreeNode bestChild = root.getBestChild();
        bestMove = bestChild.getMove();

        if (options.debug) {
            System.out.println("Ran " + simulations + " simulations.");
            System.out.println("Best move: " + bestMove + " v: " + bestChild.stats.mean() + " n: " + bestChild.stats.visits());
        }

        // Make the move in the GUI, if parallel
        if (!interrupted && parallel && callback != null)
            callback.makeMove(bestMove);

        root = null;
    }

    @Override
    public void stop() {
        interrupted = true;
    }

    @Override
    public void setOptions(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public IMove getBestMove() {
        return bestMove;
    }
}
