package ai.mcts;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveCallback;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MCTSPlayer implements AIPlayer, Runnable {

    private ArrayList<double[]> allData = new ArrayList<double[]>(1000);
    private boolean interrupted = false, parallel = true, retry = false;
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
        this.retry = false;
        this.board = board;
        this.callback = callback;
        this.parallel = parallel;
        this.myPlayer = myPlayer;

        // Reset the MAST arrays
        if (options.MAST)
            options.resetMast(board.getMaxUniqueMoveId());

        // Create a new root, or reuse the old tree
        if (!options.treeReuse || root == null || root.getArity() == 0 || lastMove == null) {
            root = new TreeNode(myPlayer, options);
        } else if (options.treeReuse) {
            root.resetVelocities();
            // Get the opponents last move from the root's children
            for (TreeNode t : root.getChildren()) {
                // Don't select the virtual node
                if (t.isVirtual())
                    continue;
                //
                if (t.getMove().equals(lastMove)) {
                    root = t;
                    break;
                }
            }
        }
        // Possible if new root was not expanded
        if (root.player != myPlayer) {
            if (options.debug && root.getChildren() != null)
                System.err.println("Incorrect player at root, old root has " + root.getArity() + " children");
            // Create a new root
            root = new TreeNode(myPlayer, options);
        }
        // Reset the nodes' stats
        TreeNode.moveStats[0].reset();
        TreeNode.moveStats[1].reset();
        TreeNode.qualityStats[0].reset();
        TreeNode.qualityStats[1].reset();
        TreeNode.winStat.reset();
        TreeNode.moveStat.reset();
        TreeNode.qStat.reset();
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
                if (root.MCTS(board, 0) == TreeNode.INF)
                    break; // Break if you find a winning move

//                Enable this to plot per arm totals
//                if (options.mapping && simulations % 10 == 0) {
//                    double[] data = new double[root.getChildren().size()];
//                    int i = 0;
//                    for (TreeNode t : root.getChildren()) {
//                        data[i] = t.stats.variance();
//                        i++;
//                    }
//                    allData.add(data);
//                }

            }
            // (SW-UCT) Remember the number of simulations for the next round
            options.numSimulations = simulations;
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
                if (root.MCTS(board, 0) == TreeNode.INF)
                    break; // Break if you find a winning move
            }
        }
        // Return the best move found
        TreeNode bestChild = root.getBestChild(board);
        // This sometimes happens in experiments
        if (bestChild == null) {
            options.debug = true;
            // Print the root's children
            root.getBestChild(board);
            options.debug = false;
            int nChildren = (root.getChildren() == null) ? 0 : root.getChildren().size();
            System.err.println("Null bestMove in MCTS player! Root has " + nChildren + " children.");
            // Try again with a new root
            root = new TreeNode(myPlayer, options);
            if (!parallel && !retry) {
                retry = true;
                interrupted = false;
                run();
            }
        }
        bestMove = bestChild.getMove();

//        if (options.mapping) {
//            double[] data = new double[2];
//            for (TreeNode t : root.getChildren()) {
//                data[0] += t.stats.true_mean();
//                data[1] += t.stats.variance();
//            }
//            data[0] /= root.getChildren().size();
//            data[1] /= root.getChildren().size();
//            allData.add(data);
//            plotAllData();
//        }

        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);
            System.out.println("Did " + simulations + " simulations");
            System.out.println("Best child: " + bestChild);
            System.out.println("Root visits: " + root.getnVisits());
            //
            if (options.relativeBonus) {
                System.out.println("Average P1 moves  : " + TreeNode.moveStats[0].true_mean() + " variance: " + TreeNode.moveStats[0].variance());
                System.out.println("Average P1 moves  : " + TreeNode.moveStats[1].true_mean() + " variance: " + TreeNode.moveStats[1].variance());
                System.out.println("Cov(X,Y)  " + options.moveCov.getCovariance());
                System.out.println("c*        " + -(options.moveCov.getCovariance() / options.moveCov.variance2()));
                System.out.println("PBC       " + options.pbc.getCorrelation());
            }
            if (options.qualityBonus) {
                System.out.println("Average P1 quality: " + TreeNode.qualityStats[0].true_mean() + " variance: " + TreeNode.qualityStats[0].variance());
                System.out.println("Average P2 quality: " + TreeNode.qualityStats[1].true_mean() + " variance: " + TreeNode.qualityStats[1].variance());
                System.out.println("Cov(X,Y)  " + options.qualityCov.getCovariance());
                System.out.println("c*        " + -(options.qualityCov.getCovariance() / options.qualityCov.variance2()));
            }
        }

        // Reset the currently computed covariances
//        options.moveCov.reset();
//        options.qualityCov.reset();
//        options.pbc.reset();

        // Set the root to the best child, so in the next move, the opponent's move can become the new root
        if (options.treeReuse)
            root = bestChild;
        else
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
        root = new TreeNode(myPlayer, options);
        options.moveCov.reset();
        options.qualityCov.reset();
        options.pbc.reset();
        TreeNode.moveStats[0].reset();
        TreeNode.moveStats[1].reset();
        TreeNode.qualityStats[0].reset();
        TreeNode.qualityStats[1].reset();
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

    private void plotAllData() {
        StringBuilder[] sbs = new StringBuilder[2];
        int i = 0;
        for (double[] d : allData) {
            int k = 0;
            for (double da : d) {
                if (sbs[k] == null)
                    sbs[k] = new StringBuilder();
                sbs[k].append(i);
                sbs[k].append(" ");
                sbs[k].append(da);
                sbs[k].append(" ");
                sbs[k].append(k);
                sbs[k].append('\n');
                k++;
            }
            i++;
        }

        try {
            PrintWriter out = new PrintWriter(options.plotOutFile);
            for (StringBuilder sb : sbs) {
                out.println(sb.toString());
                out.println();
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void plotTopArms(int top) {
        int[] maxI = new int[top];
        StringBuilder[] sbs = new StringBuilder[maxI.length];
        for (int j = 0; j < maxI.length; j++) {
            double max = -100;
            for (int i = 0; i < root.getChildren().size(); i++) {
                boolean ok = true;
                for (int k = 0; k < maxI.length; k++) {
                    if (i == maxI[k]) {
                        ok = false;
                        break;
                    }
                }
                if (ok && root.getChildren().get(i).stats.mean() > max) {
                    max = root.getChildren().get(i).stats.mean();
                    maxI[j] = i;
                }
            }
        }
        int j = 10;
        for (double[] d : allData) {
            int k = 0;
            for (int i : maxI) {
                if (sbs[k] == null)
                    sbs[k] = new StringBuilder();
                sbs[k].append(j);
                sbs[k].append(" ");
                sbs[k].append(d[i]);
                sbs[k].append(" ");
                sbs[k].append(i);
                sbs[k].append('\n');
                k++;
            }
            j += 10;
        }
        try {
            PrintWriter out = new PrintWriter(options.plotOutFile);
            for (StringBuilder sb : sbs) {
                out.println(sb.toString());
                out.println();
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        allData.clear();
    }
}

