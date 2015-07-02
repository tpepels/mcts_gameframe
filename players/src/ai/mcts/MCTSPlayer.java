package ai.mcts;

import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.MoveCallback;
import sun.reflect.generics.tree.Tree;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class MCTSPlayer implements AIPlayer, Runnable {

    private ArrayList<double[]> allData = new ArrayList(1000);
    private boolean interrupted = false, parallel = true, retry = false;
    public TreeNode root;
    private IBoard board;
    private MoveCallback callback;
    private IMove bestMove;
    private int myPlayer, nMoves = 0;
    //
    private MCTSOptions options;

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        this.retry = false;
        this.board = board.copy();
        this.callback = callback;
        this.parallel = parallel;
        this.myPlayer = myPlayer;

        // Reset the MAST arrays
        if (options.history)
            options.resetHistory(board.getMaxUniqueMoveId());

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
        if(root.getArity() == 0)
            root.expand(board, 0, myPlayer);
        // Reset the nodes' stats
        TreeNode.moveStats[0].reset();
        TreeNode.moveStats[1].reset();
        TreeNode.qualityStats[0].reset();
        TreeNode.qualityStats[1].reset();
        //
        interrupted = false;

        Thread[] t = new Thread[4];
        for(int i = 0; i < 4; i++) {
            // Start the search in a new Thread.
            t[i] = new Thread(this);
            t[i].start();
        }
        for(int i = 0; i < 4; i++) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int maxVisits = -1;
        for(IMove m : roots.keySet()) {
            if(roots.get(m) > maxVisits) {
                maxVisits = roots.get(m);
                bestMove = m;
            }
        }
        for(IMove c : roots.keySet()) {
            System.out.println(c  + " n: "  + roots.get(c));
        }

        nMoves++;
        // Release the board's memory
        this.board = null;
        System.out.println("Move: " + bestMove);
        roots.clear();
//        if (parallel) {
//            // Start the search in a new Thread.
//            Thread t = new Thread(this);
//            t.start();
//        } else {
//            run();
//        }
    }

    @Override
    public void run() {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        TreeNode root = new TreeNode(myPlayer, options);

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
        IBoard board = this.board.copy();
        if (!options.fixedSimulations) {
            double tickInterval = 30000.0;
            double startTime = System.currentTimeMillis();
            double nextTickTime = startTime + tickInterval;

            // Search for timeInterval seconds
            long endTime = System.currentTimeMillis() + options.timeInterval;
            // Run the MCTS algorithm while time allows it
            while (!interrupted) {
                simulations++;
                options.simsLeft--;
                if (System.currentTimeMillis() >= endTime) {
                    break;
                } else if (System.currentTimeMillis() >= nextTickTime) {
                    System.out.println("Tick. I have searched " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
                    nextTickTime = System.currentTimeMillis() + tickInterval;
                }
                board.newDeterminization(myPlayer, false);
                // Make one simulation from root to leaf.
                // Note: stats at root node are in view of the root player (also never used)
                if (root.MCTS(board, 0, root.player) == TreeNode.INF)
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
            options.tempSims = simulations + (int) (0.1 * simulations);
            options.simsLeft = options.tempSims;
        } else {
            options.tempSims = options.simulations;
            options.simsLeft = options.tempSims;
            // Run as many simulations as allowed
            while (simulations <= options.simulations) {
                simulations++;
                options.simsLeft--;
                board.newDeterminization(myPlayer, false);
                // Make one simulation from root to leaf.
                // Note: stats at the root node are in view of the root player (also never used)
                if (root.MCTS(board, 0, root.player) == TreeNode.INF)
                    break; // Break if you find a winning move
            }
        }
        addRoot(root);
//        // Return the best move found
//        TreeNode bestChild = root.getBestChild(board);
//        // This sometimes happens in experiments
//        if (bestChild == null) {
//            options.debug = true;
//            // Print the root's children
//            root.getBestChild(board);
//            options.debug = false;
//            int nChildren = (root.getChildren() == null) ? 0 : root.getChildren().size();
//            System.err.println("Null bestMove in MCTS player! Root has " + nChildren + " children.");
//            // Try again with a new root
//            root = new TreeNode(myPlayer, options);
//            if (!parallel && !retry) {
//                retry = true;
//                interrupted = false;
//                run();
//            }
//        }
//        bestMove = bestChild.getMove();
//
////        if (options.mapping) {
////            double[] data = new double[2];
////            for (TreeNode t : root.getChildren()) {
////                data[0] += t.stats.true_mean();
////                data[1] += t.stats.variance();
////            }
////            data[0] /= root.getChildren().size();
////            data[1] /= root.getChildren().size();
////            allData.add(data);
////            plotAllData();
////        }
//
        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + myPlayer);
//            System.out.println("Did " + simulations + " simulations");
            System.out.println("Root visits: " + root.getnVisits());
            //
            if (options.relativeBonus) {
                System.out.println("Average P1 moves  : " + TreeNode.moveStats[0].true_mean() + " variance: " + TreeNode.moveStats[0].variance());
                System.out.println("Average P1 moves  : " + TreeNode.moveStats[1].true_mean() + " variance: " + TreeNode.moveStats[1].variance());
                System.out.println("c*                : " + options.moveCov.getCovariance() / options.moveCov.variance2());
                System.out.println("c*                : " + options.moveCov1.getCovariance() / options.moveCov1.variance2());
            }
            if (options.qualityBonus) {
                System.out.println("Average P1 quality: " + TreeNode.qualityStats[0].true_mean() + " variance: " + TreeNode.qualityStats[0].variance());
                System.out.println("Average P2 quality: " + TreeNode.qualityStats[1].true_mean() + " variance: " + TreeNode.qualityStats[1].variance());
                System.out.println("c*                : " + options.qualityCov.getCovariance() / options.qualityCov.variance2());
            }
        }
//        // Turn the qb/rb/sw-uct back on
//        options.qualityBonus = qb;
//        options.relativeBonus = rb;
//        options.swUCT = sw;
//
//        //options.moveCov.reset();
//        //options.qualityCov.reset();
//
//        if (options.swUCT) {
//            for (int i = 0; i < options.maxSims.length; i++) {
//                if (options.maxSims[i] > options.windowSize)
//                    options.maxSWDepth = i;
//                options.maxSims[i] = 0;
//            }
//            options.maxSWDepth = Math.max(options.minSWDepth, options.maxSWDepth);
//            System.out.println("Max SW depth: " + options.maxSWDepth);
//        }
//
//        nMoves++;
//        // Set the root to the best child, so in the next move, the opponent's move can become the new root
//        if (options.treeReuse)
//            root = bestChild;
//        else
//            root = null;
//        // Release the board's memory
//        board = null;
//        // Make the move in the GUI, if parallel
//        if (!interrupted && parallel && callback != null)
//            callback.makeMove(bestChild.getMove());
    }

    Map<IMove, Integer> roots = new HashMap<>();

    private synchronized void addRoot(TreeNode rootNode) {
       for (TreeNode t : rootNode.getChildren()) {
           if(roots.containsKey(t)) {
               roots.put(t.getMove(), (int)(roots.get(t) + t.getnVisits()));
           } else {
               roots.put(t.getMove(), (int) t.getnVisits());
           }
       }
    }

    public void setOptions(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public void newGame(int myPlayer, String game) {
        root = new TreeNode(myPlayer, options);
        TreeNode.moveStats[0].reset();
        TreeNode.moveStats[1].reset();
        TreeNode.qualityStats[0].reset();
        TreeNode.qualityStats[1].reset();
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

