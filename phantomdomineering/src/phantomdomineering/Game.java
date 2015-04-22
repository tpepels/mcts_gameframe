package phantomdomineering;

import ai.ISMCTS.ISMCTSPlayer;
import ai.MCTSOptions;
import framework.AIPlayer;
import framework.IBoard;
import phantomdomineering.game.Board;
import phantomdomineering.game.Move;

public class Game {
    public static void main(String[] args) {
        Board b = new Board(8);
        b.initialize();

        MCTSOptions options1 = new MCTSOptions();
        AIPlayer aiPlayer1 = new ISMCTSPlayer();
        options1.fixedSimulations = true;
        options1.simulations = 10000;
        options1.banditD = true;
        options1.nDeterminizations = 20;
        options1.setGame("phantomdomineering");
        aiPlayer1.setOptions(options1);

        MCTSOptions options2 = new MCTSOptions();
        AIPlayer aiPlayer2 = new ISMCTSPlayer();
        options2.fixedSimulations = true;
        options2.simulations = 10000;
        options2.limitD = true;
        options2.nDeterminizations = 20;
        options2.forceSO = true;
        options2.setGame("phantomdomineering");
        aiPlayer2.setOptions(options2);

        AIPlayer aiPlayer;
        Move m = null;
        while (b.checkWin() == Board.NONE_WIN) {
            System.out.println(b.toString());
            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2);
            System.gc();
            IBoard copyBoard = b.copy();
            copyBoard.newDeterminization(b.getPlayerToMove());
            aiPlayer.getMove(copyBoard, null, b.getPlayerToMove(), false, m);
            m = (Move) aiPlayer.getBestMove();
            b.doAIMove(m, b.getPlayerToMove());
            if (m != null)
                System.out.println("Player " + (3 - b.getPlayerToMove()) + " played " + m);
        }

        System.out.println("Winner is " + b.checkWin());
    }
}
