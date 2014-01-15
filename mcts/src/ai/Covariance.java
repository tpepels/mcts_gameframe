package ai;

public class Covariance {
    private int n = 0;
    private double sum1, sum2, mean1, mean2, m21, m22, covSum;

    public void reset() {
        n = 0;
        sum1 = 0.;
        sum2 = 0.;
        covSum = 0.;
        mean1 = 0.;
        mean2 = 0.;
        m21 = 0.;
        m22 = 0.;
    }

    public void push(double value1, double value2) {
        sum1 += value1;
        sum2 += value2;
        n++;
        covSum += value1 * value2;
        // Compute mean and variance for sample 1
        double delta = value1 - mean1;
        mean1 += delta / n;
        m21 += delta * (value1 - mean1);
        // Compute mean and variance for sample 2
        delta = value2 - mean2;
        mean2 += delta / n;
        m22 += delta * (value2 - mean2);

    }

    public double getCovariance() {
        return (covSum - ((sum1 * sum2) / n)) / (double) (n - 1);
    }

    public double getCorrelation() {
        return getCovariance() / (stddev1() * stddev2());
    }

    public double stddev1() {
        return Math.sqrt(m21 / (double) (n - 1));
    }

    public double stddev2() {
        return Math.sqrt(m22 / (double) (n - 1));
    }

    public double variance1() {
        return m21 / (double) (n - 1);
    }

    public double variance2() {
        return m22 / (double) (n - 1);
    }

    public double getMean2() {
        return mean2;
    }

    public int getN() {
        return n;
    }
}
