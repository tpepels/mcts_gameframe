package mcts2e.SRCRMCTS;

import ai.FastLog;
import ai.mcts.MCTSOptions;

/**
 * Created by Tom on 20-2-14.
 */
public class SqrtUCT implements SelectionPolicy {

    private final MCTSOptions options;

    public SqrtUCT(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public TreeNode select(TreeNode node, int depth) {

        // Otherwise apply the selection policy
        TreeNode selected = null;
        double max = Double.NEGATIVE_INFINITY;

        // At root level, select with sqrt uct
        if (depth == 0) {
            // Use UCT down the tree
            double uctValue;
            // Select a child according to the UCT Selection policy
            for (TreeNode c : node.getChildren()) {
                // No visits or win-node
                if (c.getnVisits() == 0 || c.stats.mean() == TreeNode.INF) {
                    // First, visit all children at least once
                    uctValue = 1000 + MCTSOptions.r.nextDouble();
                } else {
                    // Compute the uct value with the (new) average value
                    uctValue = c.stats.mean() + options.uctC * Math.sqrt(Math.sqrt(node.getnVisits()) / c.getnVisits());
                }
                // Remember the highest UCT value
                if (uctValue > max) {
                    selected = c;
                    max = uctValue;
                }
            }
        } else {
            // For a chance-move, select a random child
            if (node.getMove().isChance()) {
                return node.getChildren().get(MCTSOptions.r.nextInt(node.getArity()));
            }
            // Use UCT down the tree
            double uctValue;
            // Select a child according to the UCT Selection policy
            for (TreeNode c : node.getChildren()) {
                // No visits or win-node
                if (c.getnVisits() == 0 || c.stats.mean() == TreeNode.INF) {
                    // First, visit all children at least once
                    uctValue = 1000 + MCTSOptions.r.nextDouble();
                } else {
                    // Compute the uct value with the (new) average value
                    uctValue = c.stats.mean() + options.uctC * Math.sqrt(FastLog.log(node.getnVisits()) / c.getnVisits());
                }
                // Remember the highest UCT value
                if (uctValue > max) {
                    selected = c;
                    max = uctValue;
                }
            }
        }
        return selected;
    }

    @Override
    public TreeNode selectBestMove(TreeNode node) {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : node.getChildren()) {
            // Select the child with the highest value
            value = t.stats.mean();
            //
            if (value > max) {
                max = value;
                bestChild = t;
            }
            // For debugging, print the node
            if (options.debug)
                System.out.println(t);
        }
        return bestChild;
    }
}
