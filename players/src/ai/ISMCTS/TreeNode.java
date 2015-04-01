package ai.ISMCTS;

import ai.MCTSOptions;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastLog;
import framework.util.NPlayerStats;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    public int nPrime = 0, playerToMove;
    private final MCTSOptions options;
    public NPlayerStats stats; // Stats per player
    private ArrayList<TreeNode> children;
    private IMove move;
    private boolean simulated = false;

    /**
     * Constructor for the root
     */
    public TreeNode(int playerToMove, int nPlayers, MCTSOptions options) {
        this.options = options;
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

    public int MCTS(IBoard board) {
        if (children == null)
            children = new ArrayList<>();
        // Expand returns an expanded leaf if any was added to the tree
        TreeNode child = expand(board, children, stats.getNPlayers(), options);
        // Select the best child, if we didn't find a winning position in the expansion
        int result = board.checkWin();
        boolean isTerminal = (result != IBoard.NONE_WIN);
        if (!isTerminal) {
            // Select a child node
            if (child == null)
                child = select(board);
            // Perform the move
            board.doAIMove(child.getMove(), board.getPlayerToMove());
            if (!child.simulated) {
                // Roll-out
                result = child.playOut(board);
                child.updateStats(result);
                child.simulated = true;
            } else {
                // Tree
                result = child.MCTS(board);
            }
        }
        // Back-prop
        updateStats(result);
        return result;
    }

    /**
     * Adds a single node to the tree
     *
     * @param board The Board
     * @return The expanded node
     */
    public static TreeNode expand(IBoard board, List<TreeNode> children, int nPlayers, MCTSOptions options) {
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        moves.shuffle();
        int winner = board.checkWin();
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;

        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            boolean exists = false;
            // Check here if the move is already in tree
            for (TreeNode node : children) {
                if (node.move.equals(moves.get(i)) && node.playerToMove == board.getPlayerToMove()) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                TreeNode newNode = new TreeNode(board.getPlayerToMove(), nPlayers, moves.get(i), options);
                children.add(newNode);
                newNode.nPrime++;
                // We have a new node, no need to look further.
                return newNode;
            }
        }
        // No node was added to the tree
        return null;
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
            if (!board.isLegal(c.getMove()))
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

    @SuppressWarnings("ConstantConditions")
    private int playOut(IBoard board) {
        int winner = board.checkWin();
        List<IMove> moves;
        IMove currentMove;
        while (winner == IBoard.NONE_WIN) {
            moves = board.getPlayoutMoves(options.useHeuristics);
            currentMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
            // Check if the move can be made, otherwise remove it from the list
            board.doAIMove(currentMove, board.getPlayerToMove());
            winner = board.checkPlayoutWin();
        }
        return winner;
    }

    public TreeNode getBestChild(IBoard board) {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : children) {
            // If the game is partial observable, moves in the tree may not be illegal
            if (!board.isLegal(t.getMove()))
                continue;
            // For partial observable games, use the visit count, not the values.
            value = t.getnVisits();
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

    private void updateStats(int value) {
        if (value == IBoard.DRAW)
            stats.pushDraw();
        else
            stats.pushWin(value); // TODO What if multiple players win?!?!
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
