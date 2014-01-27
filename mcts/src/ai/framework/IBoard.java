package ai.framework;

import java.util.List;
import ai.StatCounter;

public interface IBoard {
    /**
     * Win/loss/draw and player definitions, don't change these
     */
    public final int NONE_WIN = 0, P1_WIN = 1, P2_WIN = 2, DRAW = 3;
    public final int P1 = 1, P2 = 2;

    /**
     * Is called before every iteration to create a new determinization.
     * If your game is fully observable, leave the implementation blank.
     *
     * @param myPlayer The player running the algorithm
     */
    public void newDeterminization(int myPlayer);

    /**
     * Should return true if the game is not fully observable
     *
     * @return true if not fully observable
     */
    public boolean isPartialObservable();

    /**
     * Should return the number of moves made in the current game
     *
     * @return The number of moves made so far
     */
    public int getNMovesMade();

    /**
     * In partially observable games, the legality of a move is tested during selection.
     * If your game is fully observable, simply return true
     *
     * @param move the move to check
     * @return true, if the move is legal, false otherwise
     */
    public boolean isLegal(IMove move);

    /**
     * Should return true if the game allows draws.
     *
     * @return True, if draws are possible, false otherwise
     */
    public boolean noMovesIsDraw();

    /**
     * Copy the current state of the board to a new board.
     *
     * @return A new board with the exact same state, should contain no references to the original
     */
    public IBoard copy();

    /**
     * Do a complete AI move, do not check for winning positions in this method
     *
     * @param move   The move to perform
     * @param player The index of the player making the move
     * @return false if the move was illegal, otherwise true
     */
    public boolean doAIMove(IMove move, int player);

    /**
     * Get all moves that can be used for expanding the current node
     *
     * @return a list of legal moves, may contain illegal moves tested be doAIMove
     */
    public MoveList getExpandMoves();

    /**
     * Get all moves that can be used to simulate the current game
     *
     * @return a list of all moves (can contain illegal moves)
     */
    public List<IMove> getPlayoutMoves(boolean heuristics);

    /**
     * Undo the last move made on the board
     */
    public void undoMove();

    /**
     * Get the opponent index of the given player
     *
     * @param player the player index
     * @return the opponent of the player index
     */
    public int getOpponent(int player);

    /**
     * Check whether the game is in a win-loss state
     *
     * @return NONE_WIN = 0, P1_WIN = 1, P2_WIN = 2, DRAW = 3
     */
    public int checkWin();

    /**
     * Check whether the game is in a win-loss state for a simulation
     *
     * @return NONE_WIN = 0, P1_WIN = 1, P2_WIN = 2, DRAW = 3
     */
    public int checkPlayoutWin();

    /**
     * Get the player that is currently active (to move)
     *
     * @return current player
     */
    public int getPlayerToMove();

    /**
     * Gets the highest unique move-id for this game.
     * Each move has a unique ID for storing it in the MAST array.
     *
     * @return Highest move-id
     */
    public int getMaxUniqueMoveId();

    public void initialize();

    /**
     * Returns a value between -1 and 1 indicating the heuristic value of the
     * position with respect to the specified player.
     */
    public double evaluate(int player, int version);
    
    /**
     * Used to initialize a node's stat counter with wins and losses.
     * Note: player is the parent player.
     */
    public void initNodePriors(int parentPlayer, StatCounter stats, IMove move, int npvisits);

    /**
     * Returns a value between -1 and 1 representing the quality of the game
     * after the game has ended, e.g. rate of pieces remaining, score ratio etc.
     * <p/>
     * If this is not applicable to your game, return 1
     */
    public double getQuality();
}
