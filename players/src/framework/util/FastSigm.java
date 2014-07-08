package framework.util;

/**
 * Created by tom on 25/12/13.
 */
public class FastSigm {
    // Stores a number of pre-computed sigmoids
    private final static int N_SIGM = 20001;
    private final static double[] sigms = new double[N_SIGM];

    static {
        for (int i = 0; i < sigms.length; i++) {
            double x = (i - 10000) / 1000.0;
            sigms[i] = -1  + (2./ (1+ Math.exp(x)));
        }
    }

    public static double sigm(double x) {

        if (x >= -10.0 && x <= 10.0) {
            int index = (int) Math.round(x * 1000 + 10000);
            return sigms[index];
        } else if (x < -10) {
            return -1.;
        } else if (x > 10) {
            return 1.;
        }

        throw new RuntimeException("Something wrong in fastsigm x=" + x);
    }

//    public static void main(String[] args) {
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < 100; i++) {
//            double x = .5 - Math.random();
//            double k = -.25 + (.5 / (1 + Math.exp(-x)));
//            double l = FastSigm.sigm(-x);
//            System.out.println(x + " diff "  + Math.abs(k - l));
//        }
//        System.out.println("normal sigm took : " + (System.currentTimeMillis() - start));
//        FastSigm.sigm(10);
//        start = System.currentTimeMillis();
//        for (int i = 0; i < 1000000; i++) {
//            double k = FastSigm.sigm(.5 - Math.random());
//        }
//        System.out.println("fast sigm took : " + (System.currentTimeMillis() - start));
//    }
}
