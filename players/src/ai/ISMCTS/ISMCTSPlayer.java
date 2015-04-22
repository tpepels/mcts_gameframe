package ai.ISMCTS;

import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;
import framework.util.FastLog;
import framework.util.StatCounter;

public class ISMCTSPlayer implements AIPlayer, Runnable {

    private boolean interrupted = false, parallel = false;
    public TreeNode root, root2;
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
        root = new TreeNode(myPlayer, options);
        if (board.poMoves())
            root2 = new TreeNode(myPlayer, options);

        interrupted = false;
        if (parallel) {
            // Start the search in a new Thread.
            Thread t = new Thread(this);
            t.start();
        } else {
            run();
        }
    }

    double score = 0.;
    int nd, simulations;
    StatCounter[] stats;
    IBoard[] boards;

    @Override
    public void run() {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        simulations = 0;
        score = 0.;
        IBoard playBoard;
        nd = options.nDeterminizations;
        // Use bandit selection over the determinizations
        if (options.banditD) {
            boards = new IBoard[nd];
            stats = new StatCounter[nd];
            for (int i = 0; i < nd; i++) {
                boards[i] = board.copy();
                stats[i] = new StatCounter();
                boards[i].newDeterminization(myPlayer);
            }
        }
        // Generate a limited set of determinizations
        if (options.limitD) {
            boards = new IBoard[nd];
            for (int i = 0; i < nd; i++) {
                boards[i] = board.copy();
                boards[i].newDeterminization(myPlayer);
            }
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + options.timeInterval;

        if (options.fixedSimulations) {
            // 1 week search
            endTime += 7 * 24 * 60 * 60 * 1000;
        }

        // Run the MCTS algorithm while time allows it
        while (!interrupted) {
            simulations++;
            options.simsLeft--;
            int selBoard = -1;
            if (options.banditD) {
                selBoard = selectBoard(simulations);
                playBoard = boards[selBoard].copy();
            } else {
                if (options.limitD) {
                    playBoard = boards[simulations % nd].copy();
                } else {
                    playBoard = board.copy();
                    playBoard.newDeterminization(myPlayer);
                }
            }
            int res;
            if (!options.forceSO && !board.poMoves())
                res = root.MCTS(playBoard, myPlayer);
            else
                res = TreeNode.MCTS(playBoard, myPlayer, root, root2);
            //
            if (options.banditD) {
                int reward = (res == myPlayer) ? 1 : -1;
                stats[selBoard].push(reward);
                score += reward;
            }

            if (!options.fixedSimulations && System.currentTimeMillis() >= endTime)
                break;
            else if (options.fixedSimulations && simulations >= options.simulations)
                break;
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
            //
            if (options.banditD) {
                for (int i = 0; i < nd; i++) {
                    System.out.println("Board: " + i + " v: " + stats[i].visits() +
                            " s: " + stats[i].mean() + " var: " + stats[i].variance());
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
            if (stats[i].visits() == 0) {
                return i;
            }
            uctV = 0.;
            switch (options.banditStrat) {
                case 1:
                    uctV = stats[i].variance();
                    break;
                case 2:
                    uctV = stats[i].mean();
                    break;
                case 3:
                    uctV = 1. - stats[i].mean();
                    break;
                case 4:
                    uctV = stats[i].mean() - (score / (double) simulations);
                    break;
                case 5:
                    uctV = (score / (double) simulations) - stats[i].mean();
                    break;
                case 6:
                    uctV = 1. - stats[i].variance();
                    break;
            }
            //
            uctV += options.detC * Math.sqrt(FastLog.log(totSim) / stats[i].visits());
            //
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
        root = new TreeNode(myPlayer, options);
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

