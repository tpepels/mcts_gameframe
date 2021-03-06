package ai.SRCRMCTS;

import ai.MCTSOptions;
import framework.util.FastLog;

/**
 * Created by Tom on 20-2-14.
 */
public class UCT implements SelectionPolicy {
    private final MCTSOptions options;

    public UCT(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public TreeNode select(TreeNode node, int depth) {
        // For a chance-move, select a random child
        if (node.getMove() != null && node.getMove().isChance()) {
            return node.getChildren().get(MCTSOptions.r.nextInt(node.getArity()));
        }
        // Otherwise apply the selection policy
        TreeNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
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

        return selected;
    }

    @Override
    public TreeNode selectBestMove(TreeNode node) {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : node.getChildren()) {
            // If there are children with INF value, choose one of them
            if (t.stats.mean() == TreeNode.INF)
                value = TreeNode.INF + MCTSOptions.r.nextDouble();
            else if (t.stats.mean() == -TreeNode.INF)
                value = -TreeNode.INF + t.stats.totalVisits() + MCTSOptions.r.nextDouble();
            else {
                value = t.stats.totalVisits();
            }
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
