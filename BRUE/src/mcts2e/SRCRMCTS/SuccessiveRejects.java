package mcts2e.SRCRMCTS;

import ai.mcts.MCTSOptions;

import java.util.ArrayList;


public class SuccessiveRejects implements SelectionPolicy {

    private final MCTSOptions options;
    private final UCT uctSelection;
    //
    private final int FINAL_ARMS = 2; // + 1 final last arm
    private ArrayList<TreeNode> A = new ArrayList<TreeNode>();
    private double log_k = .5, k = 1;
    private int simulations = 0, nextRound, arms, n, K;

    public SuccessiveRejects(MCTSOptions options, UCT uctSelection) {
        this.options = options;
        this.uctSelection = uctSelection;
    }

    @Override
    public TreeNode select(TreeNode node, int depth) {

        // Otherwise apply the selection policy
        TreeNode selected;
        if (depth == 0) {

            // If the root was just expanded
            if (node.getnVisits() == 0) {
                n = options.numSimulations; K = node.getArity() - FINAL_ARMS;
                A.clear();
                A.addAll(node.getChildren());
                arms = A.size();
                simulations = 0;
                k = 1;
                log_k = .5;
                for (int i = 2; i <= K; i++) {
                    log_k += 1. / i;
                }
                nextRound = (int) Math.ceil((1. / log_k) * ((n - K) / (K + 1 - k)));
                k++;
            }
            //
            selected = A.get(simulations % arms);
            simulations++;

            // Check if a new round should start
            if (simulations == nextRound) {
                nextRound +=(int) Math.ceil((1. / log_k) * ((n - K) / (K + 1 - k)));
                k++;
                if (arms > 2) { // Just to make sure there are still some arms left
                    TreeNode minArm = null;
                    double minVal = Double.POSITIVE_INFINITY;
                    for (TreeNode arm : A) {
                        if (arm.stats.mean() < minVal) {
                            minArm = arm;
                            minVal = arm.stats.mean();
                        }
                    }
                    A.remove(minArm);
                    arms = A.size();
               }
            }
            return selected;
        } else {
            return uctSelection.select(node, depth);
        }
    }

    @Override
    public TreeNode selectBestMove(TreeNode node) {
        TreeNode bestChild = null;
        double max = Double.NEGATIVE_INFINITY, value;
        if (node.getnVisits() > 0.) {
            for (TreeNode t : A) {
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
        } else {
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
        }
        return bestChild;
    }

}
