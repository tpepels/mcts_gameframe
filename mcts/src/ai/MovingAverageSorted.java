package ai;

public class MovingAverageSorted {
    private double[] samples;
    private int[] depths;
    private double total = 0.;
    private int index = 0, size = 0, maxSize;

    public MovingAverageSorted(int size) {
        this.maxSize = size;
        //
        samples = new double[maxSize];
        depths = new int[maxSize];
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
        } else {
            // Number of samples > window size
            // Index is at the position to be overwritten
            total -= samples[index];
        }
        depths[index] = (depth * 10000) + index;
        samples[index++] = sample;
        total += sample;
        // Reset the index to start at the beginning of the array
        if (index == maxSize) {
            index = 0;
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
