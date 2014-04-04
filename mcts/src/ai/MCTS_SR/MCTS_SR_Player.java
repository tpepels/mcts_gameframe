package ai.MCTS_SR;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveCallback;
import ai.mcts.MCTSOptions;

public class MCTS_SR_Player implements AIPlayer, Runnable {
    private boolean interrupted = false, parallel = true;
    private MCTS_SR_Node root;
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
        root = new MCTS_SR_Node(myPlayer, null, options);
        int[] pl = {0};
        root.MCTS_SR(board, 0, options.simulations, pl);
        // Return the best move found
        MCTS_SR_Node bestChild = root.selectBestMove();
        bestMove = bestChild.getMove();
        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);
            System.out.println("Best child: " + bestChild);
            System.out.println("Play-outs: " + pl[0]);
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
            throw new RuntimeException("MCTS Options not set.");
        root = new MCTS_SR_Node(myPlayer, null, options);
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

