package ai.mcts;

public class MCTSOptions {
    public boolean treeReuse = false, treeDecay = false, ageDecay = false;
    public boolean depthDiscount = false, accelerated = false;
    public boolean relativeBonus = false, includeDepth = true;
    //
    public boolean debug = true, useHeuristics = true, solverFix = true, ucbTuned = false;
    // MCTS Specific values
    public double uctC = 1., k = 1., maxVar = .5;
    // Discounting values
    public double lambda = .999999, depthD = 0.1, treeDiscount = 0.6;
    public int timeInterval = 5000;
    // MAST stuff
    private double[][] mastValues, mastVisits;

    public void enableRB(boolean includeDepth) {
        maxVar = 1.;
        this.relativeBonus = true;
        this.includeDepth = includeDepth;
    }

    public void setGame(String game) {
        if (game.equals("cannon")) {
            if (relativeBonus && includeDepth)
                k = 1.5;
        } else if (game.equals("chinesecheckers")) {
            if (relativeBonus && includeDepth)
                k = 0.5;
        } else if (game.equals("lostcities")) {

        } else if (game.equals("pentalath")) {
            if (relativeBonus && includeDepth)
                k = 0.5;
        } else if (game.equals("amazons")) {
            if (relativeBonus && includeDepth)
                k = 0.5;
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
