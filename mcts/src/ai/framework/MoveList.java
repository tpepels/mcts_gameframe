package ai.framework;

public class MoveList {

    private IMove[] moves;
    private int size;

    public MoveList(int maxSize) {
        moves = new IMove[maxSize];
        size = 0;
    }

    public void add(IMove move) {
        moves[size++] = move;
    }

    public void clear() {
        size = 0;
    }

    public IMove get(int index) {
        return moves[index];
    }

    public IMove[] getArrayCopy() {
        IMove[] copy = new IMove[size];
        System.arraycopy(moves, 0, copy, 0, size);
        return copy;
    }

    public int size() {
        return size;
    }
}
