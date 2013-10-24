package ai.mcts;

public class MCTSOptions {
    public boolean treeReuse = false, treeDecay = false;
    public boolean depthDiscount = false, fixedDD = false, accelerated = false;
    public boolean mastEnabled = false, treeOnlyMast = false;
    //
    public boolean debug = true, useHeuristics = true, solver = true;
    // MCTS Specific values
    public double mastEpsilon = 0.5, uctC = .3;
    // Discounting values
    public double lambda = .9999;
    public double depthD = 0.05, treeDiscount = 0.6;
    public int timeInterval = 5000;
    // MAST stuff
    public int nMastValues = 0, minMastValues = 0;
    private double[][] mastValues, mastVisits;

    public void resetMast(int maxId) {
        nMastValues = 0;
        mastValues = new double[2][maxId];
        mastVisits = new double[2][maxId];
    }

    public void updateMast(int player, int moveId, double value) {
        mastValues[player - 1][moveId] +=
                (value - mastValues[player - 1][moveId]) /
                        (++mastVisits[player - 1][moveId]);
        nMastValues++;
    }

    public double getMastValue(int player, int id) {
        return mastValues[player - 1][id];
    }
}
