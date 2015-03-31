package ai.H_ISMCTS;

import ai.MCTSOptions;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastLog;
import framework.util.NPlayerStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TreeNode {
    public int nPrime = 0, playerToMove, budget, totB, round, sSize, totS;
    private final MCTSOptions options;
    public NPlayerStats stats; // Stats per player
    private ArrayList<TreeNode> children;
    private IMove move = null;
    private boolean simulated = false;

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

    public void HMCTS(IBoard board) {
        // Expand all nodes
        while (expand(board) != null) {
        }
        sSize = children.size();
        totS = children.size();
        // Run simulations
        while (budget > 0) {
            int b = Math.min(budget, getBudget());
            for (int j = 0; j < sSize; j++) {
                TreeNode c = children.get(j);
                for (int i = 0; i < b; i++) {
                    IBoard tempBoard = board.copy();
                    tempBoard.newDeterminization(playerToMove);
                    tempBoard.doAIMove(c.getMove(), board.getPlayerToMove());
                    c.MCTS(tempBoard);
                    budget--;
                }
            }
            Collections.sort(children.subList(0, sSize), comparator);
            sSize = (int) Math.ceil(sSize / 2.);
        }
    }

    private int MCTS(IBoard board) {
        // Expand returns an expanded leaf if any was added to the tree
        TreeNode child = expand(board);
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal())
                child = this;
            else // Do selection over the children
                child = select(board);
        }
        // Execute the move represented by the child
        if (!isTerminal())
            board.doAIMove(child.getMove(), board.getPlayerToMove());
        int result;
        // When a leaf is reached return the result of the play-out
        if (!child.simulated || child.isTerminal()) {
            result = child.playOut(board);
            child.updateStats(result);
            child.simulated = true;
        } else {
            result = child.MCTS(board);
        }
        updateStats(result);
        return result;
    }

    /**
     * Adds a single node to the tree
     *
     * @param board The Board
     * @return The expanded node
     */
    private TreeNode expand(IBoard board) {
        TreeNode newNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        moves.shuffle();
        if (children == null)
            children = new ArrayList<>(moves.size() * 2);
        int winner = board.checkWin();
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // No move-checking for partial observable games
            // Also, the legality of the move depends on the determinization
            boolean exists = false;
            // Check here if the move is already in the set of children
            for (TreeNode node : children)
                if (node.move.equals(moves.get(i)) && node.playerToMove == board.getPlayerToMove())
                    exists = true;
            if (!exists) {
                newNode = new TreeNode(board.getPlayerToMove(), stats.getNPlayers(), moves.get(i), options);
                children.add(newNode);
                newNode.nPrime++;
                // We have a new node, no need to look further.
                break;
            }
        }
        return newNode;
    }

    private TreeNode select(IBoard board) {
        TreeNode selected = null;
        double bestValue = Double.NEGATIVE_INFINITY, uctValue;
        // For a chance-move, select a random child
        if (move != null && move.isChance())
            return children.get(MCTSOptions.r.nextInt(children.size()));
        // Select a child according to the UCT Selection policy
        for (TreeNode c : children) {
            // If the game is partially observable, moves in the tree may not be legal
            if (board.isPartialObservable() && !board.isLegal(c.getMove()))
                continue;
            // First, visit all children at least once
            if (c.nPrime == 0)
                uctValue = 10000. + MCTSOptions.r.nextDouble();
            else
                uctValue = c.stats.mean(board.getPlayerToMove()) + options.uctC * Math.sqrt(FastLog.log(c.nPrime) / c.getnVisits());
            // Number of times this node was available
            c.nPrime++;
            // Remember the highest UCT value
            if (uctValue > bestValue) {
                selected = c;
                bestValue = uctValue;
            }
        }
        return selected;
    }

    private int getBudget() {
        return (int) Math.max(1, Math.floor(totB / (sSize * Math.ceil(Math.log(totS) / LOG2))));
    }

    private static final double LOG2 = Math.log(2.);

    private int playOut(IBoard board) {
        int currentPlayer = board.getPlayerToMove();
        int winner = board.checkWin();
        List<IMove> moves;
        IMove currentMove;
        while (winner == IBoard.NONE_WIN) {
            moves = board.getPlayoutMoves(options.useHeuristics);
            currentMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
            // Check if the move can be made, otherwise remove it from the list
            board.doAIMove(currentMove, currentPlayer);
            winner = board.checkPlayoutWin();
            currentPlayer = board.getPlayerToMove();
        }
        return winner;
    }

    public TreeNode getBestChild() {
        return children.get(0);
    }

    private void updateStats(int value) {
        if (value == IBoard.DRAW)
            stats.pushDraw();
        else
            stats.pushWin(value); // TODO What if multiple players win?!?!
    }

    private final Comparator<TreeNode> comparator = new Comparator<TreeNode>() {
        @Override
        public int compare(TreeNode o1, TreeNode o2) {
            return Double.compare(o2.stats.mean(playerToMove), o1.stats.mean(playerToMove));
        }
    };

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    public List<TreeNode> getChildren() {
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