package lostcities.game;

import java.util.Random;

public class Deck {
    private static final Random rnd = new Random();
    public static int MAX_CARD = 10, INVESTMENT = 11, N_INVEST_C = 3;
    private static int[] COLORS = {100, 200, 300, 400, 500};
    //
    private final int[] deck;
    private int index, initSize;

    public Deck(int size) {
        deck = new int[size];
        this.initSize = size;
        this.index = size - 1;
    }

    public boolean isEmpty() {
        return index < 0;
    }

    public int get(int i) {
        if (i < size())
            return deck[index - i];
        else
            throw new IndexOutOfBoundsException(i + " is out of deck bounds.");
    }

    /**
     * Take the top card from the deck, and remove it from the deck
     *
     * @return The top card from the deck
     */
    public int takeCard() {
        return deck[index--];
    }

    public void returnCard(int card) {
        deck[++index] = card;
    }

    public int size() {
        return index + 1;
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
            returnCard(hand[i]);
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
        Deck newDeck = new Deck(initSize);
        System.arraycopy(deck, 0, newDeck.deck, 0, size());
        newDeck.index = index;
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
            for (int j = 0; j < N_INVEST_C; j++) {
                deck[index] = COLOR + INVESTMENT;
                index++;
            }
        }
        // Shuffle!
        shuffleDeck();
        shuffleDeck();
    }

    // Fisher–Yates shuffle
    public void shuffleDeck() {
        int index, a;
        for (int i = size() - 1; i > 0; i--) {
            index = rnd.nextInt(i + 1);
            // Simple swap
            a = deck[index];
            deck[index] = deck[i];
            deck[i] = a;
        }
    }
}
