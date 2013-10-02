package chinesecheckers.game;

public class Field {
    public int occupant = Board.EMPTY;
    public int position;
    public Field[] neighbours = new Field[6];

    public Field(int position) {
        this.position = position;
    }
}
