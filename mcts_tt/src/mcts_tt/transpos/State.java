package mcts_tt.transpos;

import java.util.Arrays;

public class State {
    public static double INF = 999999;
    public long hash;
    public int visits;
    private int[] wins = {0, 0};
    private short solvedPlayer = 0;
    public int lastVisit = 0;
    public boolean visited = false;
    //
    public State next = null;

    public State(long hash) {
        this.hash = hash;
    }

    public void updateStats(int winner) {
        visited = true;
        if (solvedPlayer != 0)
            throw new RuntimeException("updateStats called on solved position!");
        this.wins[winner - 1]++;
        this.visits++;
    }

    public void updateStats(int n, int p1, int p2) {
        this.visits += n;
        wins[0] += p1;
        wins[1] += p2;
    }

    public double getMean(int player) {
        visited = true;
        if (solvedPlayer == 0) { // Position is not solved, return mean
            if (visits > 0)
                return (wins[player - 1] - wins[(3 - player) - 1]) / (double) visits;
            else
                return 0;
        } else    // Position is solved, return inf
            return (player == solvedPlayer) ? INF : -INF;
    }

    public void setSolved(int player) {
        visited = true;
        if (solvedPlayer > 0 && player != solvedPlayer)
            throw new RuntimeException("setSolved with different player!");
        this.solvedPlayer = (short) player;
    }

    public int getVisits() {
        return visits;
    }

    public String toString() {
        if (solvedPlayer == 0)
            return Arrays.toString(wins) + "\tn:" + visits;
        else
            return "solved win P" + solvedPlayer;
    }
}
