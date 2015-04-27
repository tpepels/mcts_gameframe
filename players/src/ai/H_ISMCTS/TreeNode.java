package ai.H_ISMCTS;

import ai.MCTSOptions;
import framework.IBoard;
import framework.IMove;
import framework.util.NPlayerStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TreeNode {
    private final static double LOG2 = Math.log(2.);
    private final MCTSOptions options;
    //
    public int nPrime = 0, playerToMove, budget, totB, round, sSize, totS;
    public NPlayerStats stats; // Stats per player
    private ArrayList<ai.ISMCTS.TreeNode> children;
    private ai.ISMCTS.TreeNode hiddenRoot;
    private IMove move = null;

    /**
     * Constructor for the root
     */
    public TreeNode(int budget, int playerToMove, int nPlayers, MCTSOptions options) {
        this.options = options;
        this.budget = budget;
        this.totB = budget;
        this.playerToMove = playerToMove;
        stats = new NPlayerStats(nPlayers);
    }

    /**
     * Constructor for internal node
     */
    public TreeNode(int playerToMove, int nPlayers, IMove move, MCTSOptions options) {
        this.playerToMove = playerToMove;
        this.move = move;
        this.options = options;
        stats = new NPlayerStats(nPlayers);
    }

    public void HMCTS(IBoard board, int visiblePlayer) {
        if (children == null)
            children = new ArrayList<>();
        // Expand all nodes
        while (ai.ISMCTS.TreeNode.expand(board, children, options, visiblePlayer) != null) {
        }
        if (board.poMoves() && !options.forceSO)
            hiddenRoot = ai.ISMCTS.TreeNode.expand(board, new ArrayList<ai.ISMCTS.TreeNode>(), options, board.getOpponent(visiblePlayer));

        sSize = children.size();
        totS = children.size();

        IBoard boards[] = null;
        if (options.limitD) {
            boards = new IBoard[options.nDeterminizations];
            for (int i = 0; i < options.nDeterminizations; i++) {
                boards[i] = board.copy();
                boards[i].newDeterminization(playerToMove);
            }
        }
        IBoard tempBoard;
        ai.ISMCTS.TreeNode c;
        int reps, iteration = 0;
        // Run simulations
        while (budget > 0) {
            int b = Math.min(budget, getBudget());
            for (int j = 0; j < sSize; j++) {
                c = children.get(j);
                reps = 0;
                for (int i = 0; i < b; i++) {
                    if (!options.limitD) {
                        tempBoard = board.copy();
                        tempBoard.newDeterminization(playerToMove);
                    } else {
                        tempBoard = boards[iteration % options.nDeterminizations].copy();
                    }
                    iteration++;
                    //
                    if (tempBoard.isLegal(c.getMove())) {
                        tempBoard.doAIMove(c.getMove(), tempBoard.getPlayerToMove());
                        if (!options.flat) {
                            if (board.poMoves() && !options.forceSO) {
                                ai.ISMCTS.TreeNode.MCTS(tempBoard, visiblePlayer, c, hiddenRoot);
                            } else
                                c.MCTS(tempBoard, visiblePlayer);
                        } else
                            c.updateStats(c.playOut(tempBoard));
                        budget--;
                    } else if (reps < options.nDeterminizations) {
                        // This determinization is not compatible with this node
                        // TODO This is not correct, selects determinizations based on moves,
                        // TODO however, some dets may be much less likely than others.
                        i--;
                        reps++;
                    }
                }
            }
            Collections.sort(children.subList(0, sSize), comparator);
            sSize = (int) Math.ceil(sSize / 2.);
        }
    }

    private ai.ISMCTS.TreeNode getNode(IMove m, List<ai.ISMCTS.TreeNode> nodes) {
        for (ai.ISMCTS.TreeNode n : nodes) {
            if (n.getMove() != null && n.getMove().equals(m))
                return n;
        }
        return null;
    }

    private int getBudget() {
        return (int) Math.max(1, Math.floor(totB / (sSize * Math.ceil(Math.log(totS) / LOG2))));
    }

    public ai.ISMCTS.TreeNode getBestChild() {
        return children.get(0);
    }

    private final Comparator<ai.ISMCTS.TreeNode> comparator = new Comparator<ai.ISMCTS.TreeNode>() {
        @Override
        public int compare(ai.ISMCTS.TreeNode o1, ai.ISMCTS.TreeNode o2) {
            return Double.compare(o2.stats.mean(playerToMove), o1.stats.mean(playerToMove));
        }
    };

    public List<ai.ISMCTS.TreeNode> getChildren() {
        return children;
    }

    public IMove getMove() {
        return move;
    }

    public double getnVisits() {
        return stats.getN();
    }

    public int getArity() {
        return (children != null) ? children.size() : 0;
    }

    @Override
    public String toString() {
        return move + "\tValue: " + stats + "\tVisits: " + getnVisits() + "\tn': " + nPrime;
    }
}
