package ai.SRCRMCTS;

/**
 * Created by Tom on 20-2-14.
 */
public interface SelectionPolicy {

    public TreeNode select(TreeNode node, int depth);

    public TreeNode selectBestMove(TreeNode node);
}
