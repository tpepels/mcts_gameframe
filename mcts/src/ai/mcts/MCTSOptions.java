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
    public boolean relativeBonus = false, includeDepth = true;
    //
    public boolean debug = true, useHeuristics = true, solverFix = true, ucbTuned = false,  auct = false;
    // MCTS Specific values
    public double uctC = 1., k = 1., maxVar = .5;
    // Discounting values
    public double lambda = .999999, depthD = 0.1, treeDiscount = 0.6;
    public int timeInterval = 2500;
    // MAST stuff
    private double[][] mastValues, mastVisits;
    // Marc's stuff
    //public int pdepth = 1000000;
    //boolean imEnabled = false; // implicit minimax

    public void enableRB(boolean includeDepth) {
        maxVar = 1.;
        this.relativeBonus = true;
        this.includeDepth = includeDepth;
    }

    public void setGame(String game) {
        if (game.equals("cannon")) {
        } else if (game.equals("chinesecheckers")) {
        } else if (game.equals("lostcities")) {
        } else if (game.equals("pentalath")) {
        } else if (game.equals("amazons")) {
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
