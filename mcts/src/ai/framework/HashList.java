package ai.framework;

/**
 * Created by Tom on 4-3-14.
 */
public class HashList {

    private long[] moves;
    private int size;

    public HashList(int maxSize) {
        moves = new long[maxSize];
        size = 0;
    }

    public void add(long move) {
        moves[size++] = move;
    }

    public HashList addAll(HashList list) {
        for (int i = 0; i < list.size(); i++) {
            add(list.get(i));
        }
        return this;
    }

    public void clear() {
        size = 0;
    }

    public long get(int index) {
        return moves[index];
    }

    public long[] getArrayCopy() {
        long[] copy = new long[size];
        System.arraycopy(moves, 0, copy, 0, size);
        return copy;
    }

    public HashList copy() {
        HashList newList = new HashList(size + 1);
        System.arraycopy(moves, 0, newList.moves, 0, size);
        newList.size = size;
        return newList;
    }

    public int size() {
        return size;
    }
}
