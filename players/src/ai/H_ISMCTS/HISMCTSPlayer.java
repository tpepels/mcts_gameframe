package ai.H_ISMCTS;

import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;

public class HISMCTSPlayer implements AIPlayer, Runnable {

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
        root = new TreeNode(options.simulations, myPlayer, 2, options);
        // Possible if new root was not expanded
        if (root.playerToMove != myPlayer) {
            if (options.debug && root.getChildren() != null)
                System.err.println("Incorrect player at root, old root has " + root.getArity() + " children");
            // Create a new root
            root = new TreeNode(options.simulations, myPlayer, 2, options);
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
        // Make a new copy and determinization just in case :)
        IBoard b = board.copy();
        b.newDeterminization(myPlayer, false);
        root.HMCTS(b, myPlayer);
        // Return the best move found
        ai.ISMCTS.TreeNode bestChild = root.getBestChild();
        bestMove = bestChild.getMove();
        // show information on the best move
        if (options.debug) {
            for (ai.ISMCTS.TreeNode t : root.getChildren()) {
                System.out.println(t);
            }
            System.out.println("Player " + myPlayer);
            System.out.println("Best child: " + bestChild);
        }
        // Set the root to the best child, so in the next move, the opponent's move can become the new root
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
        root = new TreeNode(options.simulations, myPlayer, 2, options);
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

