package ai.SRCRMCTS;

import ai.mcts.MCTSOptions;

public class HalfGreedySelect implements SelectionPolicy {

    private final MCTSOptions options;
    private final UCT uctSelection;

    public HalfGreedySelect(MCTSOptions options, UCT uctSelection) {
        this.options = options;
        this.uctSelection = uctSelection;
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
           uctSelection.select(node, depth);
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
