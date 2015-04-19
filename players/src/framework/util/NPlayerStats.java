package framework.util;

import framework.IBoard;

import java.text.DecimalFormat;

/**
 * Created by Tom Pepels on 25-3-2015.
 * tom.pepels@d-cis.nl | Thales Research and Technology - Delft
 */
public class NPlayerStats {

    private final int[] util;
    private int n;

    public NPlayerStats(int nPlayers) {
        util = new int[nPlayers];
    }

    public void push(int winningPlayer) {
        if (winningPlayer != IBoard.DRAW) {
            //
            for (int i = 0; i < util.length; i++) {
                if ((winningPlayer - 1) == i)
                    util[i]++;
                else
                    util[i]--;
            }
        }
        n++;
    }

    public void push(int[] rewards) {
        for (int i = 0; i < util.length; i++) {
            util[i] += rewards[i];
        }
        n++;
    }

    public double mean(int player) {
        if (n > 0) {
            return (util[player - 1] / (double) n);
        } else {
            return 0.;
        }
    }

    public int getNPlayers() {
        return util.length;
    }

    public int getN() {
        return n;
    }

    private static final DecimalFormat df2 = new DecimalFormat("###,##0.000");

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < util.length; i++) {
            sb.append(df2.format(mean(i + 1)));
            if (i < util.length - 1)
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
