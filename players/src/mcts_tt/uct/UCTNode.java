package mcts_tt.uct;

import ai.MCTSOptions;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastLog;
import mcts_tt.transpos.State;
import mcts_tt.transpos.TransposTable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UCTNode {
    public int player, ply, visits = 0;
    private long hash;
    //
    private final MCTSOptions options;
    //
    private boolean expanded = false, simulated = false;
    private List<UCTNode> children;
    private final TransposTable tt;
    private IMove move;
    private State state;

    /**
     * Constructor for the root
     */
    public UCTNode(int player, MCTSOptions options, IBoard board, TransposTable tt) {
        this.player = player;
        this.options = options;
        this.ply = 0;
        this.tt = tt;
        this.hash = board.hash();
        this.state = tt.getState(hash, true);
    }

    /**
     * Constructor for internal node
     */
    public UCTNode(int player, int ply, IMove move, MCTSOptions options, IBoard board, TransposTable tt) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.ply = ply;
        this.tt = tt;
        this.hash = board.hash();
        this.state = tt.getState(hash, true);
    }

    /**
     * Run the MCTS algorithm on the given node.
     *
     * @param board The current board
     * @return the currently evaluated playout value of the node
     */
    public double MCTS(IBoard board, int depth) {
        if (board.hash() != hash)
            throw new RuntimeException("Incorrect hash");
        UCTNode child = null;
        // First add some leafs if required
        if (isLeaf()) {
            // Expand returns any node that leads to a win
            child = expand(board, depth + 1);
        }
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal())
                child = this;
            else
                child = select();
        }
        //
        if (child.player < 0)
            throw new RuntimeException("Child player weird!");

        double result = 0;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.getValue()) != State.INF) {
            // Execute the move represented by the child
            if (!isTerminal())
                board.doAIMove(child.getMove(), player);

            // When a leaf is reached return the result of the playout
            if (!child.isSimulated() || child.isTerminal()) {
                result = child.playOut(board);
                child.updateStats(-result);
                child.simulated = true;
            } else {
                result = -child.MCTS(board, depth + 1);
            }
            // set the board back to its previous configuration
            if (!isTerminal())
                board.undoMove();
        }
        // Could be solved deeper in the tree as a transposition
        if (Math.abs(child.getValue()) == State.INF)
            result = child.getValue();

        // result is now in view of me in all cases
        if (options.solver) {
            // (Solver) If one of the children is a win, then I'm a win
            if (result == State.INF) {
                // If I have a win, my parent has a loss.
                setSolved(false);
                return result;
            } else if (result == -State.INF) {
                // (Solver) Check if all children are a loss
                for (UCTNode tn : children) {
                    // Are all children a loss?
                    if (tn.getValue() != result) {
                        // Return a single loss, if not all children are a loss
                        updateStats(1);
                        return -1;
                    }
                }
                // (Solver) If all children lead to a loss for the opponent, then I'm a win
                setSolved(true);
                return result; // always return in view of me
            }
        }
        if (board.hash() != hash)
            throw new RuntimeException("Incorrect hash");
        // Update the results for the current node
        updateStats(result);
        // Back-propagate the result always return in view of me
        return result;
    }

    private UCTNode expand(IBoard board, int depth) {
        expanded = true;
        int nextPlayer = board.getOpponent(board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        UCTNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (children == null)
            children = new ArrayList(moves.size());
        //
        int winner = board.checkWin();
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            // If the game is partial observable, we don't want to do the solver part
            if (board.doAIMove(moves.get(i), player)) {
                UCTNode child;
                // Initialize the child
                child = new UCTNode(nextPlayer, depth, moves.get(i), options, board, tt);
                if (options.solver && !child.isSolved()) {
                    // Check for a winner, (Solver)
                    winner = board.checkWin();
                    //
                    if (winner == player) {
                        winNode = child;
                        child.setSolved(true);
                    } else if (winner == nextPlayer) {
                        child.setSolved(false);
                    }
                }
                children.add(child);
                // reset the board
                board.undoMove();
            }
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private UCTNode select() {
        UCTNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        // Use UCT down the tree
        double uctValue, np = getVisits();
        // Select a child according to the UCT Selection policy
        for (UCTNode c : children) {
            double nc = c.getVisits();
            // Always select a proven win
            if (c.getValue() == State.INF)
                uctValue = State.INF + MCTSOptions.r.nextDouble();
            else if (c.getVisits() == 0 && c.getValue() != -State.INF) {
                // First, visit all children at least once
                uctValue = 100 + MCTSOptions.r.nextDouble();
            } else if (c.getValue() == -State.INF) {
                uctValue = -State.INF + MCTSOptions.r.nextDouble();
            } else {
                // Compute the uct value with the (new) average value
                uctValue = c.getValue() + options.uctC * Math.sqrt(FastLog.log(np) / nc);
            }
            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    private double playOut(IBoard board) {
        boolean gameEnded, moveMade, interrupted = false;
        int currentPlayer = board.getPlayerToMove(), nMoves = 0;
        List<IMove> moves;
        int winner = board.checkWin();
        gameEnded = (winner != IBoard.NONE_WIN);
        IMove currentMove;

        while (!gameEnded && !interrupted) {
            moves = board.getPlayoutMoves(options.useHeuristics);
            moveMade = false;
            while (!moveMade) {
                // All moves were thrown away, the game is a draw
                if (moves.size() == 0) {
                    gameEnded = true;
                    // The current player has no moves left
                    if (board.noMovesIsDraw())
                        winner = IBoard.DRAW;                                   // Pentalath, Lost Cities
                    else                                                        // Last player to move:
                        winner = board.getOpponent(board.getPlayerToMove());    // Cannon, Amazons, Chinese Checkers, Checkers
                    break;
                }
                currentMove = moves.get(MCTSOptions.r.nextInt(moves.size()));
                // Check if the move can be made, otherwise remove it from the list
                if (board.doAIMove(currentMove, currentPlayer)) {
                    nMoves++;
                    moveMade = true;
                    winner = board.checkPlayoutWin();
                    gameEnded = winner != IBoard.NONE_WIN;
                    currentPlayer = board.getPlayerToMove();
                } else {
                    // The move was illegal, remove it from the list.
                    moveMade = false;
                    moves.remove(currentMove);
                }
                if (!gameEnded && options.earlyEval && nMoves == options.pdepth) {
                    interrupted = true;
                }
            }
        }

        double score = 0.;
        if (gameEnded) {
            if (winner == player) score = 1.0;
            else if (winner == IBoard.DRAW) score = 0.0;
            else score = -1;
        } else if (interrupted) {
            double eval = board.evaluate(player, options.efVer);
            //System.out.println(eval);
            if (eval > options.detThreshold)
                score = 1.;
            else if (eval < -options.detThreshold)
                score = -1.;
        }
        // Undo the moves done in the playout
        for (int i = 0; i < nMoves; i++)
            board.undoMove();
        return score;
    }

    public UCTNode getBestChild() {
        double max = Double.NEGATIVE_INFINITY, value;
        UCTNode bestChild = null;
        for (UCTNode t : children) {
            // If there are children with INF value, choose one of them
            if (t.getValue() == State.INF)
                value = State.INF + MCTSOptions.r.nextDouble();
            else if (t.getValue() == -State.INF)
                value = -State.INF + t.getVisits() + MCTSOptions.r.nextDouble();
            else {
                value = t.getValue();
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

    private void updateStats(double value) {
        if (state == null)
            state = tt.getState(hash, false);
        if (value == -1)
            state.updateStats(player);
        else
            state.updateStats(3 - player);
        visits++;
    }

    private void setSolved(boolean win) {
        if (state == null)
            state = tt.getState(hash, false);
        if (win)
            state.setSolved(3 - player);
        else
            state.setSolved(player);
    }

    /**
     * @return The value of this node with respect to the parent
     */
    public double getValue() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getMean(3 - player);
    }

    /**
     * @return The number of visits of the transposition
     */
    private double getVisits() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getVisits();
    }

    private boolean isSolved() {
        return Math.abs(getValue()) == State.INF;
    }

    public boolean isLeaf() {
        return children == null || !expanded;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    public IMove getMove() {
        return move;
    }

    @Override
    public String toString() {
        DecimalFormat df2 = new DecimalFormat("###,##0.00000");
        return move + "\tValue: " + df2.format(getValue()) + "\tVisits: " + getVisits();
    }
}
