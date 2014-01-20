package ai;

/**
 * Point biserial correlation
 */
public class PBC {
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
        if (value1 == 0) {
            losses++;
            mL += value2;
        } else {
            wins++;
            mW += value2;
        }
        sum += value2;
        n++;
        // Compute mean and variance for sample 1
        double delta = value2 - mean;
        mean += delta / n;
        m2 += delta * (value2 - mean);

    }

    public double getCorrelation() {
        return (((mW/wins) - (mL/losses)) / stddev()) * Math.sqrt((wins / (double)n) * (losses /(double)n));
    }

    public double stddev() {
        return Math.sqrt(m2 / (double) (n - 1));
    }

    public double variance() {
        return m2 / (double) (n - 1);
    }

    public double getMean2() {
        return mean;
    }

    public int getN() {
        return n;
    }
}
