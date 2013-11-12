package breakthrough.game;

import ai.framework.IMove;

public class Move implements IMove {
    @Override
    public int[] getMove() {
        return new int[0];
    }

    @Override
    public int getType() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean equals(IMove move) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getUniqueId() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
