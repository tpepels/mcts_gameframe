package ai.framework;

import ai.mcts.MCTSOptions;

public abstract class IMove {
    public abstract int[] getMove();

    public abstract int getType();

    public abstract boolean equals(IMove move);

    public abstract int getUniqueId();

    public abstract boolean isChance();

    public double getHistoryVal(int player, MCTSOptions options) {
        return options.getHistoryValue(player, getUniqueId());
    }

    public int getHistoryVis(int player, MCTSOptions options) {
        return (int) options.getHistoryVisits(player, getUniqueId());
    }

    public abstract boolean isProtected();
}
