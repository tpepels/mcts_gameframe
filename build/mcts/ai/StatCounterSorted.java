package ai;

import ai.mcts.MCTSOptions;
import ai.mcts.TreeNode;

public class StatCounterSorted {
    private MovingAverageSorted ma;
    private boolean windowed = false;
    //
    private double m_sum, m_m2, m_mean;
    private int m_n;
    private final MCTSOptions options;

    public StatCounterSorted() {
        this.reset();
        // options are only required for getting the window-size
        this.options = null;
        this.windowed = false;
    }

    public StatCounterSorted(boolean windowed, MCTSOptions options) {
        this.options = options;
        this.windowed = windowed;
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
        m_sum = 0.0;
        m_m2 = 0.0;
        m_mean = 0.0;
        m_n = 0;
        //
        if (ma != null)
            ma.reset();
    }

    public void push(double num, int depth) {
        m_n++;
        // If the node is visited a few times, create the window
        if (windowed && m_n == 2) {
            // The size of the window is based on the number of simulations remaining
            int size = options.getWindowSize();
            if (size > 0) {
                ma = new MovingAverageSorted(size);
                ma.add(m_sum, depth);                  // Store the first value, the current one will be added later
            } else {
                // Make sure no new window is created
                windowed = false;
            }
        }
        m_sum += num;

        double delta = num - m_mean;
        m_mean += delta / m_n;
        m_m2 += delta * (num - m_mean);
        //
        if (ma != null) ma.add(num, depth);
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

    public boolean hasWindow() {
        return ma != null;
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