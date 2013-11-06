package ai;
/*
 * Stores stats without keep track of the actual numbers.
 *
 * Implements Knuth's online algorithm for variance, first one
 * found under "Online Algorithm" of
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
 *
 */

public class StatCounter {

    private double m_sum, m_m2, m_mean;
    private int m_n;

    public StatCounter() {
        this.reset();
    }

    public void reset() {
        m_sum = 0.0;
        m_m2 = 0.0;
        m_mean = 0.0;
        m_n = 0;
    }

    public void push(double num) {
        m_sum += num;
        m_n++;

        double delta = num - m_mean;
        m_mean += delta / m_n;
        m_m2 += delta * (num - m_mean);
    }

    public double variance() {
        return m_m2 / (double) m_n;
    }

    public double stddev() {
        return Math.sqrt(variance());
    }

    public double mean() {
        return m_sum / (double) m_n;
    }

    public int visits() {
        return m_n;
    }

    public double ci95() {
        return (1.96 * stddev() / Math.sqrt(m_n));
    }
}
