package ai.mcts;

import java.util.Random;

public class MCTSOptions {
    // Initialize a random generator, separate for each MCTS player
    public final Random r = new Random();
    // Fields for enabling tree-reuse
    public boolean treeReuse = false, treeDecay = false, ageDecay = false;
    // Discount values based on their depth
    public boolean depthDiscount = false;
    // Relative bonus!
    public boolean relativeBonus = false, stdDev = false;
    //
    public boolean debug = true, useHeuristics = true, solverFix = true;
    public boolean ucbTuned = false, auct = false, nuct = false;
    // MCTS Specific values
    public double uctC = 1., k = .05;
    // Discounting values
    public double lambda = .999999, depthD = 0.1, treeDiscount = 0.6;
    public int timeInterval = 2500;
    // Marc's stuff
    public boolean earlyEval = false; // enable dropping down to evaluation function in playouts?
    public int pdepth = Integer.MAX_VALUE; // number of moves in playout before dropping down to eval func
    public boolean implicitMM = false; // implicit minimax
    // MAST stuff
    private double[][] mastValues, mastVisits;

    public void enableRB() {
        relativeBonus = true;
        uctC = .5;
    }

    /**
     * Set default parameters that should be used here
     *
     * @param game The game to set the default parameters for
     */
    public void setGame(String game) {
        if (game.equals("cannon")) {
        } else if (game.equals("chinesecheckers")) {
        } else if (game.equals("lostcities")) {
        } else if (game.equals("pentalath")) {
        } else if (game.equals("amazons")) {
        } else if (game.equals("breakthrough")) {
        }
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
