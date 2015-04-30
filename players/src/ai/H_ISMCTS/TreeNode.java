package ai.H_ISMCTS;

import ai.MCTSOptions;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
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
        // Add all available moves (instead of 1 by 1)
        expandAll(board, options);
        // A node for the hidden player in MO-ISMCTS
        if (board.poMoves() && !options.forceSO)
            hiddenRoot = ai.ISMCTS.TreeNode.expand(board, new ArrayList<ai.ISMCTS.TreeNode>(), options, board.getOpponent(visiblePlayer));

        sSize = children.size();
        totS = children.size();

        IBoard tempBoard;
        ai.ISMCTS.TreeNode c;
        int result, b;
        // Run simulations
        while (budget > 0) {
            b = Math.min(budget, getBudget());
            for (int j = 0; j < sSize; j++) {
                c = children.get(j);
                for (int i = 0; i < b; i++) {
                    tempBoard = board.copy();
                    tempBoard.doAIMove(c.getMove(), tempBoard.getPlayerToMove());
                    tempBoard.newDeterminization(playerToMove);
                    if (!options.flat) {
                        if (board.poMoves() && !options.forceSO)
                            result = ai.ISMCTS.TreeNode.MCTS(tempBoard, visiblePlayer, c, hiddenRoot);
                        else
                            result = c.MCTS(tempBoard, visiblePlayer);
                    } else {
                        result = c.playOut(tempBoard);
                        c.updateStats(result);
                    }
                    stats.push(result);
                    budget--;
                }
            }
            Collections.sort(children.subList(0, sSize), comparator);
            sSize = (int) Math.ceil(sSize / 2.);
        }
    }

    private void expandAll(IBoard board, MCTSOptions options) {
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            if (!board.isLegal(moves.get(i)))
                continue;
            // Add the node to the tree if it didn't exist
            ai.ISMCTS.TreeNode newNode = new ai.ISMCTS.TreeNode(board.getPlayerToMove(), moves.get(i), options);
            children.add(newNode);
        }
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
