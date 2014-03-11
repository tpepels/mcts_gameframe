package lostcities.game;

import ai.framework.IMove;

public class Move extends IMove {
    // 0 Is draw from the deck, 1 - 5 corresponds to the coloured stacks
    public static final int DECK_DRAW = 0, PLAY = 1, DISCARD = -1;
    final int[] move = new int[2];
    final int type;
    private int prevTopCard = -1, handIndex = -1;

    public Move(int card, int draw, boolean discard) {
        move[0] = card;
        move[1] = draw;
        type = (discard) ? DISCARD : PLAY;
    }

    private String getColour(int colour) {
        switch (colour) {
            case (1):
                return "Y";
            case (2):
                return "B";
            case (3):
                return "W";
            case (4):
                return "G";
            case (5):
                return "R";
        }
        return "ERROR";
    }

   public int[] getMove() {
        return move;
    }


    public int getType() {
        return type;
    }


    public boolean equals(IMove move) {
        return move.getMove()[0] == this.move[0] && move.getMove()[1] == this.move[1] && move.getType() == this.type;
    }


    public int getUniqueId() {
        int pd = (type == PLAY) ? 0 : 1;
        return move[0] + (1000 * move[1]) + (10000 * pd);
    }


    public boolean isChance() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public String toString() {
        String cardStr, playStr, drawStr;
        int type = move[0] % 100;
        int colour = move[0] / 100;
        cardStr = (type == Deck.INVESTMENT) ? "$" : Integer.toString(type);
        cardStr = getColour(colour) + cardStr;
        if (this.type == DISCARD)
            playStr = "Discard ";
        else
            playStr = "Play ";

        if (move[1] == 0)
            drawStr = "deck";
        else
            drawStr = getColour(move[1]) + " stack";

        return handIndex + " " + playStr + cardStr + " draw from " + drawStr;
    }

    public int getHandIndex() {
        return handIndex;
    }

    public void setHandIndex(int handIndex) {
        this.handIndex = handIndex;
    }

    public int getPrevTopCard() {
        return prevTopCard;
    }

    public void setPrevTopCard(int prevTopCard) {
        this.prevTopCard = prevTopCard;
    }
}
