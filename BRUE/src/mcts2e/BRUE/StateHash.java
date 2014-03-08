package mcts2e.BRUE;

public class StateHash {
    public long hash;
    public double value;
    public int visits, depth;

    public StateHash(long hash, double value, int visits, int depth) {
        this.hash = hash;
        this.value = value;
        this.visits = visits;
        this.depth = depth;
    }

    public String toString() {
        return "r:" + value + " v: " + visits;
    }
}
