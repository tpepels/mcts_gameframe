package ai;

public class FastSqrt {
	// Stores a number of pre-computed logarithms
	private final static int N_ROOTS = 1000;
	private final static double[] roots = new double[N_ROOTS];

	static {
		for (int i = 0; i < roots.length; i++)
            roots[i] = Math.sqrt(i);
	}

	public double sqrt(double i) {
		if (i >= N_ROOTS)
			return Math.sqrt(i);
		return roots[(int) i];
	}
}
