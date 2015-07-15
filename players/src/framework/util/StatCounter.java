package framework.util;
/*
 * Stores stats without keep track of the actual numbers.
 *
 * Implements Knuth's online algorithm for variance, first one
 * found under "Online Algorithm" of
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
 *
 */

import ai.MCTSOptions;
import ai.mcts.TreeNode;

import java.text.DecimalFormat;
import java.util.Scanner;

public class StatCounter {
    private MovingAverage ma;
    private boolean windowed = false;
    //
    private int m_wins, m_losses;
    public double m_sum, m_m2, m_mean;
    private int m_n;
    private final MCTSOptions options;

    public StatCounter() {
        this.reset();
        // options are only required for getting the window-size
        this.options = null;
        this.windowed = false;
    }

    public StatCounter(boolean windowed, MCTSOptions options) {
        this.options = options;
        this.windowed = windowed;
        this.reset();
    }

    public StatCounter copyInv() {
        StatCounter newSc = new StatCounter();
        newSc.m_sum = -m_sum;
        newSc.m_mean = -m_mean;
        newSc.m_m2 = -m_m2;
        newSc.m_n = m_n;
        return newSc;
    }

    public StatCounter copy() {
        StatCounter newSc = new StatCounter();
        newSc.m_sum = m_sum;
        newSc.m_mean = m_mean;
        newSc.m_m2 = m_m2;
        newSc.m_n = m_n;
        newSc.m_wins = m_wins;
        newSc.m_losses = m_losses;
        return newSc;
    }

    public void reset() {
        m_wins = 0;
        m_losses = 0;
        m_sum = 0.0;
        m_m2 = 0.0;
        m_mean = 0.0;
        m_n = 0;
        //
        if (ma != null)
            ma.reset();
    }

    public void initWinsLosses(double winrate, int visits) {
        // test
        m_wins = (int) (winrate * visits);
        m_losses = visits - m_wins;
        m_sum = (m_wins - m_losses);
        m_n = visits;
        m_mean = m_sum / m_n;

        double p = ((double) m_wins) / visits;
        m_m2 = p * (1 - p) * m_n;
    }


    public void push(double num) {
        if (Math.abs(m_mean) == TreeNode.INF)
            throw new RuntimeException("Mean is INF in push");
        if (Double.isInfinite(m_sum) || Double.isInfinite(m_mean))
            throw new RuntimeException("Something is infinite in push");
        m_n++;
        if (Math.signum(num) > 0)
            m_wins++;
        else if (num != 0)
            m_losses++;
        // If the node is visited a few times, create the window
        if (windowed && m_n == 2) {
            // The size of the window is based on the number of simulations remaining
            int size = options.getWindowSize();
            if (size > 0) {
                ma = new MovingAverage(size);
                ma.add(m_sum);                  // Store the first value, the current one will be added later
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
        if (ma != null) ma.add(num);
        //
        if (Double.isInfinite(m_sum) || Double.isInfinite(m_mean))
            throw new RuntimeException("Something is infinite in push");
        if (Double.isNaN(m_mean))
            throw new RuntimeException("Mean is NaN in push");
    }

    public void setValue(double val) {
        m_mean = val;
    }

    public void subtract(StatCounter statCounter, boolean isOpp) {
        if (Math.abs(m_mean) == TreeNode.INF)
            throw new RuntimeException("Left mean is INF in subtract");
        if (Math.abs(statCounter.m_mean) == TreeNode.INF)
            throw new RuntimeException("Right mean is INF in subtract");
        if (isOpp) {
            m_wins -= statCounter.m_losses;
            m_losses -= statCounter.m_wins;
            // Add, because its in view of opponent
            m_sum += statCounter.m_sum;
        } else {
            m_wins -= statCounter.m_wins;
            m_losses -= statCounter.m_losses;
            // Add, because its in view of opponent
            m_sum -= statCounter.m_sum;
        }
        m_n -= statCounter.m_n;
        if (m_n > 0)
            m_mean = m_sum / m_n;
        if (Double.isNaN(m_mean))
            throw new RuntimeException("Mean is NaN in subtract");
    }

    public void add(StatCounter statCounter, boolean isOpp) {
        if (Math.abs(m_mean) == TreeNode.INF)
            return;
        if (Math.abs(statCounter.m_mean) == TreeNode.INF) {
            this.m_mean = statCounter.m_mean;
            return;
        }
        if (isOpp) {
            m_wins += statCounter.m_losses;
            m_losses += statCounter.m_wins;
            // Subtract, because its in view of opponent
            m_sum -= statCounter.m_sum;
        } else {
            m_wins += statCounter.m_wins;
            m_losses += statCounter.m_losses;
            // Add, because its in view of myself
            m_sum += statCounter.m_sum;
        }
        m_n += statCounter.m_n;
        if (m_n > 0)
            m_mean = m_sum / m_n;
        if (Double.isNaN(m_mean))
            throw new RuntimeException("Mean is NaN in add");
    }

    public double variance() {
        return m_m2 / (double) m_n;
    }

    public double stddev() {
        return Math.sqrt(variance());
    }

    public double mean() {
        if (Double.isNaN(m_mean))
            throw new RuntimeException("Mean is NaN in getMean");

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

    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat("00.0");
        Scanner scanner = new Scanner(System.in);
        StatCounter stat = new StatCounter();
        while(true) {
            System.out.print("Enter value: ");
            String in = scanner.nextLine();
            int val = Integer.parseInt(in);
            if(in.equals("."))
                break;
            System.out.println("n: ");
            in = scanner.nextLine();
            int n = Integer.parseInt(in);
            for(int i = 0; i < n; i++)
                stat.push(val);
        }
        System.out.println(df.format(stat.mean()) + "\\%$\\pm$" + df.format(stat.ci95()));
    }

    public String toString() {
        return "value: " + mean() + " visits: " + visits();
    }
}
