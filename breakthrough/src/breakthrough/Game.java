package breakthrough;

import ai.framework.AIPlayer;
import ai.mcts.MCTSOptions;
import breakthrough.game.Board;
import breakthrough.game.Move;
import mcts_tt.MCTS_SR.SRPlayer;
import mcts_tt.uct.UCTPlayer;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        MCTSOptions options1 = new MCTSOptions();
        options1.setGame("breakthrough");
        AIPlayer aiPlayer1 = new UCTPlayer();
        aiPlayer1.setOptions(options1);
        options1.fixedSimulations = true;
        options1.simulations = 25000;
        options1.solver = true;


        MCTSOptions options2 = new MCTSOptions();
        options2.setGame("breakthrough");
        AIPlayer aiPlayer2 = new SRPlayer();
        aiPlayer2.setOptions(options2);
        options2.fixedSimulations = true;
        options2.simulations = 25000;
        options2.solver = true;
        options2.UBLB = true;

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


