package ai;

import framework.util.Covariance;

import java.util.Random;

public class MCTSOptions {
    // Initialize a random generator, separate for each MCTS player
    public static final Random r = new Random();
    private static int instances = 0;
    // Sliding-window UCT
    public int numSimulations, minSWDepth = 2, maxSWDepth = 3;
    public double windowSize = 500;
    public int[] maxSims = new int[10];
    // Relative bonus!
    public boolean relativeBonus = false, qualityBonus = false, fullQuality = false;
    public Covariance moveCov = new Covariance(), qualityCov = new Covariance(), moveCov1 = new Covariance();
    public double kr = 2.0, kq = 2.0;
    // note: useHeuristics has a different default (false) when using SimGame
    public boolean debug = true, useHeuristics = true, solverFix = true, fixedSimulations = false, treeReuse = false, solver = true;
    public boolean ucbTuned = false, auct = false, swUCT = false, forceSO = false;
    public boolean banditD = false, flat = false;
    public int banditStrat = 1;
    public double detC = 1.;
    public String plotOutFile = "C:\\users\\tom\\desktop\\data\\arms%s.dat";
    // MCTS Specific values
    public double uctC = 1., uctCC = .5, maxVar = 1.;
    // Discounting values
    public double lambda = .999999;
    public int timeInterval = 1000, simulations = 100000, simsLeft;
    public int nDeterminizations = 10;
    public boolean limitD = false;
    // Successive Rejects
    public int rc = 2, bl = 5;
    public double bp_range = .5;
    public boolean remove = true, stat_reset = false, shot = false;
    public boolean rec_halving = false, max_back = false, range_back = false, UBLB = false;
    // MAST stuff
    public boolean history = false, to_history = false; // Set this to true to keep track of all results
    public boolean MAST = false; // Turning off heuristics also disables MAST
    public double mastEps = 0.1;
    //
    public boolean hybrid = false;
    // Marc's stuff (mostly for implicit minimax)
    public boolean earlyEval = false;           // enable dropping down to evaluation function in playouts?
    public int pdepth = Integer.MAX_VALUE;      // number of moves in playout before dropping down to eval func
    public boolean implicitMM = false;          // implicit minimax
    public double imAlpha = 0.0;
    public boolean imPruning = false;
    public boolean nodePriors = false;          // use eval func for node priors ? TEST BRANCH
    public int nodePriorsVisits = 100;          // number of visites to initialize in node priors 
    public boolean maxBackprop = false;         // max backprop 
    public int maxBackpropT = 0;                // threshold for when to switch
    public int efVer = 0;                       // int evaluation function version
    public boolean detEnabled = false;          // dynamic early terminations (uses ev. func.)
    public double detThreshold = 0.3;           // >T -> win whereas <T -> loss
    public int detFreq = 5;                     // how often the eval is checked in det
    // Epsilon-greedy play-outs, where greedy is the highest eval
    public boolean epsGreedyEval = false;
    public double egeEpsilon = 0.1;
    // Progressive bias; H_i is the evaluation function value
    public boolean progBias = false;
    public double progBiasWeight = 0.0;
    public boolean pbDecay = false;
    //
    public boolean progHistory = false;
    public double phW = 1, sr_phW = 1;
    //
    private int instance = 0;
    private double[][] histVal, histVis;

    public MCTSOptions() {
        this.instance = ++instances;
        plotOutFile = String.format(plotOutFile, instance);
    }

    public MCTSOptions(String dataName) {
        this.instance = ++instances;
        plotOutFile = String.format(plotOutFile, instance + "-" + dataName);
    }

