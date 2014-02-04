package ai;

public class MovingAverageSorted {
    public static int instances = 0, smallInstances = 0, grown = 0, full = 0;
    public final double INIT_SIZE = .2;
    private int curSize = 0;
    //
    private double[] samples;
    private int[] depths;
    private double total = 0.;
    private int index = 0, size = 0, maxSize;

    public MovingAverageSorted(int size) {
        this.maxSize = size;
        //
        curSize = (int)(INIT_SIZE * size);
        if(curSize <= 10) {
            curSize = size;
        } else {
            smallInstances++;
        }
        instances++;
        samples = new double[curSize];
        depths = new int[curSize];
    }

    public void reset() {
        index = 0;
        size = 0;
        //
        total = 0.;
    }

    public void add(double sample, int depth) {

        // Number of samples < swUCT size
        if (size < maxSize) {
            size++;
            // Grow the array to the final size
            if (maxSize != curSize && size == curSize) {
                grown++;
                double[] newSamples = new double[maxSize];
                int[] newDepths = new int[maxSize];
                System.arraycopy(samples, 0, newSamples, 0, curSize);
                System.arraycopy(depths, 0, newDepths, 0, curSize);
                this.samples = newSamples;
                this.depths = newDepths;
                curSize = maxSize;
            }
        } else {
            // Number of samples > window size
            // Index is at the position to be overwritten
            total -= samples[index];
        }
        //
        depths[index] = (depth * 10000) + index;
        samples[index++] = sample;
        total += sample;
        // Reset the index to start at the beginning of the array
        if (index == maxSize) {
            index = 0;
            full++;
            // Sort on the depth of the signal
            quickSort(depths, 0, depths.length - 1);
        }
    }

    private void quickSort(int arr[], int left, int right) {
        int index = partition(arr, left, right);
        if (left < index - 1)
            quickSort(arr, left, index - 1);
        if (index < right)
            quickSort(arr, index, right);
    }

    private int partition(int arr[], int left, int right) {
        int i = left, j = right;
        int tmp;
        double tmp2;
        int pivot = arr[(left + right) / 2];

        while (i <= j) {
            while (arr[i] < pivot)
                i++;
            while (arr[j] > pivot)
                j--;
            if (i <= j) {
                tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
                //
                tmp2 = samples[i];
                samples[i] = samples[j];
                samples[j] = tmp2;
                i++;
                j--;
            }
        }
        return i;
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