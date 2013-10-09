package lostcities.game;

import ai.framework.IMove;

public class Move implements IMove {
    // 0 Is draw from the deck, 1 - 5 corresponds to the coloured stacks
    public static final int DECK_DRAW = 0, PLAY = 1, DISCARD = -1;
    final int[] move = new int[2];
    final int type;
    private int prevTopCard, cardPlayed;

    public Move(int handIndex, int draw, boolean discard) {
        move[0] = handIndex;
        move[1] = draw;
        type = (discard) ? DISCARD : PLAY;
    }

    @Override
    public int[] getMove() {
        return move;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public boolean equals(IMove move) {
        return move.getMove()[0] == this.move[0] && move.getMove()[1] == this.move[1];
    }

    @Override
    public int getUniqueId() {
        return move[0] + 1000 * move[1];
    }

    public int getCardPlayed() {
        return cardPlayed;
    }

    public void setCardPlayed(int cardPlayed) {
        this.cardPlayed = cardPlayed;
    }

    public int getPrevTopCard() {
        return prevTopCard;
    }

    public void setPrevTopCard(int prevTopCard) {
        this.prevTopCard = prevTopCard;
    }
}
