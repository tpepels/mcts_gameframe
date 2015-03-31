package gofish;

import ai.ISMCTS.ISMCTSPlayer;
import ai.MCTSOptions;
import framework.AIPlayer;
import gofish.game.Board;
import gofish.game.Move;

import java.util.Scanner;

/**
 * Created by tom on 20/03/15.
 */
public class Game {

    private static boolean allAi = true;

    public static void main(String[] args) {
        Board t = new Board();
        t.initialize();
        //

        String player = "";
        Scanner in = new Scanner(System.in);
        //
        MCTSOptions options1 = new MCTSOptions();
        options1.numSimulations = 10000;
        options1.fixedSimulations = true;
        AIPlayer aiPlayer1 = new ISMCTSPlayer();
        aiPlayer1.setOptions(options1);

        MCTSOptions options2 = new MCTSOptions();
        options2.numSimulations = 10000;
        options2.fixedSimulations = true;
        AIPlayer aiPlayer2 = new ISMCTSPlayer();
        aiPlayer2.setOptions(options2);

        AIPlayer aiPlayer;

        Move m = null;
        //
        while (t.checkWin() == Board.NONE_WIN) {
            drawTable(t);
            if (m != null)
                System.out.println(player + " played " + m);
            player = (t.getPlayerToMove() == Board.P1) ? "Player 1" : "Player 2";
            if (allAi || t.getPlayerToMove() == Board.P2) {
                System.out.println("AI Player thinking. . .");
                if (t.getPlayerToMove() == Board.P1) {
                    aiPlayer = aiPlayer1;
                } else {
                    aiPlayer = aiPlayer2;
                }
                // Run the GC in between moves, to limit the runs during search
                System.gc();
                aiPlayer.getMove(t.copy(), null, t.getPlayerToMove(), false, m);
                m = (Move) aiPlayer.getBestMove();
                t.doAIMove(m, t.getPlayerToMove());
                continue;
            } else {
                System.out.println(player + " ask a card.");
                m = null;
                int card;
                while (m == null || !t.isLegal(m)) {
                    if (m != null)
                        System.out.println("illegal move - select another");
                    card = getCard(in.nextLine());
                    while (card > 13 || card < 1) {
                        card = getCard(in.nextLine());
                    }
                    m = new Move(card);
                    if (t.getPlayerToMove() == Board.P1)
                        if (!t.checkHand(t.p2Hand, card))
                            System.out.println("Go Fish!");
                        else if (!t.checkHand(t.p1Hand, card))
                            System.out.println("Go Fish!");
                }
            }
            t.doAIMove(m, t.getPlayerToMove());
        }
        drawTable(t);
        // Check and announce who won!
        player = (t.checkWin() == Board.P1) ? "Player 1" : "Player 2";
        System.out.println(player + " wins");
    }

    private static void drawTable(Board board) {
        for (Integer card : board.p2Hand) {
            if (allAi)
                System.out.print(getCardString(card));
            else
                System.out.print("#");
            System.out.print(" ");
        }
        System.out.println("\n");
        System.out.print("P1 Score: " + board.p1Score);
        System.out.println(" :: P2 Score: " + board.p2Score + " \t Deck: " + board.deck.size() + "\n");
        for (Integer card : board.p1Hand) {
            System.out.print(getCardString(card));
            System.out.print(" ");
        }
        System.out.println();
    }

    private static int getCard(String str) {
        switch (str.toUpperCase()) {
            case "A":
                return 1;
            case "K":
                return 13;
            case "Q":
                return 12;
            case "J":
                return 11;
            default:
                return Integer.valueOf(str);
        }
    }

    private static String getCardString(int card) {
        StringBuilder sb = new StringBuilder();
        switch (card / 100) {
            case 1:
                sb.append('\u2660');
                break;
            case 2:
                sb.append('\u2666');
                break;
            case 3:
                sb.append('\u2663');
                break;
            case 4:
                sb.append('\u2764');
                break;
        }
        //
        switch (card % 100) {
            case 11:
                sb.append("J");
                break;
            case 12:
                sb.append("Q");
                break;
            case 13:
                sb.append("K");
                break;
            case 1:
                sb.append("A");
                break;
            default:
                sb.append(card % 100);
        }
        return sb.toString();
    }
}
