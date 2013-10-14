package chinesecheckers.game;

public class Piece {
    public final int id, colour;
    public int location;

    public Piece(int id, int location, int colour) {
        this.id = id;
        this.location = location;
        this.colour = colour;
    }

    public Piece copy() {
       return new Piece(id, location, colour);
    }
}
