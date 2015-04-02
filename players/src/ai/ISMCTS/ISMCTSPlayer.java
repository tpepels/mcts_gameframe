package ai.ISMCTS;

import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;
import framework.util.FastLog;

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

    int nd;
    double[] visits, scores;
    IBoard[] boards;

    @Override
    public void run() {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        //
        int simulations = 0;
        IBoard playBoard;


        if (options.banditD) {
            nd = options.nDeterminizations;
            boards = new IBoard[nd];
            visits = new double[nd];
            scores = new double[nd];
            for (int i = 0; i < nd; i++) {
                boards[i] = board.copy();
                boards[i].newDeterminization(myPlayer);
            }
        }
        int nDet = 0;
        if (!options.fixedSimulations) {
            // Search for timeInterval seconds
            long startTime = System.currentTimeMillis();
            long endTime = startTime + options.timeInterval;
            // Run the MCTS algorithm while time allows it
            while (!interrupted) {
                simulations++;
                options.simsLeft--;

                int selBoard = -1;
                if (options.banditD) {
                    selBoard = selectBoard(simulations);
                    playBoard = boards[selBoard];
                    visits[selBoard]++;
                } else {
                    playBoard = board.copy();
                    playBoard.newDeterminization(myPlayer);
                }

                // Make one simulation from root to leaf.
                // Note: stats at root node are in view of the root player (also never used)
                int res = root.MCTS(playBoard);

                if(options.banditD) {
                    if(res == myPlayer) {
                        scores[selBoard]++;
                    } else {
                        scores[selBoard]--;
                    }
                }

                if (System.currentTimeMillis() >= endTime)
                    break;
            }
        } else {
            int detInterval = options.simulations / options.nDeterminizations;

            board.newDeterminization(myPlayer); // To make sure we have a determinization and not the actual board
            // Run as many simulations as allowed
            while (simulations <= options.simulations) {
                simulations++;
                options.simsLeft--;

                int selBoard = -1;
                if (options.banditD) {
                    selBoard = selectBoard(simulations);
                    playBoard = boards[selBoard].copy();
                    visits[selBoard]++;
                } else {
                    if(!options.limitD || simulations % detInterval == 0) {
                        board.newDeterminization(myPlayer);
                        nDet++;
                    } else {
                        board.newDeterminization(myPlayer);
                    }
                    playBoard = board.copy();
                }
                // Make one simulation from root to leaf.
                // Note: stats at the root node are in view of the root player (also never used)
                int res = root.MCTS(playBoard);

                if(options.banditD) {
                    if(res == myPlayer) {
                        scores[selBoard]++;
                    } else {
                        scores[selBoard]--;
                    }
                }

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
            System.out.println("Determinizations: " + nDet);
            if(options.banditD) {
                for (int i = 0; i < nd; i++) {
                    System.out.println("Board: " + i + " v: " + visits[i] + " s: " + scores[i]);
                }
            }
        }
        root = null;
        // Release the board's memory
        board = null;
        // Make the move in the GUI, if parallel
        if (!interrupted && parallel && callback != null)
            callback.makeMove(bestChild.getMove());
    }

    private int selectBoard(int totSim) {
        int selected = -1;
        double max = Integer.MIN_VALUE, uctV;
        for (int i = 0; i < nd; i++) {
            if (visits[i] == 0) {
                return i;
            }
            uctV = (1. - Math.abs(scores[i] / visits[i])) + Math.sqrt(FastLog.log(totSim) / visits[i]);
            if (uctV > max) {
                max = uctV;
                selected = i;
            }
        }
        return selected;
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

