package mcts2e.SRCRMCTS;

import ai.FastLog;
import ai.mcts.MCTSOptions;

public class HalfGreedySelect implements SelectionPolicy {

    private final MCTSOptions options;

    public HalfGreedySelect(MCTSOptions options) {
        this.options = options;
    }

    @Override
    public TreeNode select(TreeNode node, int depth) {

        // Otherwise apply the selection policy
        TreeNode selected = null;
        double max = Double.NEGATIVE_INFINITY;

        // At root level, select .5 eps-greedy
        if (depth == 0) {
            if (MCTSOptions.r.nextDouble() < 0.5) {
                for (TreeNode c : node.getChildren()) {
                    if (c.stats.mean() > max) {
                        max = c.stats.mean();
                        selected = c;
                    }
                }
            } else {
                return node.getChildren().get(MCTSOptions.r.nextInt(node.getArity()));
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
