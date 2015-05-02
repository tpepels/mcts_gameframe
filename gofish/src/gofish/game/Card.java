package gofish.game;

/**
 * Created by tom on 02/05/15.
 */
public class Card {
    public int card;
    public boolean visible = false;

    public Card(int card) {
        this.card = card;
    }

    public Card copy() {
        Card c = new Card(card);
        c.visible = visible;
        return c;
    }
}
