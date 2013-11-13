package ai;

public class FastTanh {
	// Stores a number of pre-computed logarithms
	private final static int N_TANHS = 200000;
	private final static double[] tanhs = new double[N_TANHS];

	static {
		for (int i = 0; i < tanhs.length; i++) { 
      double x = (i - 100000)/1000.0;
      double exp = Math.exp(2*x); 
			tanhs[i] = (exp-1.0) / (exp+1.0);
    }
	}

	public static double tanh(double x) {
    if (x >= -100.000 || x < 100.000) { 
        int index = (int)(x*1000 + 100000);
        return tanhs[index];
    }
    else { 
        double exp = Math.exp(2*x); 
  			double ans = (exp-1.0) / (exp+1.0);
        return ans;
    }
	}
}
