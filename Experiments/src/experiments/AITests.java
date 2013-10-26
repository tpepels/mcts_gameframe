package experiments;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class AITests {
    private static final DecimalFormat df2 = new DecimalFormat("###,###.###");
    //
    private AIPlayer aiPlayer1, aiPlayer2;
    private int games = 0, totalGames = 0;
    private double ai1Wins = 0, ai2Wins = 0, draws = 0;
    private int ai1Color = IBoard.P1, ai2Color = IBoard.P2;
    private IMove lastMove = null;
    //
    private String outFile, game;
    private PrintWriter out;

    public static void main(String[] args) {
        AITests aitests = new AITests();
        int test;
        if (args.length == 0) {
            Scanner in = new Scanner(System.in);
            System.out.println("Enter test number");
            test = in.nextInt();
            System.out.println("Enter no of games");
            aitests.games = in.nextInt();
            in.nextLine();
            System.out.println("Enter output file");
            aitests.outFile = in.nextLine();
            System.out.println("Enter game");
            aitests.game = in.nextLine();
            in.close();
        } else {
            test = Integer.parseInt(args[0]);
            aitests.games = Integer.parseInt(args[1]);
            aitests.outFile = args[2];
            aitests.game = args[3];
        }
        System.out.println("Running test # " + test + ", # games: " + aitests.games);
        aitests.runTests(test);
    }

    public void runTests(int which) {
        try {
            out = new PrintWriter(outFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        writeOutput(df.format(today));

        if (which == 1) {
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.debug = false;
            options1.mastEnabled = true;
            options1.treeOnlyMast = false;
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            options2.debug = false;
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            for (double i = 0.2; i < 1.; i += .2) {
                options1.mastEpsilon = i;
                runGames("AI 1 MAST Epsilon = " + i + " || AI 2 Normal");
            }
        } else if (which == 2) {
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.debug = false;
            options1.depthDiscount = true;
            options1.fixedDD = true;
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            options2.debug = false;
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            for (double i = 0.05; i <= .21; i += .05) {
                options1.depthD = i;
                runGames("AI 1 Fixed Depth Discount, DD = " + i + " || AI 2 Normal");
            }
        } else if (which == 3) {
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.debug = false;
            options1.depthDiscount = true;
            options1.fixedDD = false;
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            options2.debug = false;
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            for (double i = 0.05; i <= .21; i += .05) {
                options1.depthD = i;
                runGames("AI 1 Depth Discount, DD = " + i + " || AI 2 Normal");
            }
        } else if (which == 4) {
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.debug = false;
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            options2.debug = false;
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            for (double i = 0.6; i <= 1.6; i += .1) {
                options1.uctC = i;
                runGames("AI 1 UCT-C: " + i + " || AI 2 MCTS");
            }
        }
    }

    private void runGames(String testMessage) {
        writeOutput(testMessage);
        totalGames = 0;
        ai1Color = IBoard.P1;
        ai2Color = IBoard.P2;
        ai1Wins = 0;
        ai2Wins = 0;
        draws = 0;
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

    private IBoard getBoard() {
        Class<?> clss = null;
        try {
            if (game.equals("cannon"))
                clss = Class.forName("cannon.game.Board");
            else if (game.equals("chinesecheckers"))
                clss = Class.forName("chinesecheckers.game.Board");
            else if (game.equals("lostcities"))
                clss = Class.forName("lostcities.game.Table");
            else if (game.equals("pentalath"))
                clss = Class.forName("pentalath.game.Board");
            else if (game.equals("amazons"))
                clss = Class.forName("amazons.game.Board");
            // Instantiate the board for the chosen game and GO
            if (clss != null)
                return (IBoard) clss.newInstance();
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void runGame() {
        IBoard board = getBoard();
        board.initialize();
        int winner = IBoard.NONE_WIN;
        while (winner == IBoard.NONE_WIN) {
            //
            IMove move;
            if (ai1Color == board.getPlayerToMove()) {
                aiPlayer1.getMove(board.copy(), null, ai1Color, false, lastMove);
                move = aiPlayer1.getBestMove();
            } else {
                aiPlayer2.getMove(board.copy(), null, ai2Color, false, lastMove);
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
            ai1Wins++;
        } else if (winner == ai2Color) {
            ai2Wins++;
        } else {
            draws++;
        }
        printStatistics();
    }

    private void printStatistics() {
        double total = ai1Wins + ai2Wins + draws;
        if (total > 0) {
            double mean = (ai1Wins + (0.5 * draws)) / total;
            // Calculate the variance
            double variance = 0.;
            variance += ai1Wins * Math.pow(1.0 - mean, 2);
            variance += draws * Math.pow(0.5 - mean, 2);
            variance += ai2Wins * Math.pow(0. - mean, 2);
            // Std dev and 95% conf. int.
            variance /= total;
            double ci95 = (1.96 * Math.sqrt(variance)) / Math.sqrt(total);
            double ai2WinRate = (ai2Wins + 0.5 * draws) / total;
            writeOutput("[" + df2.format(mean * 100.0) + "%, " + df2.format(ai2WinRate * 100.0) + "%] +/- " + df2.format(ci95 * 100.0) + "%");
        }
    }
}
