package ai;

public class Covariance {

    private final double[] samples1, samples2;
    private final int maxSamples;
    //
    private int n = 0;
    private double sum1, sum2, mean1, mean2, m21, m22;

    public Covariance(int maxSamples) {
        samples1 = new double[maxSamples];
        samples2 = new double[maxSamples];
        this.maxSamples = maxSamples;
    }

    public void reset() {
        n = 0;
        sum1 = 0.;
        sum2 = 0.;
    }

    public void push(double value1, double value2) {

        if(n >= maxSamples)
            return;

        samples1[n] = value1;
        sum1 += value1;
        samples2[n] = value2;
        sum2 += value2;
        n++;
        // Compute mean and variance for sample 1
        double delta = n - mean1;
        mean1 += delta / n;
        m21 += delta * (n - mean1);
        // Compute mean and variance for sample 2
        delta = n - mean2;
        mean2 += delta / n;
        m22 += delta * (n - mean2);

    }

    public double getCovariance() {
        double mean1 = sum1 / n, mean2 = sum2 / n;

        double covariance = 0.;
        for(int i = 0; i < n; i++) {
            covariance += ((samples1[i] - mean1) * (samples2[i] - mean2)) / n;
        }

        return covariance;
    }

    public double getCorrelation() {
        return getCovariance() / (stddev1() * stddev2());
    }

    public double stddev1() {
        return Math.sqrt(m21 / (double) n);
    }

    public double stddev2() {
        return Math.sqrt(m22 / (double) n);
    }
}
