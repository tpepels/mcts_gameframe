package ai;

/**
 * Created by tom on 25/12/13.
 */
public class FastExp {
    // Stores a number of pre-computed logarithms
    private final static int N_EXPS = 20001;
    private final static double[] exps = new double[N_EXPS];

    static {
        for (int i = 0; i < exps.length; i++) {
            double x = (i - 10000) / 1000.0;
            exps[i] = Math.exp(x);
            if(i  == 0) {
                System.out.println(x);
            }
            if(i + 1 == exps.length) {
                System.out.println(x);
            }
        }
    }

    public static double exp(double x) {
        if (x >= -10.0 && x < 10.0) {
            int index = (int) Math.round(x * 1000 + 10000);
            return exps[index];
        } else {
            return Math.exp(x);
        }
    }
}
