package lostcities;

import ai.ISMCTS.ISMCTSPlayer;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import framework.AIPlayer;
import lostcities.game.Deck;
import lostcities.game.Move;
import lostcities.game.Table;

import java.util.Scanner;

public class Game {
    private static boolean allAi = true;

    public static void main(String[] args) {
        Table t = new Table();
        t.initialize();
        String player = "";
        Scanner in = new Scanner(System.in);
        MCTSOptions options1 = new MCTSOptions();
        options1.timeInterval = 5000;
        AIPlayer aiPlayer1 = new ISMCTSPlayer();
        aiPlayer1.setOptions(options1);

        MCTSOptions options2 = new MCTSOptions();
        options2.timeInterval = 5000;
        AIPlayer aiPlayer2 = new ISMCTSPlayer();
        aiPlayer2.setOptions(options2);

        AIPlayer aiPlayer;

        Move m = null;
        //
        while (t.checkWin() == Table.NONE_WIN) {
            drawTable(t);
            if (m != null)
                System.out.println(player + " played " + m);
            player = (t.getPlayerToMove() == Table.P1) ? "Player 1" : "Player 2";
            if (allAi || t.getPlayerToMove() == Table.P2) {
                System.out.println("AI Player thinking. . .");
                if (t.getPlayerToMove() == Table.P1) {
                    aiPlayer = aiPlayer1;
                } else {
                    aiPlayer = aiPlayer2;
                }
                // Run the GC in between moves, to limit the runs during search
                System.gc();
                aiPlayer.getMove(t.copy(), null, t.getPlayerToMove(), false, m);
                m = (Move) aiPlayer.getBestMove();
                t.doMove(m);
                continue;
            } else {
                System.out.println(player + " select a card.");
                m = null;
                int index;
                int card = 0;
                while (m == null || !t.isLegal(m)) {
                    if (m != null)
                        System.out.println("illegal move - select another");
                    index = -1;
                    while (index == -1) {
                        String cardString = in.nextLine();
                        card = getCard(cardString);
                        index = getHandIndex(card, t.getPlayerToMove(), t);
                        if (index == -1)
                            System.out.println("Card not in hand");
                    }
                    System.out.println("Discard? (true/false)");
                    boolean discard = in.nextBoolean();
                    in.nextLine();
                    System.out.println("Draw from which stack? (0 = Deck, 1 = Y ... 5 = R)");
                    int draw = in.nextInt();
                    in.nextLine();
                    m = new Move(card, draw, discard);
                }
            }
            t.doMove(m);
        }
        // Check and announce who won!
        player = (t.checkWin() == Table.P1) ? "Player 1" : "Player 2";
        System.out.println(player + " wins");
    }

    public static void drawTable(Table t) {
        clearConsole();
        System.out.println("Player 2 - Hand \t \t \t \t \t \t \t \tScore");
        printHand(8, 16, t.hands);
        System.out.println("\t \t \t" + t.scores[1]);
        System.out.println("\n");
        System.out.println("Player 2 - Expeditions");
        printExpeditions(5, 10, t);
        System.out.println("-----------------------------");
        System.out.println("Coloured Stacks \t \tDeck");
        for (int i = 0; i < t.stacks.length; i++) {
            if (t.stacks[i].isEmpty())
                System.out.print(getColour(i + 1) + ":# ");
            else {
                String card = (t.stacks[i].peek() % 100 < Deck.INVESTMENT) ? Integer.toString(t.stacks[i].peek() % 100) : "$";
                System.out.print(getColour(i + 1) + ":" + card + " ");
            }
        }
        System.out.println("\tD: " + t.deck.size());
        System.out.println("-----------------------------");
        System.out.println("Player 1 - Expeditions");
        printExpeditions(0, 5, t);
        System.out.println("\n");
        System.out.println("Player 1 - Hand \t \t \t \t \t \t \t \tScore");
        printHand(0, 8, t.hands);
        System.out.println("\t \t \t" + t.scores[0]);
    }

    private static void printHand(int start, int end, int[] hand) {
        int type, colour, card;
        for (int i = start; i < end; i++) {
            card = hand[i];
            colour = card / 100;
            type = card % 100;
            String cardStr = (type == Deck.INVESTMENT) ? "$" : Integer.toString(type);
            System.out.print(getColour(colour) + cardStr);
            if (i < 15)
                System.out.print(" | ");
        }
    }

    private static void printExpeditions(int start, int end, Table t) {
        int c = 1;
        System.out.println("Col | Top | Mult | Num | Scor ");
        for (int i = start; i < end; i++) {
            System.out.println(getColour(c) + "   | " + t.expeditionCards[i] + "   | " + t.multipliers[i] + "    | " + t.numExpeditionCards[i] + "   | " + t.expeditionScores[i]);
            c++;
        }
    }

    private static int getHandIndex(int card, int player, Table t) {
        int startI = (player == Table.P1) ? 0 : Table.P2_HAND_I;
        int endI = (player == Table.P1) ? Table.P2_HAND_I + 1 : t.hands.length;
        //
        for (int i = startI; i < endI; i++) {
            if (t.hands[i] == card)
                return i;
        }
        return -1;
    }

    private static int getCard(String card) {
        char colour = card.toUpperCase().charAt(0);
        int c = -1;
        switch (colour) {
            case ('Y'):
                c = 100;
                break;
            case ('B'):
                c = 200;
                break;
            case ('W'):
                c = 300;
                break;
            case ('G'):
                c = 400;
                break;
            case ('R'):
                c = 500;
                break;
        }
        if (card.charAt(1) == '$')
            return c + Deck.INVESTMENT;
        else {
            String number = card.substring(1, card.length());
            int num = Integer.parseInt(number);
            return c + num;
        }
    }

    private static String getColour(int colour) {
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

    private static void clearConsole() {
        try {
            String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                Runtime.getRuntime().exec("cls");
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (Exception exception) {
            //  Handle exception.
        }
    }
}
