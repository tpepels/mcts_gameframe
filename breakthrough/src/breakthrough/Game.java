package breakthrough;

import ai.MCTSOptions;
import ai.mcts.MCTSPlayer;
import breakthrough.game.Board;
import breakthrough.game.Move;
import framework.AIPlayer;
import framework.util.KeyboardPlayer;
import mcts_tt.H_MCTS.HybridPlayer;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        AIPlayer aiPlayer1 = new KeyboardPlayer();

        MCTSOptions options2 = new MCTSOptions();
        options2.pdepth = 4;
        options2.earlyEval = true;
        options2.useHeuristics = true;
        options2.timeInterval = 60000;
        options2.implicitMM = true;
        options2.treeReuse = true;
        options2.solver = true;
        options2.solverFix = true;
        options2.debug = true;
        options2.progBias = true;
        options2.nodePriors = true;
        MCTSPlayer aiPlayer2 = new MCTSPlayer();
        aiPlayer2.setOptions(options2);

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


