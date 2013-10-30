package ai.mcts;

public class MCTSOptions {
    public boolean treeReuse = false, treeDecay = false;
    public boolean depthDiscount = false, accelerated = false;
    public boolean mastEnabled = false, treeOnlyMast = false;
    public boolean relativeBonus = false;
    //
    public boolean debug = true, useHeuristics = true;
    // MCTS Specific values
    public double mastEpsilon = 0.5, uctC = 1., k = 1.;
    // Discounting values
    public double lambda = .9999, depthD = 0.1, treeDiscount = 0.6;
    public int timeInterval = 5000;
    // MAST stuff
    private double[][] mastValues, mastVisits;

    public void setGame(String game) {
        if (game.equals("cannon")) {
            if (depthDiscount)
                uctC = 0.8;
        } else if (game.equals("chinesecheckers")) {
            if (depthDiscount)
                uctC = 0.6;
        } else if (game.equals("lostcities")) {
            mastEnabled = true;
            mastEpsilon = 0.6;
            depthD = 0.1;
        } else if (game.equals("pentalath")) {
            if (depthDiscount)
                uctC = 0.6;
            depthD = 0.15;
        } else if (game.equals("amazons")) {
            if (depthDiscount)
                uctC = 0.6;
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
