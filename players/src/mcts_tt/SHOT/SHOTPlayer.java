package mcts_tt.SHOT;

import ai.mcts.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;
import mcts_tt.transpos.TransposTable;

public class SHOTPlayer implements AIPlayer, Runnable {

    private TransposTable tt = new TransposTable();
    private boolean interrupted = false, parallel = true;
    private SHOTNode root;
    private IBoard board;
    private MoveCallback callback;
    private IMove bestMove;
    private int myPlayer;
    public int total = 0;
    public long totalTime = 0;
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
        SHOTNode.maxDepth = 0;
        SHOTNode.totalPlayouts = 0;
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
        root = new SHOTNode(myPlayer, null, options, board.hash(), tt);
        int[] pl = {0, 0, 0, 0};
        long startT = System.currentTimeMillis();
        root.SHOT(board, 0, options.simulations, pl);
        long endT = System.currentTimeMillis();
        // Return the best move found
        SHOTNode bestChild = root.selectBestMove();
        bestMove = bestChild.getMove();
        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);
            System.out.println("Best child: " + bestChild);
            System.out.println("Play-outs: " + pl[3]);
            System.out.println("Play-outs check: " + SHOTNode.totalPlayouts);
            System.out.println("Max sr depth: " + SHOTNode.maxDepth);
            System.out.println((int) ((1000. * SHOTNode.totalPlayouts) / (endT - startT)) + " playouts per s");
        }
        total += SHOTNode.totalPlayouts;
        totalTime += endT - startT;
        int removed = tt.pack(0);
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
        System.out.println("UCT-C " + options.uctC);
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

