package ai.mcts;

public class MCTSOptions {
	public boolean treeReuse = false;
	public boolean treeDecay = false;
	public boolean accelerated = false;
    public boolean mast = false;
    public double[] mastValues;
    public boolean debug = true;
	//
	public double discount = 0.6, lambda = .9999, epsilon = 0.1;
    public int timeInterval = 5000;
}
