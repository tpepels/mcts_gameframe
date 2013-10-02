package ai.framework;

public interface IMove {
	public int[] getMove();
	public int getType();
	public boolean equals(IMove move);
    public int getUniqueId();
}
