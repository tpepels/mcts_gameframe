package ai;

public class MovingAverage {
    public final int INIT_SIZE = 10;
    //
    private double[] samples;
    private double total = 0.;
    private int index = 0, size = 0, maxSize;

    public MovingAverage(int size) {
        this.maxSize = size;
        //
        samples = new double[INIT_SIZE];
    }

    public void reset() {
        index = 0;
        size = 0;
        //
        total = 0.;
    }

    public void add(double sample) {
        // Number of samples < sliding window size
        if (size < maxSize) {
            size++;
            // Grow the array to the final size
            if (size == INIT_SIZE) {
                double[] newSamples = new double[maxSize];
                System.arraycopy(samples, 0, newSamples, 0, INIT_SIZE);
                this.samples = newSamples;
            }
        } else {
            // Number of samples > window size
            // Index is at the position to be overwritten
            total -= samples[index];
        }
        samples[index++] = sample;
        total += sample;
        // Reset the index to start at the beginning of the array
        if (index == maxSize) {
            index = 0;
        }
    }

    public double getSize() {
        return size;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public double getAverage() {
        return total / (double) size;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int tot = 0;
        for (int i = 0; i < index; i++) {
            tot += samples[i];
            sb.append((int) samples[i]);
            sb.append(", ");
        }
        for (int i = index; i < maxSize; i++) {
            tot += samples[i];
            sb.append((int) samples[i]);
            sb.append(", ");
        }
        sb.insert(0, tot);
        return sb.toString();
    }
}
