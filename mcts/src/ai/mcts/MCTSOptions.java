package ai.mcts;

import ai.Covariance;

import java.util.Random;

public class MCTSOptions {
    // Initialize a random generator, separate for each MCTS player
    public static final Random r = new Random();
    private static int instances = 0;
    public double maxMoves = 0;
    //
    public Covariance currentCov = new Covariance();
    public double cStar = -1;
    // Fields for enabling tree-reuse
    public boolean treeReuse = false;
    // Discount values based on their depth
    public boolean depthDiscount = false;
    // Sliding-window UCT
    public boolean swUCT = false;
    public int numSimulations, minSWDepth = 2;
    public double switches = 4.;
    // Relative bonus!
    public boolean relativeBonus = false, qualityBonus = false;
    public double k = 2.0;
    // note: useHeuristics uses a different default (false) when using SimGame
    public boolean debug = true, useHeuristics = true, solverFix = true, fixedSimulations = false, mapping = false;
    public boolean ucbTuned = false, auct = false;
    public String plotOutFile = "C:\\users\\tom\\desktop\\data\\arms%s.dat";
    // MCTS Specific values
    public double uctC = 1., maxVar = 1.;
    // Discounting values
    public double lambda = .999999, depthD = 0.1;
    public int timeInterval = 1000, simulations = 10000, simsLeft = 10000;
    // Marc's stuff (mostly for implicit minimax)
    public boolean earlyEval = false;           // enable dropping down to evaluation function in playouts?
    public int pdepth = Integer.MAX_VALUE;      // number of moves in playout before dropping down to eval func
    public boolean implicitMM = false;          // implicit minimax
    public double imAlpha = 0.0;
    public boolean imPruning = false;
    public boolean nodePriors = false;          // use eval func for node priors ? TEST BRANCH
    public int nodePriorsVisits = 100;          // number of visites to initialize in node priors 
    public boolean maxBackprop = false;         // max backprop (FIXME: (i) Ramanujan version, (ii) only works with pdepth = 0)
    public int efVer = 1;                       // int evaluation function version
    public boolean detEnabled = false;          // dynamic early terminations (uses ev. func.)
    public double detThreshold = 0.0;           // >T -> win whereas <T -> loss
    public int detFreq = 5;                     // >T -> win whereas <T -> loss
    // Epsilon-greedy play-outs, where greedy is the highest eval
    public boolean epsGreedyEval = false;
    public double egeEpsilon = 0.1;
    // Progressive bias; H_i is the evaluation function value
    public boolean progBias = false;
    public double progBiasWeight = 0.0;
    // MAST stuff
    public boolean MAST = false, TO_MAST = false; // Turning off heuristics also disables MAST
    public double mastEps = 0.8;
    //
    private int instance = 0;
    private double[][] mastValues, mastVisits;

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
        if (game.equals("cannon")) {
            uctC = .8;
            k = 2.8;
            cStar = .4;
            maxMoves = 200.;
        } else if (game.equals("chinesecheckers")) {
            uctC = .8;
            k = 2.0;
            cStar = .5;
            maxMoves = 400.;
        } else if (game.equals("lostcities")) {
        } else if (game.equals("checkers")) {
            k = 1.8;
            cStar = .5;
            maxMoves = 250;
        } else if (game.equals("pentalath")) {
            uctC = .8;
            MAST = true;
            mastEps = .95;
            cStar = .48;
            k = .9;
            maxMoves = 100;
        } else if (game.equals("amazons")) {
            uctC = .5;
            MAST = true;
            mastEps = .3;
            k = 2.2;
            cStar = .65;
            maxMoves = 70;
        } else if (game.equals("breakthrough")) {
            uctC = 1.;
            MAST = true;
            mastEps = .7;
            k = 0.4;
            cStar = .45;
            maxMoves = 120.;
        }
        resetSimulations(game);
    }

    public void resetSimulations(String game) {
        if (game.equals("cannon")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 2 * timeInterval;
        } else if (game.equals("chinesecheckers")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 15 * timeInterval;
        } else if (game.equals("lostcities")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 7 * timeInterval;
        } else if (game.equals("pentalath")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 24 * timeInterval;
        } else if (game.equals("amazons")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 5 * timeInterval;
        } else if (game.equals("breakthrough")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 18 * timeInterval;
        }
        simsLeft = numSimulations;
    }

    public int getWindowSize() {
        if (simsLeft > 100)
            return (int) Math.sqrt((simsLeft * Math.log(simsLeft)) / switches);
        else
            return -1;
    }

    public void resetMast(int maxId) {
        mastValues = new double[2][maxId];
        mastVisits = new double[2][maxId];
    }

    public void updateMast(int player, int moveId, double value) {
        mastValues[player - 1][moveId] +=
                (value - mastValues[player - 1][moveId]) /
                        (++mastVisits[player - 1][moveId]);
    }

    public double getMastValue(int player, int id) {
        return mastValues[player - 1][id];
    }
}
