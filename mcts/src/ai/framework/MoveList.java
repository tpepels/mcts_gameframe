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

    public MoveList addAll(MoveList list) {
        for (int i = 0; i < list.size(); i++) {
            add(list.get(i));
        }
        return this;
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

    public MoveList copy() {
        MoveList newList = new MoveList(size + 1);
        System.arraycopy(moves, 0, newList.moves, 0, size);
        newList.size = size;
        return newList;
    }

    public int size() {
        return size;
    }
}
