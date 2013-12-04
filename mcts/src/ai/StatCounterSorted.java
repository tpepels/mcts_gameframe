package ai;
/*
 * Stores stats without keep track of the actual numbers.
 *
 * Implements Knuth's online algorithm for variance, first one
 * found under "Online Algorithm" of
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
 *
 */

import ai.mcts.TreeNode;

public class StatCounterSorted {

    public MovingAverageSorted ma;
    private double m_sum, m_m2, m_mean;
    private int m_n;
    private int m_wins;
    private int m_losses;

    public StatCounterSorted() {
        this.reset();
    }

    public StatCounterSorted(int window) {
        ma = new MovingAverageSorted(window);
        this.reset();
    }

    public StatCounterSorted copyInv() {
        StatCounterSorted newSc = new StatCounterSorted();
        newSc.m_sum = -m_sum;
        newSc.m_mean = -m_mean;
        newSc.m_m2 = -m_m2;
        newSc.m_n = m_n;
        return newSc;
    }

    public void reset() {

        m_losses = 0;
        m_wins = 0;

        m_sum = 0.0;
        m_m2 = 0.0;
        m_mean = 0.0;
        m_n = 0;
        //
        if (ma != null)
            ma.reset();
    }

    public void push(double num, int depth) {
        m_sum += num;
        m_n++;

        if (Math.signum(num) > 0)
            m_wins++;
        else if (num != 0)
            m_losses++;

        double delta = num - m_mean;
        m_mean += delta / m_n;
        m_m2 += delta * (num - m_mean);

        if (ma != null) ma.add(num, depth);
    }

    public String wlString() {
        return "W:" + m_wins + " L:" + m_losses;
    }

    public void setValue(double val) {
        m_sum = val;
        m_mean = val;
    }

    public double variance() {
        return m_m2 / (double) m_n;
    }

    public double stddev() {
        return Math.sqrt(variance());
    }

    public double mean() {
        if (ma == null || Math.abs(m_mean) == TreeNode.INF)
            return m_mean;
        else
            return ma.getAverage();
    }

    public double true_mean() {
        return m_mean;
    }

    public double window_mean() {
        return ma.getAverage();
    }

    public double windowSize() {
        if (ma != null)
            return ma.getMaxSize();
        else
            return -1;
    }

    public int visits() {
        if (ma == null)
            return m_n;
        else
            return (int) ma.getSize();
    }

    public int totalVisits() {
        return m_n;
    }

    public double ci95() {
        return (1.96 * stddev() / Math.sqrt(m_n));
    }
}
