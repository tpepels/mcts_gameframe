package ai.framework;

import java.util.List;

public interface IBoard {
    /**
     * Win/loss/draw and player definitions
     */
    public final int NONE_WIN = 0, P1_WIN = 1, P2_WIN = 2, DRAW = 3;
    public final int P1 = 1, P2 = 2;

    /**
     * Should return true if the pentalath.game allows draws.
     *
     * @return True, if draws are possible, false otherwise
     */
    public boolean drawPossible();

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
     * @param player The player to move
     * @return a list of legal moves, may contain illegal moves tested be doAIMove
     */
    public IMove[] getExpandMoves();

    /**
     * Get all moves that can be used to simulate the current pentalath.game
     *
     * @return a list of all moves (can contain illegal moves)
     */
    public List<IMove> getPlayoutMoves();

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
     * Check whether the pentalath.game is in a win-loss state
     *
     * @return NONE_WIN = 0, P1_WIN = 1, P2_WIN = 2, DRAW = 3
     */
    public int checkWin();

    /**
     * Check whether the pentalath.game is in a win-loss state for a simulation
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
     * Gets the highest unique move-id for this pentalath.game.
     * Each move has a unique ID for storing it in the MAST array.
     *
     * @return Highest move-id
     */
    public int getMaxUniqueMoveId();
}
