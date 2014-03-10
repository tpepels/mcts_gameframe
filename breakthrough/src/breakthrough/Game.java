package breakthrough;

import ai.SRMCTS.SRMCTSPlayer;
import ai.framework.AIPlayer;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import breakthrough.game.Board;
import breakthrough.game.Move;
import ai.SRCRMCTS.*;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        MCTSOptions options1 = new MCTSOptions();
        options1.setGame("breakthrough");
        AIPlayer aiPlayer1 = new SRMCTSPlayer();
        aiPlayer1.setOptions(options1);
//        options1.timeInterval = 2500;
//        options1.fixedSimulations = true;
//        options1.simulations = 20000;

        MCTSOptions options2 = new MCTSOptions();
        options2.setGame("breakthrough");
        AIPlayer aiPlayer2 = new MCTSPlayer();
        aiPlayer2.setOptions(options2);
//        options2.timeInterval = 2500;
//        options2.fixedSimulations = true;
//        options2.simulations = 20000;

        AIPlayer aiPlayer;
        Move m = null;
        while (b.checkWin() == Board.NONE_WIN) {
            int player = b.getPlayerToMove();
            System.out.println(b.toString());

            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2);
            System.gc();
            aiPlayer.getMove(b.copy(), null, b.getPlayerToMove(), false, m);
            m = (Move) aiPlayer.getBestMove();
            b.doAIMove(m, player);

            if (m != null)
                System.out.println("Player " + player + " played " + m);
        }

        System.out.println("Winner is " + b.checkWin());
    }

}


