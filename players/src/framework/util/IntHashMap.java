package framework.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tom on 04/04/15.
 */
public class IntHashMap<K> {
    private final static int TT_SIZE = (int) Math.pow(2, 20);
    private final static int MASK = TT_SIZE - 1;
    //
    private List<K>[] states;
    public int entries = 0;

    public IntHashMap() {
        this.states = new ArrayList[TT_SIZE];
    }

    public void clear() {
        this.states = new ArrayList[TT_SIZE];
        this.entries = 0;
    }

    public void put(int hash, K value) {
        int hashPos = getHashPos(hash);

        entries++;

        if (states[hashPos] == null) {
            states[hashPos] = new ArrayList<>();
        }

        if (!states[hashPos].contains(value))
            states[hashPos].add(value);
    }

    public boolean exists(int hash, K value) {
        int hashPos = getHashPos(hash);
        if (states[hashPos] != null)
            return states[hashPos].contains(value);
        else
            return false;
    }

    private int getHashPos(int hash) {
        return (hash & MASK);
    }
}
