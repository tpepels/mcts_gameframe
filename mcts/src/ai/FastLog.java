package ai;

public class FastLog {
	// Stores a number of pre-computed logarithms
	private final static int N_LOGS = 200000;
	private final static double[] logs = new double[N_LOGS];

	static {
		for (int i = 0; i < logs.length; i++)
			logs[i] = Math.log(i);
	}

	public double log(double i) {
		if (i >= N_LOGS)
			return logs[N_LOGS - 1];
		return logs[(int) i];
	}
}
