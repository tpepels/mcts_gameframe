package ai.mcts;

public class MCTSOptions {
    public boolean treeReuse = false, treeDecay = false;
    public boolean depthDiscount = false, accelerated = false;
    //
    public boolean debug = true, useHeuristics = true;
    //
    public double discount = 0.6, lambda = .9999, depthD = 0.1;
    public int timeInterval = 5000;
    // Mast not yet implemented
    public boolean mast = false;
    public double[] mastValues;
}
