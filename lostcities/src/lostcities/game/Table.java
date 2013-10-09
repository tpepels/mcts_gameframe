package lostcities.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.Arrays;
import java.util.List;

public class Table implements IBoard {
    public static final MoveList moves = new MoveList(500);
    public static int P2_EXP_I = 5, P2_HAND_I = 8, EXP_COST = 20, BONUS = 20, N_BONUS_CARDS = 8;
    //
    public Deck deck = new Deck(60);                // The main deck
    public Stack[] stacks = new Stack[5];           // Hold the coloured discard piles
    public int[] expeditionScores = new int[10];    // Hold the scores for the expeditionScores
    public int[] multipliers = new int[10];         // Holds the number of multipliers per expedition
    public int[] expeditionCards = new int[10];     // Holds the topmost card for each expedition
    public int[] numExpeditionCards = new int[10];  // Holds the number of cards per expedition (for the 20 bonus points)
    public int[] hands = new int[16];               // Holds the cards in player's hands
    public int[] scores = new int[2];              // Total score per player
    public int currentPlayer = P1, winner = NONE_WIN;
    private java.util.Stack<Move> pastMoves = new java.util.Stack<Move>();

    public void initialize() {
        deck.initialize();
        // Initially all scores are multiplied by 1
        Arrays.fill(multipliers, 1);
        // Deal first player's hand
        deck.dealHand(hands, 0, P2_HAND_I);
        // Deal second player's hand
        deck.dealHand(hands, P2_HAND_I, hands.length);
        // Initialize the stacks
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = new Stack();
        }
    }

    public void doMove(Move move) {
        int handIndex = move.getMove()[0];
        int card = hands[handIndex];
        move.setCardPlayed(card);
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
        // Draw a new card
        if (draw == Move.DECK_DRAW) {
            hands[handIndex] = deck.takeCard();
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
        System.arraycopy(numExpeditionCards, 0, newTable.numExpeditionCards, 0, numExpeditionCards.length);
        newTable.scores[0] = scores[0];
        newTable.scores[1] = scores[1];
        return newTable;
    }

    public void generateAllMoves() {
        moves.clear();
        int startI = (currentPlayer == P1) ? 0 : P2_HAND_I;
        int endI = (currentPlayer == P1) ? P2_HAND_I + 1 : hands.length;
        for (int i = startI; i < endI; i++) {
            // Move for drawing from deck &
            // Moves for drawing from coloured stacks (0 < j < 5)
            for (int j = 0; j < stacks.length + 1; j++) {
                // Discard move
                moves.add(new Move(i, j, true));
                // Play card move
                moves.add(new Move(i, j, false));
            }
        }
    }

    public void generateLegalMoves() {
        moves.clear();
        int startI = (currentPlayer == P1) ? 0 : P2_HAND_I;
        int endI = (currentPlayer == P1) ? P2_HAND_I + 1 : hands.length;
        int stackI = (currentPlayer == P1) ? 0 : P2_EXP_I;
        int card, color, stack, type;
        boolean canPlay;
        for (int i = startI; i < endI; i++) {
            canPlay = false;
            card = hands[i];
            type = card % 100;
            color = (card / 100) - 1;
            stack = color + stackI;
            // Investment card --> no expedition cards have been played
            if (type == Deck.INVESTMENT && expeditionCards[stack] == 0) {
                canPlay = true;
            } else if (expeditionCards[stack] < type) {
                // Expedition card, can be played in the expedition
                canPlay = true;
            }
            // Move for drawing from deck &
            // Moves for drawing from coloured stacks (0 < j < 5)
            for (int j = 0; j < stacks.length + 1; j++) {
                if (j == 0 || !stacks[j - 1].isEmpty()) {
                    // Discard move
                    moves.add(new Move(i, j, true));
                    // Play card move
                    if (canPlay)
                        moves.add(new Move(i, j, false));
                }
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
    public List<IMove> getPlayoutMoves() {
        generateLegalMoves();
        return Arrays.asList(moves.getArrayCopy());
    }

    @Override
    public void undoMove() {
        Move move = pastMoves.pop();
        if (move == null)
            return;
        //
        currentPlayer = getOpponent(currentPlayer);
        // Undo the move
        int card = move.getCardPlayed(), handIndex = move.getMove()[0];
        int draw = move.getMove()[1];
        // Return the drawn card
        if (draw == Move.DECK_DRAW) {
            deck.returnCard(hands[handIndex]);
            winner = NONE_WIN;
        } else {
            stacks[draw - 1].addCard(hands[handIndex]);
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
        // Put the invisible player's hand back in the deck, and shuffle it
        deck.addHandToDek(hands, si, ei);
        deck.shuffleDeck();
        // Deal a new hand to the invisible player
        deck.dealHand(hands, si, ei);
    }

    @Override
    public boolean isPartialObservable() {
        return true;
    }

    @Override
    public boolean isLegal(IMove move) {
        boolean canDraw = true;
        // Check if the draw is possible
        if (move.getMove()[1] != Move.DECK_DRAW)
            canDraw = !stacks[move.getMove()[1] - 1].isEmpty();
        if (!canDraw)
            return false;

        // Cards can always be discarded
        if (move.getType() == Move.DISCARD)
            return true;

        // Check if we can play the card on an expedition
        int card = hands[move.getMove()[0]];
        int color = (card / 100) - 1;
        int stackI = (currentPlayer == P1) ? 0 : P2_EXP_I;
        // Investment card --> no expedition cards have been played
        if (card % 100 == Deck.INVESTMENT && expeditionCards[color + stackI] == 0) {
            return true;
        } else if (expeditionCards[color + stackI] < card % 100) {
            // Expedition card, can be played in the expedition
            return true;
        }
        return false;
    }

    @Override
    public boolean drawPossible() {
        // For this game, this method does not apply, since there will always be moves available
        return false;
    }
}
