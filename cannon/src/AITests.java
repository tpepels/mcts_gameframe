import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveCallback;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import cannon.game.Board;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Scanner;

public class AITests implements MoveCallback {
    private AIPlayer aiPlayer1, aiPlayer2;
    private IBoard board;
    private int games = 20;
    //
    private int winner = -1, totalGames;
    private double ai1Wins = 0;
    private int ai1Color = IBoard.P1, ai2Color = IBoard.P2;
    private double ai2Wins = 0;
    private IMove lastMove = null;
    //
    private String outFile;
    private PrintWriter out;
    DecimalFormat df2 = new DecimalFormat("#,###,###,###,##0");
    DecimalFormat df1 = new DecimalFormat("#,###,###,###,##0.##");

    public static void main(String[] args) {
        AITests aitests = new AITests();
        int test = 0, games = 0;
        if (args.length == 0) {
            Scanner in = new Scanner(System.in);
            System.out.println("Enter test number");
            test = in.nextInt();
            System.out.println("Enter no of games");
            games = in.nextInt();
            in.close();
        } else {
            test = Integer.parseInt(args[0]);
            games = Integer.parseInt(args[1]);
        }
        System.out.println("Running test # " + test + ", # games: " + games);
        aitests.runTests(test);
    }

    public void runTests(int which) {
        if (which == 1) {
            outFile = "D:\\tests\\test1.txt";
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.treeDecay = true;
            options1.treeReuse = true;
            options1.accelerated = false;
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            for (double i = 0.0; i <= 1.0; i += 0.2) {
                options1.discount = i;
                runGames("AI1 tree discount :" + i + " AI2 Vanilla MCTS");
            }
        } else if (which == 2) {
            outFile = "D:\\tests\\test2.txt";
            MCTSOptions options1 = new MCTSOptions();
            options1.treeDecay = false;
            options1.treeReuse = false;
            options1.accelerated = true;
            // AI 1
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            for (double i = 1; i < 7; i++) {
                options1.lambda = 1 - Math.pow(0.1, i);
                runGames("AI1 lambda k :" + i + " AI2 Vanilla MCTS");
            }
        } else if (which == 3) {

        } else if (which == 4) {

        }
    }

    private void runGames(String testMessage) {
        try {
            out = new PrintWriter(outFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        writeOutput(testMessage);
        totalGames = 0;
        ai1Color = IBoard.P1;
        ai2Color = IBoard.P2;
        ai1Wins = 0;
        ai2Wins = 0;
        while (totalGames < games) {
            runGame();
            // Switch the colors so 50% is played as black/white
            ai1Color = ai1Color == IBoard.P1 ? IBoard.P2 : IBoard.P1;
            ai2Color = ai2Color == IBoard.P1 ? IBoard.P2 : IBoard.P1;
        }
        //
        writeOutput("AI1: Wins " + ai1Wins);
        writeOutput("AI2: Wins " + ai2Wins);
    }

    private void writeOutput(String output) {
        if (out != null) { // Write output to file
            out.println(output);
            out.flush();
        }
        // Also write it to default output in case writing to file fails.
        System.out.println(output);
    }

    public void runGame() {
        board = new Board();
        winner = IBoard.NONE_WIN;
        while (winner == IBoard.NONE_WIN) {
            //
            IMove move;
            if (ai1Color == board.getPlayerToMove()) {
                aiPlayer1.getMove(board.copy(), this, ai1Color, false, lastMove);
                move = aiPlayer1.getBestMove();
            } else {
                aiPlayer2.getMove(board.copy(), this, ai2Color, false, lastMove);
                move = aiPlayer2.getBestMove();
            }
            if (board.doAIMove(move, board.getPlayerToMove())) {
                lastMove = move;
                winner = board.checkWin();
            } else {
                System.err.println("Error, invalid move!");
            }
        }
        lastMove = null;
        // Bookkeeping
        totalGames++;
        if (winner == ai1Color) {
            writeOutput("Ai 1 wins.");
            ai1Wins++;
        } else if (winner == ai2Color) {
            writeOutput("Ai 2 wins.");
            ai2Wins++;
        } else {
            writeOutput("Draw.");
        }
    }

    @Override
    public void makeMove(IMove move) {

    }
}
