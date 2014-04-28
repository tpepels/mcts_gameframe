package mcts_tt.MCTS_SR;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveCallback;
import ai.mcts.MCTSOptions;
import mcts_tt.transpos.TransposTable;

public class SRPlayer implements AIPlayer, Runnable {

    private TransposTable tt = new TransposTable();
    private boolean interrupted = false, parallel = true;
    private SRNode root;
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
            throw new RuntimeException("MCTS Options not set.");
        this.board = board;
        this.callback = callback;
        this.parallel = parallel;
        this.myPlayer = myPlayer;
        // Reset the MAST arrays
        if (options.history)
            options.resetHistory(board.getMaxUniqueMoveId());
        //
        SRNode.maxDepth = 0;
        SRNode.totalPlayouts = 0;
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
        root = new SRNode(myPlayer, null, options, board.hash(), tt);
        int[] pl = {0, 0, 0, 0};
        root.MCTS_SR(board, 0, options.simulations, pl);
        // Return the best move found
        SRNode bestChild = root.selectBestMove();
        bestMove = bestChild.getMove();
        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);
            System.out.println("Best child: " + bestChild);
            System.out.println("Play-outs: " + pl[0]);
            System.out.println("Play-outs check: " + SRNode.totalPlayouts);
            System.out.println("Max sr depth: " + SRNode.maxDepth);
            System.out.println("Collisions: " + tt.collisions + ", tps: " + tt.positions);
        }
        int removed = tt.pack();
        if (options.debug)
            System.out.println("Pack cleaned: " + removed + " transpositions");
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

