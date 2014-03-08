package ai;

/**
 * Point biserial correlation
 */
public class PBC {
    private double[][] data = new double[2][1000000];
    private int n = 0;
    private double sum, mean, m2;

    private int wins = 0, losses = 0;
    private double mW = 0, mL = 0;

    public void reset() {
        n = 0;
        sum = 0;
        mean = 0;
        m2 = 0;
        wins = 0;
        losses = 0;
        mW = 0;
        mL = 0;
    }

    public void push(int value1, double value2) {
        if (n == data[0].length)
            return;

        if (value1 == 0) {
            data[0][losses] = value2;
            losses++;
            mL += value2;
        } else {
            data[1][wins] = value2;
            wins++;
            mW += value2;
        }
        sum += value2;
        n++;
        // Compute mean and variance for sample 2
        double delta = value2 - mean;
        mean += delta / n;
        m2 += delta * (value2 - mean);

    }

    public double getCovariance() {
//        double mean1 = mL / losses;
//        double mean2 = mW / wins;
//        int max = Math.min(losses, wins);
//        double tot = 0.;
//        for(int i = 0; i < max; i++) {
//            tot += (data[0][i] - mean1) * (data[1][i] - mean2);
//        }
//        return tot / max;
//        return ((mW + mL) / n) - ((mW / wins) * (mL / losses));
        return ((1. - (wins/(double)n)) * (wins/(double)n)) * ((mW/wins) - (mL/losses));
    }

    public double getCorrelation() {
        return (((mW / wins) - (mL / losses)) / stddev()) * Math.sqrt((wins / (double) n) * (losses / (double) n));
    }

    public double stddev() {
        return Math.sqrt(m2 / (double) (n - 1));
    }

    public double getSign() {
        return Math.signum((mW / wins) - (mL / losses));
    }

    public double variance() {
        return m2 / (double) (n - 1);
    }

    public double getMean() {
        return mean;
    }

    public int getN() {
        return n;
    }
}
