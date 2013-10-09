package lostcities.game;

import java.util.Random;

public class Deck {
    private static final Random rnd = new Random();
    public static int MAX_CARD = 10, INVESTMENT = 11;
    private static int[] COLORS = {100, 200, 300, 400, 500};
    //
    private final int[] deck;
    private int size;

    public Deck(int size) {
        deck = new int[size];
        this.size = size - 1;
    }

    public boolean isEmpty() {
        return size < 0;
    }

    /**
     * Take the top card from the deck, and remove it from the deck
     *
     * @return The top card from the deck
     */
    public int takeCard() {
        return deck[size--];
    }

    public void returnCard(int card) {
        deck[++size] = card;
    }

    public int size() {
        return size + 1;
    }

    /**
     * Place a player's hand back in the deck, when searching, do this for the non-visible hand.
     * You probably want to call shuffleDeck() after this method is finished ;)
     *
     * @param hand       The hand(s) to place back
     * @param startIndex Start index of the hand in the hand array
     * @param endIndex   Final index of the hand in the hand array (i < endIndex)
     */
    public void addHandToDek(int[] hand, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            deck[++size] = hand[i];
        }
    }

    /**
     * Deal a full hand to a player.
     *
     * @param hand       The hand(s) to place back
     * @param startIndex Start index of the hand in the hand array
     * @param endIndex   Final index of the hand in the hand array (i < endIndex)
     */
    public void dealHand(int[] hand, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
           hand[i] = takeCard();
        }
    }

    public Deck copy() {
        Deck newDeck = new Deck(size);
        System.arraycopy(deck, 0, newDeck.deck, 0, size);
        return newDeck;
    }

    public void initialize() {
        int index = 0;
        for (int COLOR : COLORS) {
            for (int j = 2; j <= MAX_CARD; j++) {
                deck[index] = COLOR + j;
                index++;
            }
            /* Each color has three investment cards */
            deck[index] = COLOR + INVESTMENT;
            index++;
            deck[index] = COLOR + INVESTMENT;
            index++;
            deck[index] = COLOR + INVESTMENT;
            index++;
        }
        // Shuffle!
        shuffleDeck();
    }

    // Fisher–Yates shuffle
    public void shuffleDeck() {
        int index, a;
        for (int i = size - 1; i > 0; i--) {
            index = rnd.nextInt(i + 1);
            // Simple swap
            a = deck[index];
            deck[index] = deck[i];
            deck[i] = a;
        }
    }
}
