package test;

public class test {
    //
    public static void main(String[] args) {
        for (double i = -1.; i <= 1.; i += 0.1) {
            System.out.println(0.5 * (i + 1.) + " " + (getEntropy(0.5 * (i + 1.)) - 0.5));
        }
    }

    private static double getEntropy(double value) {
        return -value * log(value, 2) - (1. - value) * log(1. - value, 2);
    }

    private static double log(double x, int base) {
        if (x == 0)
            return 0;
        return Math.log(x) / Math.log(base);
    }
}
