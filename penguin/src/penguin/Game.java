package penguin;

import ai.MCTSOptions;
import ai.mcts.MCTSPlayer;
import framework.AIPlayer;
import penguin.game.Board;
import penguin.game.Move;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        MCTSOptions options1 = new MCTSOptions();
        options1.setGame("penguin");
        AIPlayer aiPlayer1 = new MCTSPlayer();
        aiPlayer1.setOptions(options1);
        options1.fixedSimulations = true;
        options1.useHeuristics = true;
        options1.simulations = 25000;
        options1.solver = false;


        MCTSOptions options2 = new MCTSOptions();
        options2.setGame("penguin");
        AIPlayer aiPlayer2 = new MCTSPlayer();
        aiPlayer2.setOptions(options2);
        options2.fixedSimulations = true;
        options2.simulations = 25000;
        options2.useHeuristics = true;
        options2.solver = false;
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


