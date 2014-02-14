package mcts2e.BRUE;

import ai.framework.*;
import ai.mcts.MCTSOptions;

import java.util.Random;

public class MCTS2ePlayer implements AIPlayer, Runnable {
    private final int TT_SIZE = 33554432;
    private final long MASK = TT_SIZE - 1;
    private final int searchTime = 1000;
    private final boolean debug = true;
    //
    private static int sigma = 0, horizon = 0, simulations = 0;
    private static Random r = new Random();
    //
    private int myPlayer = 0;
    private double[] stateValues, stateVisits;
    private boolean interrupted = false, parallel = false;
    private FiniteBoard board;
    private IMove bestMove;
    private MoveCallback callback = null;

    @Override
    public void newGame(int myPlayer, String game) {

    }

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel, IMove lastMove) {
        this.myPlayer = myPlayer;
        this.board = (FiniteBoard) board;
        this.parallel = parallel;
        this.callback = callback;
        horizon = ((FiniteBoard) board).getHorizon();
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
        stateVisits = new double[TT_SIZE];
        stateValues = new double[TT_SIZE];

        // Search for timeInterval seconds
        long endTime = System.currentTimeMillis() + searchTime;
        // Run the MCTS algorithm while time allows it
        while (!interrupted) {
            simulations++;
            // BRUE Round-robin switching function on sigma/horizon
            sigma = horizon - ((simulations - 1) % horizon);
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            board.newDeterminization(myPlayer);
            probe(board, 0);
        }
        // Set the best move to play
        double max = Double.NEGATIVE_INFINITY;
        MoveList moves = board.getExpandMoves();
        if (debug)
            System.out.println("h:" + board.getStateHash());
        for (int i = 0; i < moves.size(); i++) {
            board.doAIMove(moves.get(i), board.getPlayerToMove());
            int hashPos = getHashPos(board.getStateHash());

            if (debug) {
                System.out.println(moves.get(i) + " r:" + stateValues[hashPos] + " h:" + board.getStateHash());
            }

            if (stateValues[hashPos] > max) {
                max = stateValues[hashPos];
                bestMove = moves.get(i);
            }
            board.undoMove();
        }
        if (debug)
            System.out.println("h:" + board.getStateHash());
        //
        if (debug)
            System.out.println("Ran " + simulations + " simulations.");

        // Make the move in the GUI, if parallel
        if (!interrupted && parallel && callback != null)
            callback.makeMove(bestMove);
    }

    private double probe(FiniteBoard board, int d) {
        MoveList moves = board.getExpandMoves();

        IMove maxMove = null;
        // Estimation policy
        if (d < sigma) {
            if (board.getPlayerToMove() == myPlayer) {
                double max = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < moves.size(); i++) {
                    board.doAIMove(moves.get(i), board.getPlayerToMove());
                    int hashPos = getHashPos(board.getStateHash());
                    if (stateValues[hashPos] > max) {
                        max = stateValues[hashPos];
                        maxMove = moves.get(i);
                    }
                    board.undoMove();
                }
            } else {
                double min = Double.POSITIVE_INFINITY;
                for (int i = 0; i < moves.size(); i++) {
                    board.doAIMove(moves.get(i), board.getPlayerToMove());
                    int hashPos = getHashPos(board.getStateHash());
                    if (stateValues[hashPos] < min) {
                        min = stateValues[hashPos];
                        maxMove = moves.get(i);
                    }
                    board.undoMove();
                }
            }
        }
        // Exploration policy
        if (d > sigma || maxMove == null) {
            maxMove = moves.get(r.nextInt(moves.size()));
        }

        // Go to the next state s' P(S | s, a)
        board.doAIMove(maxMove, board.getPlayerToMove());

        // Check for a possible reward
        double r;                               // No intermediary rewards
        int winner = board.checkWin();
        if (winner != FiniteBoard.NONE_WIN) {
            r = (winner == myPlayer) ? 1 : -1;  // Only win/loss status gives award
        } else {
            r = probe(board, d + 1);            // Go 1 deeper
        }
        // Update the next state, since that encapsulates the action
        if (d == sigma) updateState(board.getStateHash(), r, d);
        // Clear the board
        board.undoMove();
        return r;
    }

    public void updateState(long hash, double r, int d) {
        if (d == 1)
            System.err.println("Update at depth " + d + " hash " + hash);
        int hashPos = getHashPos(hash);
        stateVisits[hashPos]++;
        stateValues[hashPos] += (r - stateValues[hashPos]) / stateVisits[hashPos];
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
        // Not implemented
    }

    @Override
    public IMove getBestMove() {
        return bestMove;
    }
}
