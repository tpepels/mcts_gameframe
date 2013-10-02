package pentalath.ai;

public class ABMove {
    public final int position;
    public int history;

    //
    public ABMove(int position) {
        this.position = position;
        history = 0;
    }
}
