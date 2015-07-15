package breakthrough;

import ai.MCTSOptions;
import ai.mcts.MCTSPlayer;
import breakthrough.game.Board;
import breakthrough.game.Move;
import framework.AIPlayer;
import framework.util.KeyboardPlayer;
import mcts_tt.H_MCTS.HybridPlayer;
import mcts_tt.uct.UCTPlayer;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        AIPlayer aiPlayer2 = new KeyboardPlayer();

//        AIPlayer aiPlayer1 = new UCTPlayer();
//        MCTSOptions options1 = new MCTSOptions();
//        options1.setGame("breakthrough");
//        options1.fixedSimulations = true;
//        options1.simulations = 30000;
//        options1.solver = true;
//        options1.useHeuristics = true;
//        aiPlayer1.setOptions(options1);


        AIPlayer aiPlayer1 = new UCTPlayer();
        MCTSOptions options2 = new MCTSOptions();
        options2.setGame("breakthrough");
        options2.timeInterval = 30000;
        options2.solver = true;
        options2.solverFix = true;
        options2.useHeuristics = true;
        aiPlayer1.setOptions(options2);

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


