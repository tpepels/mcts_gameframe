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

    public LinkedList<Integer> p1Hand, p2Hand;
    public int p1NPiles, p2NPiles;
    private int[] p1CardCount, p2CardCount;
    private Deck deck;
    private int nMoves, currentPlayer;
    //
    private final MoveList expandMoves;
    private final ArrayList<IMove> playoutMoves;
    private boolean[] piles;

    public Board() {
        // Moves are always the same
        expandMoves = new MoveList(Deck.MAX_CARD);
        playoutMoves = new ArrayList<>(Deck.MAX_CARD);
        p1CardCount = new int[Deck.MAX_CARD];
        p2CardCount = new int[Deck.MAX_CARD];
        piles = new boolean[Deck.MAX_CARD];
    }

    @Override
    public IBoard copy() {
        Board newBoard = new Board();
        newBoard.p1Hand = (LinkedList<Integer>) p1Hand.clone();
        newBoard.p2Hand = (LinkedList<Integer>) p2Hand.clone();
        System.arraycopy(p1CardCount, 0, newBoard.p1CardCount, 0, p1CardCount.length);
        System.arraycopy(p2CardCount, 0, newBoard.p2CardCount, 0, p2CardCount.length);
        System.arraycopy(piles, 0, newBoard.piles, 0, piles.length);
        newBoard.currentPlayer = currentPlayer;
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

    private void dealEmptyHand(LinkedList<Integer> hand, int[] cardCount, int player) {
        // Deal first player's hand
        deck.dealHand(hand, 7);
        for (Integer card : hand) {
            cardCount[(card % 100) - 1]++;
        }
        checkQuartet(player);
    }

    @Override
    public void newDeterminization(int myPlayer) {
        LinkedList<Integer> hand = (myPlayer == P1) ? p2Hand : p1Hand;
        int nCards = hand.size();
        // Put the invisible player's hand back in the deck, and shuffle it
        deck.addHandToDek(hand);
        deck.shuffleDeck();
        // Deal a new hand to the invisible player
        deck.dealHand(hand, nCards);
        // Maintain the card counts for the player
        int[] cardCount;
        if (myPlayer == P1) {
            p2CardCount = new int[Deck.MAX_CARD];
            cardCount = p2CardCount;
        } else {
            p1CardCount = new int[Deck.MAX_CARD];
            cardCount = p1CardCount;
        }
        //
        for (Integer c : hand) {
            cardCount[(c % 100) - 1]++;
            // We know that the player has no quartet!
            // Therefore, this determinization is not possible
            if (cardCount[(c % 100) - 1] == 4) {
                newDeterminization(myPlayer);
                return;
            }
        }
    }


    @Override
    public MoveList getExpandMoves() {
        expandMoves.clear();
        for (int i = 1; i <= piles.length; i++) {
            if (!piles[i - 1])
                expandMoves.add(new Move(i));
        }
        return expandMoves;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        if (!heuristics) {
            playoutMoves.clear();
            for (int i = 1; i <= piles.length; i++) {
                if (!piles[i - 1])
                    playoutMoves.add(new Move(i));
            }
            return playoutMoves;
        } else {
            playoutMoves.clear();
            LinkedList myHand = (currentPlayer == P1) ? p1Hand : p2Hand;
            for (int i = 1; i <= piles.length; i++) {
                if (!piles[i - 1] && myHand.contains(Integer.valueOf(i)))
                    playoutMoves.add(new Move(i));
            }
            if (playoutMoves.isEmpty())
                return getPlayoutMoves(false);
            return playoutMoves;
        }
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        LinkedList<Integer> opHand = (player == P1) ? p2Hand : p1Hand;
        LinkedList<Integer> myHand = (player == P2) ? p2Hand : p1Hand;
        int[] myCardCount = (player == P1) ? p1CardCount : p2CardCount;
        int[] opCardCount = (player == P1) ? p2CardCount : p1CardCount;
        int opp = getOpponent(currentPlayer);
        Iterator<Integer> it = opHand.iterator();
        int card, index = 0;
        boolean cardTaken = false;
        int[] sw1 = new int[4];
        // Take all cards of the requested type from the user's hand
        while (it.hasNext()) {
            card = it.next();
            if (card % 100 == move.getMove()[0]) {
                myHand.add(card);
                myCardCount[(card % 100) - 1]++;
                opCardCount[(card % 100) - 1]--;
                it.remove();
                cardTaken = true;
                sw1[index] = card;
                index++;
            }
        }
        // The opponent gave all his cards to the player
        if (cardTaken && opHand.isEmpty())
            dealEmptyHand(opHand, opCardCount, opp);
        //
        int[] quartet;
        if (!cardTaken && !deck.isEmpty()) {
            Integer draw = deck.takeCard();
            myCardCount[(draw % 100) - 1]++;
            myHand.add(draw);
        }
        quartet = checkQuartet(player);
        // Keep track of the quartet
        if (quartet != null) {
            piles[(quartet[0] % 100) - 1] = true;
            // The quartet was the last card in my hand
            if (myHand.isEmpty())
                dealEmptyHand(myHand, myCardCount, currentPlayer);
        }
        // Push the quartet (null if none found)
        if (index > 0) {
            int[] sw2 = new int[index];
            System.arraycopy(sw1, 0, sw2, 0, index);
        }
        currentPlayer = getOpponent(currentPlayer);
        return true;
    }

    private int[] checkQuartet(int player) {
        int[] cardCount = (player == P1) ? p1CardCount : p2CardCount;
        LinkedList<Integer> hand = (player == P1) ? p1Hand : p2Hand;
        int card, index = 0;
        int[] quartet = null;
        // Check for quartets!
        for (int i = 0; i < cardCount.length; i++) {
            if (cardCount[i] == 4) { // The player has 4 cards of this type!
                if (player == P1) p1NPiles++;
                else p2NPiles++;
                cardCount[i] -= 4;
                quartet = new int[4];
                // Remove the 4 cards from the hand
                Iterator<Integer> it = hand.iterator();
                while (it.hasNext()) {
                    card = it.next();
                    if (card % 100 == (1 + i)) {
                        quartet[index] = card;
                        index++;
                        it.remove();
                    }
                }
                // Only a single quartet can be achieved per turn
                break;
            }
        }
        return quartet;
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
            return (p1NPiles > p2NPiles) ? P1_WIN : P2_WIN;
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
            return p1NPiles - p2NPiles;
        else
            return p2NPiles - p1NPiles;
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
        return true;
    }

    @Override
    public boolean noMovesIsDraw() {
        return false;
    }

    @Override
    public double getQuality() {
        return 0;
    }

    @Override
    public MoveList getOrderedMoves() {
        return null;
    }

    @Override
    public long hash() {
        return 0;
    }
}
