package mcts2e.SRCRMCTS;

import ai.mcts.MCTSOptions;

import java.util.ArrayList;
import java.util.List;


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
                n = options.numSimulations;
                K = node.getArity() - FINAL_ARMS;
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
                nextRound += (int) Math.ceil((1. / log_k) * ((n - K) / (K + 1 - k)));
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

    public int difference = 0;
    @Override
    public TreeNode selectBestMove(TreeNode node) {
        TreeNode bestChild1 = null;TreeNode bestChild2 = null;
        double max1 = Double.NEGATIVE_INFINITY, max2 = Double.NEGATIVE_INFINITY, value, value1= 0, value2 = 0;
        List<TreeNode> l = (node.getnVisits() > 0.) ? A : node.getChildren();
        for (TreeNode t : l) {
            if (t.stats.mean() == TreeNode.INF)
                value = TreeNode.INF + MCTSOptions.r.nextDouble();
            else if (t.stats.mean() == -TreeNode.INF)
                value = -TreeNode.INF + t.stats.totalVisits() + MCTSOptions.r.nextDouble();
            else {
                // Select the child with the highest value
                value = t.stats.mean();
                value1 = value;
                if (t.getArity() > 0) {
                    double maxv = Double.NEGATIVE_INFINITY, vnew = 0;
                    for (TreeNode c : t.getChildren()) {
                        //  Get the value of the node with the most visits
                        if (c.getnVisits() > maxv) {
                            vnew = -c.stats.mean();
                            maxv = c.getnVisits();
                        }
                    }
                    if (options.debug)
                        System.out.println("node: " + value + " max v child: " + vnew);
                    value2 = vnew;
                    value = vnew;
                }
            }
            if (value1 > max1) {
                max1 = value;
                bestChild1 = t;
            }
            if (value2 > max2) {
                max2 = value;
                bestChild2 = t;
            }
            // For debugging, print the node
            if (options.debug)
                System.out.println(t);
        }

        if(!bestChild1.getMove().equals(bestChild2.getMove()))
            difference++;

        return bestChild2;
    }

}
