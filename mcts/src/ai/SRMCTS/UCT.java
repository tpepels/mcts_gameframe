package ai.SRMCTS;


import ai.FastLog;
import ai.mcts.MCTSOptions;

import java.util.List;

public class UCT {
    private final MCTSOptions options;

    public UCT(MCTSOptions options) {
        this.options = options;
    }

    public TreeNode select(List<TreeNode> nodes, double np) {
        // Otherwise apply the selection policy
        TreeNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        // Use UCT down the tree
        double uctValue;
        // Select a child according to the UCT Selection policy
        for (TreeNode c : nodes) {
            // No visits or win-node
            if (c.getnVisits() == 0 || c.stats.mean() == ai.SRCRMCTS.TreeNode.INF) {
                // First, visit all children at least once
                uctValue = 1000 + MCTSOptions.r.nextDouble();
            } else {
                // Compute the uct value with the (new) average value
                uctValue = c.stats.mean() + options.uctC * Math.sqrt(FastLog.log(np) / c.getnVisits());
            }
            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }

        return selected;
    }
}