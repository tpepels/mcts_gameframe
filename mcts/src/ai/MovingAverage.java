package ai;

public class MovingAverage {
    private double[] samples;
    private double total = 0., size;
    private int index, maxSize;

    public MovingAverage(int size) {
        this.maxSize = size;
        this.size = 0.;
        this.index = 0;
        //
        samples = new double[maxSize];
    }

    public static void main(String[] args) {
        MovingAverage ma = new MovingAverage(10);
        for (int i = 0; i < 10; i++) {
            ma.add(i + 1);
        }
        System.out.println(ma.getAverage());
        ma.reset();
        for (int i = 0; i < 10; i++) {
            ma.add(i + 1);
        }
        System.out.println(ma.getAverage());
    }

    public void reset() {
        index = 0;
        size = 0;
        total = 0;
    }

    public void add(double sample) {
        // Number of samples < window size
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

    public double getAverage() {
        return total / size;
    }

}
