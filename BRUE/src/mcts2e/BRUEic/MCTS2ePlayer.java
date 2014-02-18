package mcts2e.BRUEic;

import ai.framework.*;
import ai.mcts.MCTSOptions;
import mcts2e.BRUE.StateHash;

public class MCTS2ePlayer implements AIPlayer, Runnable {
    private final int TT_SIZE = 33554432;
    private final long MASK = TT_SIZE - 1;
    //
    private static int sigma = 0, horizon = 0, simulations = 0;
    //
    private int myPlayer = 0;
    private StateHash[] stateValues;
    private boolean interrupted = false, parallel = false;
    private FiniteBoard board;
    private IMove bestMove;
    private MoveCallback callback = null;
    //
    private MCTSOptions options;

    @Override
    public void newGame(int myPlayer, String game) {

    }

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel, IMove lastMove) {
        this.myPlayer = myPlayer;
        this.board = (FiniteBoard) board;
        this.parallel = parallel;
        this.callback = callback;
        this.bestMove = null;
        horizon = ((FiniteBoard) board).getHorizon();
        //
        interrupted = false;
        collisions = 0;
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
        stateValues = new StateHash[TT_SIZE];

        // Search for timeInterval seconds
        long endTime = System.currentTimeMillis() + options.timeInterval;
        // Run the MCTS algorithm while time allows it
        while (!interrupted) {
            simulations++;
            // BRUE Round-robin switching function on sigma/horizon
            sigma = horizon - ((simulations - 1) % horizon);
            board.newDeterminization(myPlayer);
            probe(board, 0);
            // Check for stopping conditions
            if(!options.fixedSimulations && System.currentTimeMillis() >= endTime)
                break;
            if(options.fixedSimulations && simulations == options.simulations)
                break;
        }
        // Set the best move to play
        double max = Double.NEGATIVE_INFINITY, maxVisits = Double.NEGATIVE_INFINITY;
        MoveList moves = board.getExpandMoves();
        if (options.debug)
            System.out.println("h:" + board.getStateHash());
        for (int i = 0; i < moves.size(); i++) {
            board.doAIMove(moves.get(i), board.getPlayerToMove());
            int hashPos = getHashPos(board.getStateHash());
            if (options.debug) {
                System.out.println(moves.get(i) + " " + stateValues[hashPos] + " h:" + board.getStateHash());
            }
            if (stateValues[hashPos] != null && stateValues[hashPos].value > max) {
                max = stateValues[hashPos].value;
                maxVisits = stateValues[hashPos].visits;
                bestMove = moves.get(i);
            }
            board.undoMove();
        }
        if(bestMove == null)
            bestMove = moves.get(MCTSOptions.r.nextInt(moves.size()));

        if (options.debug) {
            System.out.println("h:" + board.getStateHash());
            System.out.println("Ran " + simulations + " simulations.");
            System.out.println("Collisions: " + collisions);
            System.out.println("Best move: " + bestMove + " " + max + " v: " + maxVisits);
        }

        // Make the move in the GUI, if parallel
        if (!interrupted && parallel && callback != null)
            callback.makeMove(bestMove);
        // Release the values for GC
        stateValues = null;
    }

    private double probe(FiniteBoard board, int d) {
        MoveList moves = board.getExpandMoves();
        IMove maxMove = null;
        // Estimation policy
        if (d > sigma) {
            if (board.getPlayerToMove() == myPlayer) {
                double max = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < moves.size(); i++) {
                    board.doAIMove(moves.get(i), board.getPlayerToMove());
                    int hashPos = getHashPos(board.getStateHash());
                    if (stateValues[hashPos] != null && stateValues[hashPos].value > max) {
                        max = stateValues[hashPos].value;
                        maxMove = moves.get(i);
                    }
                    board.undoMove();
                }
            } else {
                double min = Double.POSITIVE_INFINITY;
                for (int i = 0; i < moves.size(); i++) {
                    board.doAIMove(moves.get(i), board.getPlayerToMove());
                    int hashPos = getHashPos(board.getStateHash());
                    if (stateValues[hashPos] != null && stateValues[hashPos].value < min) {
                        min = stateValues[hashPos].value;
                        maxMove = moves.get(i);
                    }
                    board.undoMove();
                }
            }
        }
        // Exploration policy
        if (d <= sigma || maxMove == null) {
            maxMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
        }
        // Go to the next state s' P(S | s, a)
        board.doAIMove(maxMove, board.getPlayerToMove());
        // Check for a possible reward
        double r;                               // No intermediary rewards
        int winner = board.checkWin();
        if (winner != FiniteBoard.NONE_WIN) {
            r = (winner == myPlayer) ? 1 : -1;  // Only win/loss status gives award
            // The game ended before the switching point
            if(d + 1 > sigma) updateState(board.getStateHash(), r, d);
        } else {
            r = probe(board, d + 1);            //
            // Update the next state, since that encapsulates the action
            if (d + 1 == sigma) updateState(board.getStateHash(), r, d);
        }
        // Clear the board
        board.undoMove();
        return r;
    }
    private int collisions = 0;
    public void updateState(long hash, double r, int d) {
        int hashPos = getHashPos(hash);
        if(stateValues[hashPos] == null) {
            stateValues[hashPos] = new StateHash(hash, r, 1, d);
            return;
        } else if(stateValues[hashPos].hash != hash && d > stateValues[hashPos].depth) {
            collisions++;
            return;
        }
        stateValues[hashPos].visits++;
        stateValues[hashPos].value += (r - stateValues[hashPos].value) / stateValues[hashPos].visits;
    }

    private int getHashPos(long hash) {
        return (int) (hash & MASK);
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
