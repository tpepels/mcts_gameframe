package breakthrough;

import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import breakthrough.game.Board;
import breakthrough.game.Move;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        MCTSOptions options1 = new MCTSOptions();
//        options1.pdepth = 0;
//        options1.earlyEval = true;
//        options1.useHeuristics = true;
//        options1.timeInterval = 1000;
        options1.setGame("breakthrough");
        MCTSPlayer aiPlayer1 = new MCTSPlayer();
        aiPlayer1.setOptions(options1);
        aiPlayer1.newGame(1, "breakthrough");

        MCTSOptions options2 = new MCTSOptions();
//        options2.relativeBonus = true;
//        options2.pdepth = 0;
//        options2.earlyEval = true;
//        options2.useHeuristics = true;
//        options2.timeInterval = 1000;
//        options2.implicitMM = true;
        MCTSPlayer aiPlayer2 = new MCTSPlayer();
        aiPlayer2.setOptions(options2);

        MCTSPlayer aiPlayer;
        Move m = null;
//        setupTestBoard(b);
//        b.curPlayer = 2;
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

    private static void setupTestBoard(Board b) {
        String board = ".bbb....\n" +
                "b...bbb.\n" +
                "w.bb....\n" +
                ".b.w.b..\n" +
                "..bw.w..\n" +
                "wwww....\n" +
                "w.....w.\n" +
                "..ww.w..";
        String[] arr = board.split("\n");
        b.board = new char[8][8];
        for (int i = 0; i < 8; i++) {
            b.board[i] = arr[i].toCharArray();
        }
    }
}


