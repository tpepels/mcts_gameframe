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
    private final boolean hiddenMove;
    private final MCTSOptions options;
    public NPlayerStats stats; // Stats per player
    private ArrayList<TreeNode> children;
    private IMove move;
    private boolean simulated = false;

    /**
     * Constructor for the root
     */
    public TreeNode(int playerToMove, MCTSOptions options) {
        this.options = options;
        this.playerToMove = playerToMove;
        this.hiddenMove = false;
        stats = new NPlayerStats(2);
    }

    /**
     * Constructor for internal node
     */
    public TreeNode(int playerToMove, IMove move, MCTSOptions options) {
        this.playerToMove = playerToMove;
        this.move = move;
        this.options = options;
        this.hiddenMove = false;
        stats = new NPlayerStats(2);
    }

    /**
     * Constructor for hidden-move node
     */
    public TreeNode(int playerToMove, boolean hiddenMove, MCTSOptions options) {
        this.playerToMove = playerToMove;
        this.hiddenMove = hiddenMove;
        this.options = options;
        stats = new NPlayerStats(2);
    }

    public int MCTS(IBoard board, int visiblePlayer) {
        if (children == null)
            children = new ArrayList<>();
        // Expand returns an expanded leaf if any was added to the tree
        TreeNode child = expand(board, children, options, visiblePlayer);
        // Select the best child, if we didn't find a winning position in the expansion
        int result = board.checkWin();
        boolean isTerminal = (result != IBoard.NONE_WIN);
        if (!isTerminal) {
            // Select a child node
            if (child == null)
                child = select(board);
            // Do the opponent's hidden move before descending
            if (child.hiddenMove) {
                MoveList ml = board.getExpandMoves();
                // Randomly select a hidden move (SO-ISMCTS)
                IMove hMove = ml.get(MCTSOptions.r.nextInt(ml.size()));
                board.doAIMove(hMove, board.getPlayerToMove());
            } else {
                // Perform the move
                board.doAIMove(child.getMove(), board.getPlayerToMove());
            }
            if (!child.simulated || options.flat) {
                // Roll-out
                result = child.playOut(board);
                child.updateStats(result);
                child.simulated = true;
            } else {
                // Tree
                result = child.MCTS(board, visiblePlayer);
            }
        }
        // Back-prop
        updateStats(result);
        return result;
    }

    public static int MCTS(IBoard board, int visiblePlayer, TreeNode node1, TreeNode node2) {
        if (node1.children == null)
            node1.children = new ArrayList<>();
        if (node2.children == null)
            node2.children = new ArrayList<>();

        // Expand returns an expanded leaf if any was added to the tree
        TreeNode child, child1, child2;
        child1 = node1.expand(board, node1.children, node1.options, visiblePlayer);
        child2 = node2.expand(board, node2.children, node2.options, board.getOpponent(visiblePlayer));
        // Select the best child, if we didn't find a winning position in the expansion
        int result = board.checkWin();
        boolean isTerminal = (result != IBoard.NONE_WIN);
        if (!isTerminal) {
            // Select a child node
            if (child1 == null)
                child1 = node1.select(board);
            if (child2 == null)
                child2 = node2.select(board);
            // Check which tree has the current move to make
            if (board.getPlayerToMove() == visiblePlayer)
                child = child1;
            else
                child = child2;
            // Perform the move
            board.doAIMove(child.getMove(), board.getPlayerToMove());
            if (!child.simulated || child.options.flat) {
                // Roll-out
                result = child.playOut(board);
                child1.updateStats(result);
                child2.updateStats(result);
                child1.simulated = true;
                child2.simulated = true;
            } else {
                // Tree
                result = MCTS(board, visiblePlayer, child1, child2);
            }
        }
        // Back-prop
        node1.updateStats(result);
        node2.updateStats(result);
        return result;
    }

    /**
     * Adds a single node to the tree
     *
     * @param board The Board
     * @return The expanded node
     */
    public static TreeNode expand(IBoard board, List<TreeNode> children, MCTSOptions options, int visiblePlayer) {
        if (board.poMoves() && board.getPlayerToMove() != visiblePlayer) {
            for (TreeNode c : children)
                if (c.hiddenMove)
                    return c;
            // The aggregate node for the hidden player was not yet made
            TreeNode newNode = new TreeNode(board.getPlayerToMove(), true, options);
            children.add(newNode);
            return newNode;
        }
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
                if (board.poMoves() && node.hiddenMove)
                    continue;

                if (node.move.equals(moves.get(i)) && node.playerToMove == board.getPlayerToMove()) {
                    exists = true;
                    break;
                }
            }
            // Add the node to the tree if it didn't exist
            if (!exists) {
                TreeNode newNode = new TreeNode(board.getPlayerToMove(), moves.get(i), options);
                children.add(newNode);
                newNode.nPrime++;
                // It may be possible that getExpandMoves returns illegal moves
                if (board.isLegal(moves.get(i)))
                    return newNode;
            }
        }
        // No node was added to the tree
        return null;
    }

    private TreeNode select(IBoard board) {
        TreeNode selected = null;
        double bestValue = Double.NEGATIVE_INFINITY, uctValue;
        // Select a child according to the UCT Selection policy
        for (TreeNode c : children) {
            // If the game is partially observable, moves in the tree may not be legal
            if (board.getPlayerToMove() != c.playerToMove || !board.isLegal(c.getMove()))
                continue;
            // First, visit all children at least once
            if (c.nPrime == 0)
                uctValue = 100. + MCTSOptions.r.nextDouble();
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

    public int playOut(IBoard board) {
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
        //System.out.println("Winner = " + winner);
        //System.out.println(board);
        return winner;
    }

    public TreeNode getBestChild() {
        double max = Double.NEGATIVE_INFINITY, value;
        TreeNode bestChild = null;
        for (TreeNode t : children) {
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

    public void updateStats(int winner) {
        stats.push(winner);
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

    @Override
    public String toString() {
        return move + "\tValue: " + stats + "\tVisits: " + getnVisits() + "\tn': " + nPrime;
    }
}
