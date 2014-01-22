package ai.mcts;

import ai.Covariance;
import ai.PBC;

import java.util.Random;

public class MCTSOptions {
    // Initialize a random generator, separate for each MCTS player
    public static final Random r = new Random();
    private static int instances = 0;
    // Sliding-window UCT
    public int numSimulations, minSWDepth = 2, maxSWDepth = 3;
    public double switches = 4.;
    // Relative bonus!
    public boolean relativeBonus = false, qualityBonus = false;
    public Covariance moveCov = new Covariance(), qualityCov = new Covariance();
    public double k = 2.0;
    // note: useHeuristics has a different default (false) when using SimGame
    public boolean debug = true, useHeuristics = true, solverFix = true, fixedSimulations = false,treeReuse = false;
    public boolean ucbTuned = false, auct = false, swUCT = false;
    // Plot stats to a file
    public boolean mapping = false;
    public String plotOutFile = "C:\\users\\tom\\desktop\\data\\arms%s.dat";
    // MCTS Specific values
    public double uctC = 1., maxVar = 1.;
    // Discounting values
    public double lambda = .999999;
    public int timeInterval = 1000, simulations = 10000, simsLeft;
    // MAST stuff
    public boolean MAST = false, TO_MAST = false; // Turning off heuristics also disables MAST
    public double mastEps = 0.8;
    //
    // Marc's stuff
    public boolean earlyEval = false;           // enable dropping down to evaluation function in playouts?
    public int pdepth = Integer.MAX_VALUE;      // number of moves in playout before dropping down to eval func
    public boolean implicitMM = false;          // implicit minimax
    public double imAlpha = 0.0;
    public boolean imPruning = false;
    // Epsilon-greedy play-outs, where greedy is the highest eval
    public boolean epsGreedyEval = false;
    public double egeEpsilon = 0.1;
    // Progressive bias; H_i is the evaluation function value
    public boolean progBias = false;
    public double progBiasWeight = 0.0;
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
            k = 3.0;
        } else if (game.equals("chinesecheckers")) {
            uctC = .8;
            k = 1.2;
        } else if (game.equals("lostcities")) {
        } else if (game.equals("checkers")) {
            k = 2.8;
        } else if (game.equals("pentalath")) {
            uctC = .8;
            MAST = true;
            mastEps = .95;
            k = 1.;
        } else if (game.equals("amazons")) {
            uctC = .5;
            MAST = true;
            mastEps = .3;
            k = 2.6;
        } else if (game.equals("breakthrough")) {
            uctC = 1.;
            MAST = true;
            mastEps = .7;
            k = 8.0;
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
                numSimulations = 14 * timeInterval;
        } else if (game.equals("checkers")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 24 * timeInterval;
        } else if (game.equals("lostcities")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 6 * timeInterval;
        } else if (game.equals("pentalath")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 21 * timeInterval;
        } else if (game.equals("amazons")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 3 * timeInterval;
        } else if (game.equals("breakthrough")) {
            if (fixedSimulations)
                numSimulations = simulations;
            else
                numSimulations = 17 * timeInterval;
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
