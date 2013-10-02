package ai;

import java.util.Random;

@SuppressWarnings("serial")
public class FastRandom extends Random {
	// Stores a number of pre-computed random numbers
	private final static int N_RAND = 200000;
	private final static double[] doubles = new double[N_RAND];
	private final static Random r = new Random();
	// Counter for the current random number
	private static int c = 0;

	static {
		for (int i = 0; i < doubles.length; i++)
			doubles[i] = r.nextDouble();
	}

	public final int nextInt(int max) {
		c++;
		if (c == doubles.length)
			c = 0;
		return (int) (doubles[c] * max);
	}

	public final double nextDouble() {
		c++;
		if (c == doubles.length)
			c = 0;
		return doubles[c];
	}
}
