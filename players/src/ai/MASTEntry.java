package ai;

public class MASTEntry {
    public int id;
    public double value;

    public MASTEntry(int id, double value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public String toString() {
        return "id " + id + " val " + value;
    }
}
