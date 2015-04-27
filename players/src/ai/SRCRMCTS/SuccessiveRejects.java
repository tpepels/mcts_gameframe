package ai.SRCRMCTS;

import ai.MCTSOptions;

import java.util.ArrayList;
import java.util.List;


public class SuccessiveRejects implements SelectionPolicy {

    private final MCTSOptions options;
    private final SelectionPolicy uctSelection;
    //
    private final int FINAL_ARMS = 2; // + 1 final last arm
    private ArrayList<TreeNode> A = new ArrayList<TreeNode>();
    private double log_k = .5, k = 1;
    private int simulations = 0, nextRound, arms, n, K;
    public int myPlayer = 0;

    public SuccessiveRejects(MCTSOptions options, SelectionPolicy uctSelection) {
        this.options = options;
        this.uctSelection = uctSelection;
    }

    @Override
    public TreeNode select(TreeNode node, int depth) {
        // Otherwise apply the selection policy
        TreeNode selected;
        if (depth < 1) {
            myPlayer = node.player;
            // If the root was just expanded
            if (node.getnVisits() == 0) {
                n = options.tempSims;
                K = node.getArity();
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
                int round = (int) Math.ceil((1. / log_k) * ((n - K) / (K + 1 - k)));
                nextRound += round;
                k++;
                if (arms > FINAL_ARMS) { // Just to make sure there are still arms left
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
                    for (TreeNode arm : A) {
                        arm.simulations += round / arms;
                    }
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
        List<TreeNode> l = (node.getnVisits() > 0.) ? A : node.getChildren();
        for (TreeNode t : l) {
            if (t.stats.mean() == TreeNode.INF)
                value = TreeNode.INF + MCTSOptions.r.nextDouble();
            else if (t.stats.mean() == -TreeNode.INF)
                value = -TreeNode.INF + t.stats.totalVisits() + MCTSOptions.r.nextDouble();
            else {
                // Select the child with the highest value
                value = t.stats.mean();
            }
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
