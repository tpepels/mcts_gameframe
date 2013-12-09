package ai;

public class MovingAverage {
    private double[] samples;
    private double total = 0.;
    private int index = 0, size = 0, maxSize;

    public MovingAverage(int size) {
        this.maxSize = size;
        //
        samples = new double[maxSize];
    }

    public void reset() {
        index = 0;
        size = 0;
        //
        total = 0.;
    }

    public void add(double sample) {
        // Number of samples < swUCT size
        if (size < maxSize) {
            size++;
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

    public void moveWindow() {
        // Number of samples < swUCT size
        if (size < maxSize) {
            size++;
        } else {
            // Number of samples > window size
            // Index is at the position to be overwritten
            total -= samples[index];
        }
        samples[index++] = 0;
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
