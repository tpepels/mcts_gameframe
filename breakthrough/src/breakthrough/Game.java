package brakthrough;

import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import breakthrough.game.Move;
import breakthrough.game.Table;

import java.util.Scanner;

public class Game {

    public static void main(String[] args) { 
        Board b = new Board(); 
        b.initialize(); 

        MCTSOptions options1 = new MCTSOptions();
        MCTSPlayer aiPlayer1 = new MCTSPlayer();
        aiPlayer1.setOptions(options1);

        MCTSOptions options2 = new MCTSOptions();
        MCTSPlayer aiPlayer2 = new MCTSPlayer();
        aiPlayer2.setOptions(options2);

        MCTSPlayer aiPlayer;
        Move m = null;
        int player = 1; 

        while (t.checkWin() == Board.NONE_WIN) {
            System.out.println(b.toString());
            if (m != null)
                System.out.println("Player " + player + " played " + m);

            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2); 
            System.gc();
            aiPlayer.getMove(b.copy(), null, b.getPlayerToMove(), false, m);
            m = (Move) aiPlayer.getBestMove();
            b.doAIMove(m);
        }

        System.out.println("Winner is " + b.checkWin());
    }

}


