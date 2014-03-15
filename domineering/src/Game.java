import ai.SRMCTS.SRMCTSPlayer;
import ai.framework.AIPlayer;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import domineering.game.Board;
import domineering.game.Move;

public class Game {
    public static void main(String[] args) {
        Board b = new Board(8);
        Board.CRAM = true;
        b.initialize();

        MCTSPlayer aiPlayer1 = new MCTSPlayer();
        MCTSOptions options1 = new MCTSOptions();
        options1.setGame("domineering");
        aiPlayer1.setOptions(options1);

        MCTSOptions options2 = new MCTSOptions();
        options2.setGame("domineering");
        AIPlayer aiPlayer2 = new SRMCTSPlayer();
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