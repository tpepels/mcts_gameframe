package framework;

public interface FiniteBoard extends IBoard {

    /**
     * @return The maximum horizon H of the game
     */
    public int getHorizon();

    /**
     * @return The zobrist hash of the current gamestate
     */
    public long getStateHash();
}
