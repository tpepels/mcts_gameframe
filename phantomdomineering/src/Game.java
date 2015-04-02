import ai.ISMCTS.ISMCTSPlayer;
import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import phantomdomineering.game.Board;
import phantomdomineering.game.Move;

import java.util.concurrent.ExecutorService;

public class Game {
    public static void main(String[] args) {
        Board b = new Board(6);
        b.initialize();

        AIPlayer aiPlayer1 = new ISMCTSPlayer();
        MCTSOptions options1 = new MCTSOptions();
        options1.fixedSimulations = true;
        options1.simulations = 10000;
        options1.setGame("domineering");
        aiPlayer1.setOptions(options1);

        MCTSOptions options2 = new MCTSOptions();
        options2.setGame("domineering");
        options2.fixedSimulations = true;
        options2.simulations = 10000;
        AIPlayer aiPlayer2 = new ISMCTSPlayer();
        aiPlayer2.setOptions(options2);

        AIPlayer aiPlayer;
        Move m = null;
        while (b.checkWin() == Board.NONE_WIN) {
            System.out.println(b.toString());
            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2);
            System.gc();
            IBoard copyBoard = b.copy();
            copyBoard.newDeterminization(b.getPlayerToMove());
            System.out.println(copyBoard);
            copyBoard = b.copy();
            copyBoard.newDeterminization(b.getPlayerToMove());
            try {
                aiPlayer.getMove(copyBoard, null, b.getPlayerToMove(), false, m);
            } catch (Exception ex) {
                copyBoard = b.copy();
                copyBoard.newDeterminization(b.getPlayerToMove());
            }
            m = (Move) aiPlayer.getBestMove();
            b.doAIMove(m, b.getPlayerToMove());
            if (m != null)
                System.out.println("Player " + (3 - b.getPlayerToMove()) + " played " + m);
        }

        System.out.println("Winner is " + b.checkWin());
    }
}
