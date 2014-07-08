package mcts_tt.uct;

import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;
import ai.mcts.MCTSOptions;
import mcts_tt.transpos.State;
import mcts_tt.transpos.TransposTable;

public class UCTPlayer implements AIPlayer, Runnable {

    private TransposTable tt = new TransposTable();
    private boolean interrupted = false, parallel = true;
    public UCTNode root;
    private IBoard board;
    private MoveCallback callback;
    private IMove bestMove;
    private int myPlayer, nMoves = 0;
    public int total = 0;
    public long totalT = 0;
    //
    private MCTSOptions options;

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        this.board = board;
        this.callback = callback;
        this.parallel = parallel;
        this.myPlayer = myPlayer;
        UCTNode.nodesSimulated = 0;

        // Reset the MAST arrays
        if (options.history)
            options.resetHistory(board.getMaxUniqueMoveId());

        root = new UCTNode(myPlayer, options, board, tt);
        if (nMoves > 0 && board.hash() == 0)
            throw new RuntimeException("Unlikely hash");
        // Reset the nodes' stats
        UCTNode.moveStats[0].reset();
        UCTNode.moveStats[1].reset();
        UCTNode.qualityStats[0].reset();
        UCTNode.qualityStats[1].reset();
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
        long startT = System.currentTimeMillis();
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
                // Note: stats at root node are in view of the root player (also never used)
                if (Math.abs(root.MCTS(board, 0)) == State.INF)
                    break; // Break if you find a winning move
            }
            // (SW-UCT) Remember the number of simulations for the next round
            options.numSimulations = simulations + (int) (0.1 * simulations);
            options.simsLeft = options.numSimulations;
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
                if (Math.abs(root.MCTS(board, 0)) == State.INF)
                    break; // Break if you find a winning move
            }
        }
        long endT = System.currentTimeMillis();
        // Return the best move found
        UCTNode bestChild = root.getBestChild();
        bestMove = bestChild.getMove();

        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);
            System.out.println("Did " + simulations + " simulations");
            System.out.println("Best child: " + bestChild);
            System.out.println("Collisions: " + tt.collisions + ", tps: " + tt.positions);
            System.out.println("Recoveries: " + tt.recoveries);
            System.out.println((int) ((1000. * simulations) / (endT - startT)) + " playouts per s");
        }
        total += simulations;
        totalT += endT - startT;
        int removed = tt.pack(0);
        if (options.debug) {
            System.out.println("Pack cleaned: " + removed + " transpositions");
        }
        // Turn the qb/rb/sw-uct back on
        options.qualityBonus = qb;
        options.relativeBonus = rb;
        options.swUCT = sw;
        nMoves++;
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
        UCTNode.moveStats[0].reset();
        UCTNode.moveStats[1].reset();
        UCTNode.qualityStats[0].reset();
        UCTNode.qualityStats[1].reset();
        options.qualityCov.reset();
        options.moveCov.reset();
        nMoves = 0;
        options.maxSWDepth = options.minSWDepth;
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

