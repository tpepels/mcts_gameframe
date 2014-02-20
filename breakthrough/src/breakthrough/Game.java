package breakthrough;

import ai.framework.AIPlayer;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import breakthrough.game.Board;
import breakthrough.game.Move;
import mcts2e.SRCRMCTS.HalfGreedySelect;
import mcts2e.SRCRMCTS.SRCRMCTSPlayer;
import mcts2e.SRCRMCTS.SelectionPolicy;
import mcts2e.SRCRMCTS.UCT;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        MCTSOptions options1 = new MCTSOptions();
        options1.setGame("breakthrough");
        AIPlayer aiPlayer1 = new SRCRMCTSPlayer();
        aiPlayer1.setOptions(options1);
        SelectionPolicy selectionPolicy1 = new HalfGreedySelect(options1);
        ((SRCRMCTSPlayer)aiPlayer1).setSelectionPolicy(selectionPolicy1);

        MCTSOptions options2 = new MCTSOptions();
        options2.setGame("breakthrough");
        AIPlayer aiPlayer2 = new SRCRMCTSPlayer();
        aiPlayer2.setOptions(options2);
        SelectionPolicy selectionPolicy2 = new UCT(options2);
        ((SRCRMCTSPlayer)aiPlayer2).setSelectionPolicy(selectionPolicy2);

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


