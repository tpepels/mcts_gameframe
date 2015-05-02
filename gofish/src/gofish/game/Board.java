package gofish.game;

import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.StatCounter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Board implements IBoard {
    private static final int[] SCORES = {3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2};
    private static final int Q_SIZE = 4;
    //
    public LinkedList<Card> p1Hand, p2Hand;
    public int p1Score, p2Score;
    private int[] p1CardCount, p2CardCount;
    public Deck deck;
    private int nMoves, currentPlayer;
    //
    private final MoveList expandMoves;
    private final ArrayList<IMove> playoutMoves;

    public Board() {
        // Moves are always the same
        expandMoves = new MoveList(Deck.MAX_CARD);
        playoutMoves = new ArrayList<>(Deck.MAX_CARD);
        p1CardCount = new int[Deck.MAX_CARD];
        p2CardCount = new int[Deck.MAX_CARD];
    }

    @Override
    public IBoard copy() {
        Board newBoard = new Board();
        newBoard.p1Hand = new LinkedList<>();
        for (Card c : p1Hand) {
            newBoard.p1Hand.add(c.copy());
        }
        newBoard.p2Hand = new LinkedList<>();
        for (Card c : p2Hand) {
            newBoard.p2Hand.add(c.copy());
        }
        System.arraycopy(p1CardCount, 0, newBoard.p1CardCount, 0, p1CardCount.length);
        System.arraycopy(p2CardCount, 0, newBoard.p2CardCount, 0, p2CardCount.length);
        newBoard.currentPlayer = currentPlayer;
        newBoard.p1Score = p1Score;
        newBoard.p2Score = p2Score;
        newBoard.deck = deck.copy();
        newBoard.nMoves = nMoves;
        return newBoard;
    }

    @Override
    public void initialize() {
        deck = new Deck(52);
        nMoves = 0;
        deck.initialize();
        p1Hand = new LinkedList<>();
        p2Hand = new LinkedList<>();
        // Deal first player's hand
        dealEmptyHand(p1Hand, p1CardCount, P1);
        // Deal second player's hand
        dealEmptyHand(p2Hand, p2CardCount, P2);
        //
        currentPlayer = P1;
    }

    private void dealEmptyHand(LinkedList<Card> hand, int[] cardCount, int player) {
        // Deal first player's hand
        deck.dealHand(hand, 7);
        for (Card card : hand) {
            cardCount[(card.card % 100) - 1]++;
        }
        checkBook(player);
    }

    @Override
    public void newDeterminization(int myPlayer, boolean postMove) {
        LinkedList<Card> hand = (myPlayer == P1) ? p2Hand : p1Hand;
        int nCards = hand.size();
        // Put the invisible player's hand back in the deck, and shuffle it
        deck.addHandToDek(hand);
        deck.shuffleDeck();
        // Deal a new hand to the invisible player
        deck.dealHand(hand, nCards);
        // Maintain the card counts for the player
        if (myPlayer == P1) {
            p2CardCount = new int[Deck.MAX_CARD];
            for (Card c : hand) {
                p2CardCount[(c.card % 100) - 1]++;
            }
        } else {
            p1CardCount = new int[Deck.MAX_CARD];
            for (Card c : hand) {
                p1CardCount[(c.card % 100) - 1]++;
            }
        }
    }


    @Override
    public MoveList getExpandMoves() {
        expandMoves.clear();
        LinkedList myHand = (currentPlayer == P1) ? p1Hand : p2Hand;
        for (int i = 1; i <= Deck.MAX_CARD; i++) {
            if (checkHand(myHand, i))
                expandMoves.add(new Move(i));
        }
        return expandMoves;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        playoutMoves.clear();
        LinkedList myHand = (currentPlayer == P1) ? p1Hand : p2Hand;
        for (int i = 1; i <= Deck.MAX_CARD; i++) {
            if (checkHand(myHand, i))
                playoutMoves.add(new Move(i));
        }
        return playoutMoves;
    }

    public boolean checkHand(LinkedList<Card> hand, int rank) {
        for (Card card : hand) {
            if (card.card % 100 == rank)
                return true;
        }
        return false;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        LinkedList<Card> opHand = (player == P1) ? p2Hand : p1Hand;
        LinkedList<Card> myHand = (player == P2) ? p2Hand : p1Hand;
        int[] myCardCount = (player == P1) ? p1CardCount : p2CardCount;
        int[] opCardCount = (player == P1) ? p2CardCount : p1CardCount;
        int opp = getOpponent(currentPlayer);
        Iterator<Card> it = opHand.iterator();
        Card card;
        boolean cardTaken = false;
        // Take all cards of the requested type from the user's hand
        while (it.hasNext()) {
            card = it.next();
            if (card.card % 100 == move.getMove()[0]) {
                myHand.add(card);
                card.visible = true;
                myCardCount[(card.card % 100) - 1]++;
                opCardCount[(card.card % 100) - 1]--;
                it.remove();
                cardTaken = true;
            }
        }
        // The opponent gave all his cards to the player
        if (cardTaken && opHand.isEmpty())
            dealEmptyHand(opHand, opCardCount, opp);
        //
        int[] book;
        if (!cardTaken && !deck.isEmpty()) {
            Card draw = deck.takeCard();
            myCardCount[(draw.card % 100) - 1]++;
            myHand.add(draw);
            // The player drew the card he asked for
            // therefore he gets another turn
            if (draw.card % 100 == move.getMove()[0])
                cardTaken = true;
        }
        book = checkBook(player);
        // Keep track of the book
        if (book != null) {
            // The book was the last card in my hand
            if (myHand.isEmpty())
                dealEmptyHand(myHand, myCardCount, currentPlayer);
        }
        if(!cardTaken) {
            // No card taken, opponent's turn
            currentPlayer = getOpponent(currentPlayer);
        }
        return true;
    }

    private int[] checkBook(int player) {
        int[] cardCount = (player == P1) ? p1CardCount : p2CardCount;
        LinkedList<Card> hand = (player == P1) ? p1Hand : p2Hand;
        int index = 0;
        Card card;
        int[] book = null;
        // Check for books!
        for (int i = 0; i < cardCount.length; i++) {
            if (cardCount[i] >= Q_SIZE) { // The player has 4 cards of this type!
                if (player == P1) p1Score += SCORES[i];
                else p2Score += SCORES[i];
                cardCount[i] -= Q_SIZE;
                book = new int[Q_SIZE];
                // Remove the Q_SIZE cards from the hand
                Iterator<Card> it = hand.iterator();
                while (it.hasNext() && index < 2) {
                    card = it.next();
                    if (card.card % 100 == (1 + i)) {
                        book[index] = card.card;
                        index++;
                        it.remove();
                    }
                }
                // Only a single book can be achieved per turn
                break;
            }
        }
        return book;
    }

    @Override
    public void undoMove() {
    }

    @Override
    public int getOpponent(int player) {
        return 3 - player;
    }

    @Override
    public int checkWin() {
        if (deck.isEmpty()) {
            if(p1Score == p2Score)
                return DRAW;
            return (p1Score > p2Score) ? P1_WIN : P2_WIN;
        }
        return NONE_WIN;
    }

    @Override
    public int checkPlayoutWin() {
        return checkWin();
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public int getMaxUniqueMoveId() {
        return 400 + Deck.MAX_CARD;
    }

    @Override
    public double evaluate(int player, int version) {
        if (player == P1)
            return p1Score - p2Score;
        else
            return p2Score - p1Score;
    }

    @Override
    public void initNodePriors(int parentPlayer, StatCounter stats, IMove move, int npvisits) {

    }

    @Override
    public boolean isPartialObservable() {
        return true;
    }

    @Override
    public int getNMovesMade() {
        return nMoves;
    }

    @Override
    public boolean isLegal(IMove move) {
        return checkHand((currentPlayer == P1) ? p1Hand : p2Hand, move.getMove()[0] % 100);
    }

    @Override
    public boolean noMovesIsDraw() {
        return false;
    }

    @Override
    public double getQuality() {
        if (checkWin() == P1_WIN) {
            return p1Score - p2Score / 105.;
        } else {
            return p2Score - p1Score / 105.;
        }
    }

    @Override
    public MoveList getOrderedMoves() {
        return null;
    }

    @Override
    public long hash() {
        return 0;
    }

    @Override
    public boolean poMoves() {
        return false;
    }

    @Override
    public int getNPlayers() {
        return 2;
    }
}
