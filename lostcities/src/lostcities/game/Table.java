package lostcities.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Table implements IBoard {
    public static int P2_EXP_I = 5, P2_HAND_I = 8, EXP_COST = 20, BONUS = 20, N_BONUS_CARDS = 8, MAX_DISC_STACK_DRAW = 2;
    private final MoveList moves = new MoveList(625);
    private final ArrayList<IMove> playoutMoves = new ArrayList<IMove>(100);
    private final java.util.Stack<Move> pastMoves = new java.util.Stack<Move>();
    //
    public Deck deck;                               // The main deck
    public Stack[] stacks = new Stack[5];           // Hold the coloured discard piles
    public int[] expeditionScores = new int[10];    // Hold the scores for the expeditionScores
    public int[] multipliers = new int[10];         // Holds the number of multipliers per expedition
    public int[] expeditionCards = new int[10];     // Holds the topmost card for each expedition
    public int[] numExpeditionCards = new int[10];  // Holds the number of cards per expedition (for the 20 bonus points)
    public int[] hands = new int[16];               // Holds the cards in player's hands
    public boolean[] known = new boolean[16];       // Whether all players know you hold this card
    public int[] scores = new int[2];               // Total score per player
    public int currentPlayer = P1, winner = NONE_WIN;
    private int card, value, colour, stack, stackI; // Some variables for move generation
    private int[] minCard = new int[5];
    private int[] discardStackDraws = {0, 0};       // Keep track of the discard - stack draw moves
    private int invisiblePlayer;

    @Override
    public void initialize() {
        deck = new Deck(60);
        deck.initialize();
        // Initially all scores are multiplied by 1
        Arrays.fill(multipliers, 1);
        // Reset the known cards in hand
        known = new boolean[16];
        // Deal first player's hand
        deck.dealHand(hands, 0, P2_HAND_I, known);
        // Deal second player's hand
        deck.dealHand(hands, P2_HAND_I, hands.length, known);
        // Initialize the stacks
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = new Stack();
        }
    }

    public void doMove(Move move) {
        int card = move.getMove()[0];
        pastMoves.push(move);
        int stack = (card / 100) - 1;
        int pStack = (currentPlayer == P1) ? stack : P2_EXP_I + stack;
        // Type of move
        if (move.getType() == Move.DISCARD) {
            // Discard the card to the given stack
            stacks[stack].addCard(card);
        } else {
            // Either investment, or the value of the card
            int cardType = card % 100;
            int pIndex = (currentPlayer == P1) ? 0 : 1;
            // Play a card on one of the expeditions
            if (numExpeditionCards[pStack] == 0) {
                // Starting a new expedition
                expeditionScores[pStack] -= EXP_COST;
                scores[pIndex] -= EXP_COST;
            }
            // Keep track of the number of cards per expedition
            numExpeditionCards[pStack]++;
            // We're adding an investment
            if (cardType == Deck.INVESTMENT) {
                move.setPrevTopCard(expeditionCards[pStack]);
                multipliers[pStack]++;
                // Investment cards multiply the cost of expeditions
                expeditionScores[pStack] -= EXP_COST;
                scores[pIndex] -= EXP_COST;
            } else {
                // For undoing the move
                move.setPrevTopCard(expeditionCards[pStack]);
                // Place the card on the expedition
                expeditionCards[pStack] = cardType;
                // Add the score, multiplied to the total score of the player
                scores[pIndex] += cardType * multipliers[pStack];
                expeditionScores[pStack] += cardType * multipliers[pStack];
                // Bonus points for more than 8 cards on any given expedition
                if (numExpeditionCards[pStack] >= N_BONUS_CARDS) {
                    scores[pIndex] += BONUS;
                    expeditionScores[pStack] += BONUS;
                }
            }
        }
        int draw = move.getMove()[1];
        int handIndex = getHandIndex(card, currentPlayer);
        // Set the hand index for undoMove
        move.setHandIndex(handIndex);
        // Draw a new card
        if (draw == Move.DECK_DRAW) {
            hands[handIndex] = deck.takeCard();
            known[handIndex] = false;
            discardStackDraws[currentPlayer - 1] = 0;
            // The game has ended, compare the players' scores
            if (deck.isEmpty()) {
                if (scores[0] > scores[1]) {
                    winner = P1_WIN;
                } else if (scores[1] > scores[0]) {
                    winner = P2_WIN;
                } else {
                    winner = DRAW;
                }
            }
        } else {
            hands[handIndex] = stacks[draw - 1].takeCard();
            known[handIndex] = true;
            //
            if (move.getType() == Move.DISCARD)
                discardStackDraws[currentPlayer - 1]++;
            else
                discardStackDraws[currentPlayer - 1] = 0;
        }
        currentPlayer = getOpponent(currentPlayer);
    }

    @Override
    public IBoard copy() {
        Table newTable = new Table();
        // Copy the deck
        newTable.deck = deck.copy();
        // Copy the stacks
        for (int i = 0; i < stacks.length; i++) {
            newTable.stacks[i] = stacks[i].copy();
        }
        // Copy the game state
        System.arraycopy(expeditionScores, 0, newTable.expeditionScores, 0, expeditionScores.length);
        System.arraycopy(multipliers, 0, newTable.multipliers, 0, multipliers.length);
        System.arraycopy(expeditionCards, 0, newTable.expeditionCards, 0, expeditionCards.length);
        System.arraycopy(hands, 0, newTable.hands, 0, hands.length);
        System.arraycopy(known, 0, newTable.known, 0, known.length);
        System.arraycopy(numExpeditionCards, 0, newTable.numExpeditionCards, 0, numExpeditionCards.length);
        newTable.scores[0] = scores[0];
        newTable.scores[1] = scores[1];
        newTable.discardStackDraws[0] = discardStackDraws[0];
        newTable.discardStackDraws[1] = discardStackDraws[1];
        newTable.currentPlayer = currentPlayer;
        newTable.winner = winner;
        return newTable;
    }

    public void generateAllMoves() {
        moves.clear();
        // Don't generate moves if there is already a winner
        if (winner != NONE_WIN)
            return;
        int startI = (currentPlayer == P1) ? 0 : P2_HAND_I;
        int endI = (currentPlayer == P1) ? P2_HAND_I : hands.length;
        stackI = (currentPlayer == P1) ? 0 : P2_EXP_I;
        // Generate the moves in the hand of the player
        for (int i = startI; i < endI; i++) {
            generateMove(hands[i]);
        }

        // Also generate moves that are in the deck, for the hidden player
        if (currentPlayer == invisiblePlayer) {
            for (int i = 0; i < deck.size(); i++) {
                generateMove(deck.get(i));
            }
        }
    }

    private void generateMove(int card) {
        boolean canPlay = false;
        value = card % 100;
        colour = (card / 100) - 1;
        stack = colour + stackI;
        // Investment card --> no expedition cards have been played
        if (value == Deck.INVESTMENT && expeditionCards[stack] == 0) {
            canPlay = true;
        } else if (value != Deck.INVESTMENT && expeditionCards[stack] < value) {
            // Expedition card, can be played in the expedition
            canPlay = true;
        }
        // Move for drawing from deck & moves for drawing from coloured stacks (0 < j < 5)
        for (int j = 0; j < stacks.length + 1; j++) {
            if (j == 0 || (j > 0 && !stacks[j - 1].isEmpty())) {
                // Discard move
                if (j == 0 || (j > 0 && discardStackDraws[currentPlayer - 1] < MAX_DISC_STACK_DRAW))
                    moves.add(new Move(card, j, true));
                // Play card move
                if (canPlay)
                    moves.add(new Move(card, j, false));
            }
        }
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        doMove((Move) move);
        return true;
    }

    @Override
    public MoveList getExpandMoves() {
        generateAllMoves();
        return moves;
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        playoutMoves.clear();
        // Don't generate moves if there is already a winner
        if (winner != NONE_WIN)
            return Arrays.asList();
        Arrays.fill(minCard, 20);
        int startI = (currentPlayer == P1) ? 0 : P2_HAND_I;
        int endI = (currentPlayer == P1) ? P2_HAND_I : hands.length;
        stackI = (currentPlayer == P1) ? 0 : P2_EXP_I;
        boolean canPlay, hasMove = false;
        IMove m;
        for (int i = startI; i < endI; i++) {
            canPlay = false;
            card = hands[i];
            value = card % 100;
            colour = (card / 100) - 1;
            stack = colour + stackI;
            // Investment card --> no expedition cards have been played
            if (value == Deck.INVESTMENT && expeditionCards[stack] == 0) {
                canPlay = true;
            } else if (value != Deck.INVESTMENT && expeditionCards[stack] < value) {
                // Expedition card, can be played in the expedition
                canPlay = true;
                hasMove = true;
                // Remember the lowest card per colour
                if (minCard[colour] > value) {
                    minCard[colour] = value;
                }
            }
            // Move for drawing from deck &
            // Moves for drawing from coloured stacks (0 < j < 5)
            for (int j = 0; j < stacks.length + 1; j++) {
                if (j == 0 || (j > 0 && !stacks[j - 1].isEmpty())) {
                    // Discard move
                    if (j == 0 || (j > 0 && discardStackDraws[currentPlayer - 1] < MAX_DISC_STACK_DRAW)) {
                        m = new Move(card, j, true);
                        ((Move)m).setHandIndex(i);
                        playoutMoves.add(m);
                    }
                    // Play card move
                    if (canPlay) {
                        m = new Move(card, j, false);
                        ((Move)m).setHandIndex(i);
                        playoutMoves.add(m);
                    }
                }
            }
        }
        // We now have all legal moves, but it doesn't make sense to play high cards when low ones are in hand
        // At the end of the game though, this may not apply (dump high cards quick), use e-greedy with some p here.
        if (hasMove && heuristics && deck.size() >= 5) {
            Iterator<IMove> i = playoutMoves.iterator();
            // Make sure no cards higher than the lowest available card are played
            while (i.hasNext()) {
                m = i.next();
                card = m.getMove()[0];
                value = card % 100;
                if (value == Deck.INVESTMENT)
                    continue;
                // Only playing value-cards is considered
                if (m.getType() == Move.DISCARD) {
                    i.remove();
                    continue;
                }
                colour = (card / 100) - 1;
                if (minCard[colour] < value) {
                    i.remove();
                }
            }
        }
        return playoutMoves;
    }

    @Override
    public void undoMove() {
        Move move = pastMoves.pop();
        if (move == null)
            return;
        //
        currentPlayer = getOpponent(currentPlayer);
        // Undo the move
        int card = move.getMove()[0];
        int draw = move.getMove()[1];
        int handIndex = move.getHandIndex();
        // Return the drawn card
        if (draw == Move.DECK_DRAW) {
            deck.returnCard(hands[handIndex]);
            winner = NONE_WIN;
        } else {
            stacks[draw - 1].addCard(hands[handIndex]);
            if (move.getType() == Move.DISCARD)
                discardStackDraws[currentPlayer - 1]--;
        }
        // Return the played card the the player's hand
        int stack = (card / 100) - 1;
        // Type of move
        if (move.getType() == Move.DISCARD) {
            // Return the discarded card to the player's hand
            hands[handIndex] = stacks[stack].takeCard();
        } else {
            hands[handIndex] = card;
            // Either investment, or the value of the card
            int cardType = card % 100;
            int pIndex = (currentPlayer == P1) ? 0 : 1;
            int pStack = (currentPlayer == P1) ? stack : P2_EXP_I + stack;
            // We're taking back an investment
            if (cardType == Deck.INVESTMENT) {
                multipliers[pStack]--;
                // Investment
                // Investment cards multiply the cost of expeditions
                expeditionScores[pStack] += EXP_COST;
                scores[pIndex] += EXP_COST;
            } else {
                expeditionCards[pStack] = move.getPrevTopCard();
                // Subtract the score, multiplied, from the total score of the player
                scores[pIndex] -= cardType * multipliers[pStack];
                expeditionScores[pStack] -= cardType * multipliers[pStack];
                // Bonus points for more than 8 cards on any given expedition
                if (numExpeditionCards[pStack] == N_BONUS_CARDS) { // We are taking the 8th card
                    scores[pIndex] -= BONUS;
                    expeditionCards[pIndex] -= BONUS;
                }
            }
            // Keep track of the number of cards per expedition
            numExpeditionCards[pStack]--;
            // Reset the expedition if it's empty
            if (numExpeditionCards[pStack] == 0) {
                // Resetting to a new expedition
                expeditionScores[pStack] = 0;
                scores[pIndex] += EXP_COST;
            }
        }
    }

    /**
     * Finds the index of a specified card in the player's hand.
     *
     * @param card   The card to search
     * @param player The player's to search in
     * @return -1 if the card is not in the player's hand, the hand index otherwise
     */
    private int getHandIndex(int card, int player) {
        int handIndex = (player == P1) ? 0 : P2_HAND_I;
        int endIndex = (player == P1) ? P2_HAND_I : hands.length;
        // Find the card in the player's hand
        while (handIndex < endIndex && hands[handIndex] != card) {
            handIndex++;
        }
        if (handIndex < endIndex)
            return handIndex;
        else
            return -1;
    }

    @Override
    public int getOpponent(int player) {
        return (player == P1) ? P2 : P1;
    }

    @Override
    public int checkWin() {
        return winner;
    }

    @Override
    public int checkPlayoutWin() {
        return winner;
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public int getMaxUniqueMoveId() {
        return 5511;
    }

    @Override
    public void newDeterminization(int myPlayer) {
        int si = (myPlayer == P1) ? P2_HAND_I : 0, ei = (myPlayer == P1) ? hands.length : P2_HAND_I;
        invisiblePlayer = (myPlayer == P1) ? P2 : P1;
        // Put the invisible player's hand back in the deck, and shuffle it
        deck.addHandToDek(hands, si, ei, known);
        deck.shuffleDeck();
        // Deal a new hand to the invisible player
        deck.dealHand(hands, si, ei, known);
    }

    @Override
    public boolean isPartialObservable() {
        return true;
    }

    @Override
    public boolean isLegal(IMove move) {
        boolean canDraw;
        // Check if the draw is possible
        if (move.getMove()[1] != Move.DECK_DRAW) {
            canDraw = !stacks[move.getMove()[1] - 1].isEmpty();
            // Don't allow too many subsequent discard-stack draw moves
            if (canDraw && discardStackDraws[currentPlayer - 1] >= MAX_DISC_STACK_DRAW)
                canDraw = false;
        } else
            canDraw = !deck.isEmpty();
        //
        if (!canDraw)
            return false;
        // Check if the card is in the player's hand
        if (getHandIndex(move.getMove()[0], currentPlayer) == -1)
            return false;
        // Cards can always be discarded
        if (move.getType() == Move.DISCARD)
            return true;
        // Check if we can play the card on an expedition
        int card = move.getMove()[0];
        int colour = (card / 100) - 1;
        int stackI = (currentPlayer == P1) ? 0 : P2_EXP_I;
        int type = card % 100;
        // Investment card --> no expedition cards have been played
        // Expedition card --> the card is higher than the top card
        return type == Deck.INVESTMENT && expeditionCards[colour + stackI] == 0
                || (type != Deck.INVESTMENT && expeditionCards[colour + stackI] < type);
    }

    @Override
    public boolean drawPossible() {
        // For this game, this method does not apply, since there will always be moves available
        return false;
    }
}
