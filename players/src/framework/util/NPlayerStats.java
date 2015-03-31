package framework.util;

import java.text.DecimalFormat;

/**
 * Created by Tom Pepels on 25-3-2015.
 * tom.pepels@d-cis.nl | Thales Research and Technology - Delft
 */
public class NPlayerStats {

    private final int[] wins;
    private int n;

    public NPlayerStats(int nPlayers) {
        wins = new int[nPlayers];
    }

    public void pushWin(int winner) {
        wins[winner - 1]++;
        n++;
    }

    public void pushDraw() {
        n++;
    }

    public double mean(int player) {
        if (n > 0) {
            double sum = wins[player - 1];
            for (int i = 0; i < wins.length; i++) {
                if (i == (player - 1))
                    continue;
                // Subtract the scores of the opponents
                sum -= wins[i];
            }
            return (sum / (double) n);
        } else {
            return 0.;
        }
    }

    public int getNPlayers() {
        return wins.length;
    }

    public int getN() {
        return n;
    }

    private static final DecimalFormat df2 = new DecimalFormat("###,##0.000");

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < wins.length; i++) {
            sb.append(df2.format(mean(i + 1)));
            if (i < wins.length - 1)
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