    /**
     * Set default parameters that should be used here
     *
     * @param game The game to set the default parameters for
     */
    public void setGame(String game) {
        rc = 2;
        if (game.equalsIgnoreCase("amazons")) {
            uctC = .4;
//            if (hybrid)
//                uctC = 0.25;
            // Budget limit
            bl = 90;
            // Bonus constants
            kr = 2.2;
            kq = 1.6;
        } else if (game.equalsIgnoreCase("breakthrough")) {
            uctC = .8;
//            if (hybrid)
//                uctC = .65;
            // Budget limit
            if (!solver || useHeuristics) {
                bl = 70;
            } else {
                bl = 50;
            }
            // Bonus constants
            kr = 8.0;
            kq = 2.0;
        } else if (game.equalsIgnoreCase("cannon")) {
            uctC = .8;
            // Bonus constants
            kr = 3.0;
            kq = 4.;
        } else if (game.equalsIgnoreCase("chinesecheckers")) {
            uctC = .8;
            // Bonus constants
            kr = 1.2;
            kq = 2.8;
        } else if (game.startsWith("nogo")) {
            uctC = .6;
//            if (hybrid)
//                uctC = .55;
            // Budget limit
            if (!solver)
                bl = 90;
            else
                bl = 110;
        } else if (game.equalsIgnoreCase("pentalath")) {
            uctC = .8;
//            if (hybrid)
//                uctC = .7;
            // Budget limit
            if (!solver)
                bl = 30;
            else
                bl = 90;
            // Bonus constants
            kr = 1.;
            kq = 1.6;
        } else if (game.equalsIgnoreCase("lostcities")) {
            uctC = .6;
            nDeterminizations = 250;
        } else if (game.equalsIgnoreCase("kalah")) {
            uctC = 1.;
        } else if (game.equalsIgnoreCase("checkers")) {
            // Bonus constants
            kr = 2.8;
            kq = 2.0;
        } else if (game.equalsIgnoreCase("domineering")) {
            uctC = 1.;
        } else if (game.startsWith("phantomdomineering")) {
            uctC = .6;
            nDeterminizations = 250;
        } else if (game.equalsIgnoreCase("penguin")) {
            uctC = 1.;
        } else if (game.equalsIgnoreCase("gofish")) {
            uctC = .8;
            nDeterminizations = 150;
        } else {
            throw new RuntimeException("Game not found! " + game);
        }
        resetSimulations(game);
    }

    public void resetSimulations(String game) {
        if (game.equalsIgnoreCase("cannon")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 2 * timeInterval;
        } else if (game.equalsIgnoreCase("chinesecheckers")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 14 * timeInterval;
        } else if (game.equalsIgnoreCase("checkers")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 24 * timeInterval;
        } else if (game.equalsIgnoreCase("lostcities")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 6 * timeInterval;
        } else if (game.equalsIgnoreCase("pentalath")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 21 * timeInterval;
        } else if (game.equalsIgnoreCase("amazons")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 3 * timeInterval;
        } else if (game.equalsIgnoreCase("breakthrough")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 17 * timeInterval;
        } else if (game.startsWith("kalah")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 22 * timeInterval;
        } else if (game.equalsIgnoreCase("domineering")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 22 * timeInterval;
        } else if (game.startsWith("phantomdomineering")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 22 * timeInterval;
        } else if (game.startsWith("nogo")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 22 * timeInterval;
        } else if (game.startsWith("penguin")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 22 * timeInterval;
        } else if (game.startsWith("gofish")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 22 * timeInterval;
        } else {
            throw new RuntimeException("Game not found! " + game);
        }
        simsLeft = numSimulations;
    }

    public int getWindowSize() {
        if (simsLeft > windowSize)
            return (int) windowSize;
        else
            return -1;
    }

    public void resetHistory(int maxId) {
        histVal = new double[2][maxId];
        histVis = new double[2][maxId];
    }

    public void updateHistory(int player, int moveId, double value) {
        histVal[player - 1][moveId] += (value - histVal[player - 1][moveId]) / (++histVis[player - 1][moveId]);
    }

    public void enableShot() {
        this.shot = true;
        this.remove = true;
        this.rc = 2;
        this.rec_halving = false;
        this.stat_reset = false;
        this.max_back = false;
        this.range_back = false;
    }

    public double getHistoryValue(int player, int id) {
        return histVal[player - 1][id];
    }

    public double getHistoryVisits(int player, int id) {
        return histVis[player - 1][id];
    }
}
